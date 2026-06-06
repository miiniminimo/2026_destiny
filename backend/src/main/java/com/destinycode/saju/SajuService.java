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
    private final CharacterClassResolver classResolver;

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
        info.setImageReady(false);

        CharacterClassResolver.ClassResult classResult = classResolver.resolve(
                req.getBirthYear(), req.getBirthMonth(), req.getBirthDay(),
                "MALE".equals(req.getGender())
        );

        String description = generateDescription(req, classResult);

        SajuInfo saved = sajuRepository.save(info);

        generateAndSaveImageAsync(saved.getId(), req.getName(), req.getGender(),
                classResult.element(), classResult.className(), classResult.title());

        return SajuResponse.from(saved, toSummary(classResult, description));
    }

    @Transactional(readOnly = true)
    public SajuResponse getSaju(String email) {
        User user = findUser(email);
        SajuInfo info = sajuRepository.findByUser(user)
                .orElseThrow(() -> BusinessException.notFound("사주 정보가 없습니다."));

        CharacterClassResolver.ClassResult classResult = classResolver.resolve(
                info.getBirthYear(), info.getBirthMonth(), info.getBirthDay(),
                "MALE".equals(info.getGender())
        );

        String description = generateDescriptionFromInfo(info, classResult);
        return SajuResponse.from(info, toSummary(classResult, description));
    }

    // ─── 비동기 이미지 생성 ────────────────────────────────────────────────────

    @Async("imageExecutor")
    public void generateAndSaveImageAsync(Long sajuInfoId, String name, String gender,
                                           String element, String className, String title) {
        log.info("[Async] DALL-E 이미지 생성 시작 - sajuInfoId: {}", sajuInfoId);
        try {
            String base64Image = openAiService.generateCharacterImage(
                    name, gender, element, className, title
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

    private String generateDescription(SajuRequest req, CharacterClassResolver.ClassResult cr) {
        String aiDesc = anthropicService.generateSajuAnalysis(
                req.getName(), req.getGender(),
                String.valueOf(req.getBirthYear()), String.valueOf(req.getBirthMonth()),
                String.valueOf(req.getBirthDay()), req.getBirthTime(),
                req.getBirthPlace(), cr.element(), cr.className()
        );
        return aiDesc != null ? aiDesc : buildFallback(cr, req.getBirthTime(), req.getBirthPlace());
    }

    private String generateDescriptionFromInfo(SajuInfo info, CharacterClassResolver.ClassResult cr) {
        String aiDesc = anthropicService.generateSajuAnalysis(
                info.getName(), info.getGender(),
                String.valueOf(info.getBirthYear()), String.valueOf(info.getBirthMonth()),
                String.valueOf(info.getBirthDay()), info.getBirthTime(),
                info.getBirthPlace(), cr.element(), cr.className()
        );
        return aiDesc != null ? aiDesc : buildFallback(cr, info.getBirthTime(), info.getBirthPlace());
    }

    private String buildFallback(CharacterClassResolver.ClassResult cr, String birthTime, String birthPlace) {
        String timeNote = birthTime != null
                ? " " + birthTime + "에 태어나 시간의 천간 기운이 능력에 스며들었습니다."
                : " 태어난 시간이 신비로운 안개에 싸여 변칙 영력을 지니게 되었습니다.";
        return cr.baseDescription() + timeNote + " "
                + birthPlace + "의 영산 기운이 모태에 스며들어 직업 시너지가 활성화되었습니다.";
    }

    private SajuResponse.CharacterSummary toSummary(CharacterClassResolver.ClassResult cr, String description) {
        return SajuResponse.CharacterSummary.builder()
                .element(cr.element())
                .className(cr.className())
                .title(cr.title())
                .description(description)
                .build();
    }
}
