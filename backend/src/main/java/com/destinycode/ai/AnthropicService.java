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
     * 사주 정보를 받아 RPG 캐릭터 설명 텍스트를 생성합니다.
     */
    public String generateSajuAnalysis(String name, String gender, String birthYear,
                                       String birthMonth, String birthDay, String birthTime,
                                       String birthPlace, String element, String className,
                                       SajuPillars pillars) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("ANTHROPIC_API_KEY가 설정되지 않아 기본 설명을 사용합니다.");
            return null;
        }

        String genderKr = gender.equals("MALE") ? "남성" : "여성";
        String timeText = birthTime != null ? birthTime + "시" : "시간 미상";

        String pillarsText = pillars.yearPillar() + " " + pillars.monthPillar() + " " + pillars.dayPillar()
                + (pillars.timePillar() != null ? " " + pillars.timePillar() : " (시주 미상)");
        String shenShaText = pillars.shenSha().isEmpty() ? "없음" : String.join(", ", pillars.shenSha());

        String prompt = String.format("""
                아래 사주 정보를 바탕으로 한국의 전통 무속 세계관과 RPG 게임 언어를 결합한 캐릭터 설명을 작성해주세요.
                이 정보는 만세력 기준으로 정확히 산출된 사주팔자이니, 아래 수치를 반드시 반영해 묘사하세요.

                [사주 정보]
                - 이름: %s
                - 성별: %s
                - 생년월일: %s년 %s월 %s일
                - 태어난 시간: %s
                - 태어난 장소: %s
                - 오행 속성(일간 기준): %s
                - 직업 클래스: %s
                - 사주팔자 (년주 월주 일주 시주): %s
                - 일간(日干, 본질): %s
                - 일주 지세(十二運星): %s
                - 신살/귀성: %s

                [작성 규칙]
                1. 200자 내외로 간결하게 작성
                2. 사주팔자(년주/월주/일주/시주)와 일간, 십이운성 기운을 게임 스탯 언어로 표현
                3. K-POP 아이돌처럼 매력적이고 화려한 묘사 포함
                4. 신살/귀성이 있다면 각각을 무속적 RPG 스킬(퇴마, 치유, 소환 등)로 변환해 묘사. 없다면 일간의 기운을 스킬로 표현
                5. 마크다운 없이 순수 텍스트로만 작성

                캐릭터 설명:
                """, name, genderKr, birthYear, birthMonth, birthDay, timeText, birthPlace, element, className,
                pillarsText, pillars.dayGan(), pillars.dayJiSe(), shenShaText);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", ANTHROPIC_VERSION);

            Map<String, Object> body = Map.of(
                    "model", MODEL,
                    "max_tokens", 512,
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
}
