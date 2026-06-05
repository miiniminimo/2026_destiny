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

    /**
     * 사주 저장 + Claude 분석 → 즉시 응답
     * DALL-E 이미지는 백그라운드에서 생성 후 DB 저장 (imageReady: false로 먼저 반환)
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
        info.setImageReady(false); // 이미지 생성 전

        CharacterMeta meta = buildMeta(req.getBirthYear(), req.getGender());
        String description = generateDescription(
                req.getName(), req.getGender(),
                req.getBirthYear(), req.getBirthMonth(), req.getBirthDay(),
                req.getBirthTime(), req.getBirthPlace(),
                meta
        );

        SajuInfo saved = sajuRepository.save(info);

        // 이미지 생성은 백그라운드에서 (즉시 응답 후 처리)
        generateAndSaveImageAsync(saved.getId(), req.getName(), req.getGender(), meta);

        return SajuResponse.from(saved, toSummary(meta, description));
    }

    @Transactional(readOnly = true)
    public SajuResponse getSaju(String email) {
        User user = findUser(email);
        SajuInfo info = sajuRepository.findByUser(user)
                .orElseThrow(() -> BusinessException.notFound("사주 정보가 없습니다."));

        CharacterMeta meta = buildMeta(info.getBirthYear(), info.getGender());
        String description = generateDescription(
                info.getName(), info.getGender(),
                info.getBirthYear(), info.getBirthMonth(), info.getBirthDay(),
                info.getBirthTime(), info.getBirthPlace(),
                meta
        );
        return SajuResponse.from(info, toSummary(meta, description));
    }

    // ─── 비동기 이미지 생성 ────────────────────────────────────────────────────

    @Async("imageExecutor")
    public void generateAndSaveImageAsync(Long sajuInfoId, String name, String gender, CharacterMeta meta) {
        log.info("[Async] DALL-E 이미지 생성 시작 - sajuInfoId: {}", sajuInfoId);
        try {
            String base64Image = openAiService.generateCharacterImage(
                    name, gender, meta.element(), meta.className(), meta.title()
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

    private String generateDescription(String name, String gender, int year, int month,
                                        int day, String birthTime, String birthPlace,
                                        CharacterMeta meta) {
        String aiDesc = anthropicService.generateSajuAnalysis(
                name, gender,
                String.valueOf(year), String.valueOf(month), String.valueOf(day),
                birthTime, birthPlace, meta.element(), meta.className()
        );
        return aiDesc != null ? aiDesc : meta.fallbackDescription(birthTime, birthPlace);
    }

    private SajuResponse.CharacterSummary toSummary(CharacterMeta meta, String description) {
        return SajuResponse.CharacterSummary.builder()
                .element(meta.element())
                .className(meta.className())
                .title(meta.title())
                .description(description)
                .build();
    }

    CharacterMeta buildMeta(int birthYear, String gender) {
        boolean male = "MALE".equals(gender);
        return switch (birthYear % 10) {
            case 0, 1 -> new CharacterMeta(
                    "금 (Metal)",
                    male ? "명부의 저승사자(使者)" : "성스러운 삼신선녀(三神仙女)",
                    "명부의 룰을 다스리는 초절정 비주얼",
                    "탄생 연도의 예리한 금(金)의 기운을 얻어 날카로운 철검과 부적으로 사악한 영을 정화합니다."
            );
            case 2, 3 -> new CharacterMeta(
                    "수 (Water)",
                    male ? "대나무숲 청룡도사" : "심연의 용신무녀(龍神巫女)",
                    "신령스러운 해류를 지배하는 카리스마",
                    "깊고 고요한 수(水)의 영성으로 용신의 방울을 흔들며 비를 부르고 어둠을 차단합니다."
            );
            case 4, 5 -> new CharacterMeta(
                    "목 (Wood)",
                    male ? "청운의 옥피리 도사" : "버드나무 백호선녀",
                    "동방의 생명력을 개척하는 수려한 선인",
                    "푸르른 목(木)의 청명한 생명력으로 신령의 부채와 피리 소리로 영혼을 치유합니다."
            );
            case 6, 7 -> new CharacterMeta(
                    "화 (Fire)",
                    male ? "벼락 불꽃의 천우신장(神將)" : "붉은 봉황의 지옥 화무(巫堂)",
                    "신당의 불꽃을 각성한 화려한 리드 보컬",
                    "뜨겁고 찬란한 화(火)의 불꽃으로 오색 깃발과 불타는 방울을 흔들어 악귀를 퇴마합니다."
            );
            default -> new CharacterMeta(
                    "토 (Earth)",
                    male ? "바위 성벽의 태수장군(大將軍)" : "지리산 산신녀(山神女)",
                    "대지의 무게를 견디는 든든한 리더",
                    "묵직하고 강건한 토(土)의 기운으로 지리산 영산의 정기를 담아 강건한 가호의 장벽을 칩니다."
            );
        };
    }

    record CharacterMeta(String element, String className, String title, String baseDesc) {
        String fallbackDescription(String birthTime, String birthPlace) {
            String timeNote = birthTime != null
                    ? " 태어난 시각 " + birthTime + "의 천간 기운이 능력에 스며들었습니다."
                    : " 태어난 시간이 신비로운 안개에 싸여 변칙 영력을 지니게 되었습니다.";
            return baseDesc + timeNote + " " + birthPlace + "의 영산 기운이 모태에 스며들어 직업 시너지가 활성화되었습니다.";
        }
    }
}
