package com.destinycode.saju;

import com.destinycode.ai.AnthropicService;
import com.destinycode.ai.OpenAiService;
import com.destinycode.common.exception.BusinessException;
import com.destinycode.saju.dto.SajuRequest;
import com.destinycode.saju.dto.SajuResponse;
import com.destinycode.user.User;
import com.destinycode.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SajuService {

    private final SajuRepository sajuRepository;
    private final UserRepository userRepository;
    private final AnthropicService anthropicService;
    private final OpenAiService openAiService;
    private final SajuCalculator sajuCalculator;
    private final CharacterClassResolver classResolver;

    /**
     * 사주 미리보기 - DB에 저장하지 않고 Claude 분석 + DALL-E 이미지를 즉시 응답
     * 사용자가 "저장" 버튼을 눌러야 saveSaju()로 실제 저장됨
     */
    public SajuResponse previewSaju(SajuRequest req) {
        SajuPillars pillars = sajuCalculator.calculate(
                req.getCalendarType(), req.getBirthYear(), req.getBirthMonth(), req.getBirthDay(), req.getBirthTime()
        );
        CharacterClassResolver.ClassResult classResult = classResolver.resolve(pillars, "MALE".equals(req.getGender()));

        String description = generateDescription(req, pillars, classResult);

        String imageData = openAiService.generateCharacterImage(
                req.getName(), req.getGender(), classResult.element(), classResult.className(), classResult.title(), description
        );

        return SajuResponse.fromPreview(req, toSummary(pillars, classResult, description), imageData);
    }

    /**
     * 사주 저장 + Claude 분석 → 즉시 응답
     * DALL-E 이미지는 백그라운드에서 생성 후 DB 저장
     */
    @Transactional
    public SajuResponse saveSaju(String email, SajuRequest req) {
        User user = findUser(email);

        SajuInfo info = sajuRepository.findByUser(user)
                .orElse(SajuInfo.builder().user(user).build());

        info.setName(req.getName());
        info.setGender(req.getGender());
        info.setCalendarType(req.getCalendarType());
        info.setBirthYear(req.getBirthYear());
        info.setBirthMonth(req.getBirthMonth());
        info.setBirthDay(req.getBirthDay());
        info.setBirthTime(req.getBirthTime());
        info.setBirthPlace(req.getBirthPlace());
        SajuPillars pillars = sajuCalculator.calculate(
                req.getCalendarType(), req.getBirthYear(), req.getBirthMonth(), req.getBirthDay(), req.getBirthTime()
        );
        CharacterClassResolver.ClassResult classResult = classResolver.resolve(pillars, "MALE".equals(req.getGender()));

        String description = generateDescription(req, pillars, classResult);

        if (req.getImageData() != null && !req.getImageData().isBlank()) {
            // 미리보기에서 이미 생성된 이미지를 그대로 저장 (재생성 비용 방지)
            info.setImageData(req.getImageData());
            info.setImageReady(true);
            SajuInfo saved = sajuRepository.save(info);
            return SajuResponse.from(saved, toSummary(pillars, classResult, description));
        }

        info.setImageReady(false);
        info.setImageData(null);

        SajuInfo saved = sajuRepository.save(info);

        generateAndSaveImageAsync(saved.getId(), req.getName(), req.getGender(),
                classResult.element(), classResult.className(), classResult.title(), description);

        return SajuResponse.from(saved, toSummary(pillars, classResult, description));
    }

    @Transactional(readOnly = true)
    public SajuResponse getSaju(String email) {
        User user = findUser(email);
        SajuInfo info = sajuRepository.findByUser(user)
                .orElseThrow(() -> BusinessException.notFound("사주 정보가 없습니다."));

        SajuPillars pillars = sajuCalculator.calculate(
                info.getCalendarType(), info.getBirthYear(), info.getBirthMonth(), info.getBirthDay(), info.getBirthTime()
        );
        CharacterClassResolver.ClassResult classResult = classResolver.resolve(pillars, "MALE".equals(info.getGender()));

        String description = generateDescriptionFromInfo(info, pillars, classResult);
        return SajuResponse.from(info, toSummary(pillars, classResult, description));
    }

    // ─── 비동기 이미지 생성 ────────────────────────────────────────────────────

    @Async("imageExecutor")
    public void generateAndSaveImageAsync(Long sajuInfoId, String name, String gender,
                                           String element, String className, String title, String description) {
        log.info("[Async] DALL-E 이미지 생성 시작 - sajuInfoId: {}", sajuInfoId);
        try {
            String base64Image = openAiService.generateCharacterImage(
                    name, gender, element, className, title, description
            );
            if (base64Image != null) {
                saveImage(sajuInfoId, base64Image);
                log.info("[Async] DALL-E 이미지 저장 완료 - sajuInfoId: {}", sajuInfoId);
            }
        } catch (Exception e) {
            log.error("[Async] DALL-E 이미지 생성 실패 - sajuInfoId: {}", sajuInfoId, e);
        }
    }

    @Transactional
    public void saveImage(Long sajuInfoId, String base64Image) {
        sajuRepository.findById(sajuInfoId).ifPresent(info -> {
            info.setImageData(base64Image);
            info.setImageReady(true);
            sajuRepository.save(info);
        });
    }

    // ─── 내부 헬퍼 ────────────────────────────────────────────────────────────

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다."));
    }

    private String generateDescription(SajuRequest req, SajuPillars pillars, CharacterClassResolver.ClassResult cr) {
        return anthropicService.generateSajuAnalysis(
                req.getName(), req.getGender(),
                String.valueOf(req.getBirthYear()), String.valueOf(req.getBirthMonth()),
                String.valueOf(req.getBirthDay()), req.getBirthTime(),
                req.getBirthPlace(), cr.element(), cr.className(), cr.title(), pillars
        );
    }

    private String generateDescriptionFromInfo(SajuInfo info, SajuPillars pillars, CharacterClassResolver.ClassResult cr) {
        return anthropicService.generateSajuAnalysis(
                info.getName(), info.getGender(),
                String.valueOf(info.getBirthYear()), String.valueOf(info.getBirthMonth()),
                String.valueOf(info.getBirthDay()), info.getBirthTime(),
                info.getBirthPlace(), cr.element(), cr.className(), cr.title(), pillars
        );
    }

    private SajuResponse.CharacterSummary toSummary(SajuPillars pillars, CharacterClassResolver.ClassResult cr, String description) {
        String pillarsText = pillars.yearPillar() + " " + pillars.monthPillar() + " " + pillars.dayPillar()
                + (pillars.timePillar() != null ? " " + pillars.timePillar() : " (시주 미상)");

        return SajuResponse.CharacterSummary.builder()
                .element(cr.element())
                .className(cr.className())
                .title(cr.title())
                .description(description)
                .pillars(pillarsText)
                .shenSha(cr.shenSha())
                .build();
    }
}
