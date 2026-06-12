package com.destinycode.ai;

import com.destinycode.saju.SajuPillars;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AnthropicService {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final String MODEL = "claude-opus-4-6";

    private final String apiKey;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnthropicService(@Value("${ai.anthropic.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * 사주 정보를 받아 상세한 RPG 캐릭터 프로필을 생성합니다.
     *
     * @param element   오행+음양 (예: "金 (양금)")
     * @param className 세분화된 클래스명 (예: "서릿발 같은 저승사자(使者) [절정]")
     * @param title     타이틀 (예: "절정의 金의 순수한 기운이 충만한 영웅")
     */
    public String generateSajuAnalysis(String name, String gender,
                                        String birthYear, String birthMonth, String birthDay,
                                        String birthTime, String birthPlace,
                                        String element, String className, String title,
                                        SajuPillars pillars) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("ANTHROPIC_API_KEY가 설정되지 않아 기본 설명을 사용합니다.");
            return null;
        }

        String genderKr  = "MALE".equals(gender) ? "남성" : "여성";
        String timeText  = birthTime != null ? birthTime + "시" : "시간 미상";

        String prompt = buildPrompt(name, genderKr, birthYear, birthMonth, birthDay,
                                    timeText, birthPlace, element, className, title, pillars);
        log.info("Anthropic API 호출 - 클래스: {}", className);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", ANTHROPIC_VERSION);

            Map<String, Object> body = Map.of(
                    "model", MODEL,
                    "max_tokens", 1024,
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    )
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(API_URL, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                return root.path("content").get(0).path("text").asText();
            }
        } catch (Exception e) {
            log.error("Anthropic API 호출 오류: {}", e.getMessage(), e);
        }

        return null;
    }

    private String buildPrompt(String name, String genderKr,
                                String year, String month, String day,
                                String time, String place,
                                String element, String className, String title,
                                SajuPillars pillars) {
        String pillarsText = pillars.yearPillar() + " " + pillars.monthPillar() + " " + pillars.dayPillar()
                + (pillars.timePillar() != null ? " " + pillars.timePillar() : " (시주 미상)");
        String shenShaText = pillars.shenSha().isEmpty() ? "없음" : String.join(", ", pillars.shenSha());

        return String.format("""
당신은 정밀 만세력 산출 및 사주명리학 분석을 수행하는 '스타일 사주 마스터 AI'입니다.
친근하면서도 날카로운 전문가의 어조로, 한국 전통 무속·사주 세계관과 현대 K-POP 판타지 RPG를 결합한
캐릭터 분석 보고서를 작성하세요.

[입력 정보 - 만세력(절입일 기준) 산출 완료된 사주 원국]
- 이름: %s
- 성별: %s
- 생년월일: %s년 %s월 %s일 %s
- 출생지: %s
- 오행/음양 (일간 기준): %s
- 직업 클래스: %s
- 칭호: %s
- 사주팔자 (년주 월주 일주 시주): %s
- 일간(日干, 본질): %s
- 일주 지세(十二運星): %s
- 신살/귀성: %s

위 사주 원국을 바탕으로 아래 형식을 **정확히** 지켜 분석 보고서를 작성하세요.
각 항목은 위에 주어진 사주팔자, 일간, 십이운성, 신살/귀성 정보를 최대한 구체적으로 반영해야 합니다.

---

【캐릭터 소개】
(이 캐릭터가 어떤 존재인지, 출생지와 오행의 기운이 어떻게 작용했는지 3~4문장으로 서술. K-POP 아이돌의 화려한 비주얼과 전통 무속의 신비로움을 함께 묘사.)

【오행 및 십성 분석】
주어진 사주팔자와 일간을 기준으로 비견·겁재·식신·상관·편재·정재·편관·정관·편인·정인의 분포를 분석하여
과다(35%% 이상)·발달(25~30%%)·적정(10~20%%)·부족(0%%) 성분을 짚어주고, 이를 바탕으로 핵심 사회적 기질과 태도를 2~3문장으로 설명하세요.

【신살 및 길성 기반 성향】
위에 주어진 신살/귀성(천을귀인, 문창귀인, 홍염살, 도화살, 괴강살, 백호대살, 현침살, 귀문관살 등)이 있다면
그 조합이 만들어내는 독특한 매력·성향·행동 패턴을 무속적 RPG 세계관에 녹여 2~3문장으로 설명하세요.
신살이 없다면 일간과 십이운성 기운을 바탕으로 성향을 유추하세요.

【종합 보고서】
- 핵심 기질: (비유적 표현으로 한 문장 정의)
- 장점: (2가지)
- 보완할 점: (1가지)
- 한 줄 타이틀: (이 인물에게 어울리는 칭호 형태의 한 줄)
- 핵심 키워드: (3개, 쉼표로 구분)
- 대화형 질문: (사용자와의 소통을 유도하는 질문 1개)

---
마크다운 기호(#, *, - 등) 없이 위 양식의 【】 제목만 사용해 그대로 출력하세요.
내용은 오행 속성과 클래스 특성에 맞게 캐릭터마다 반드시 다르게 작성하세요.
""",
                name, genderKr, year, month, day, time, place, element, className, title,
                pillarsText, pillars.dayGan(), pillars.dayJiSe(), shenShaText);
    }
}
