package com.destinycode.ai;

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
                                        String element, String className, String title) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("ANTHROPIC_API_KEY가 설정되지 않아 기본 설명을 사용합니다.");
            return null;
        }

        String genderKr  = "MALE".equals(gender) ? "남성" : "여성";
        String timeText  = birthTime != null ? birthTime + "시" : "시간 미상";

        String prompt = buildPrompt(name, genderKr, birthYear, birthMonth, birthDay,
                                    timeText, birthPlace, element, className, title);
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
                                String element, String className, String title) {
        return String.format("""
당신은 한국 전통 무속·사주 세계관과 현대 K-POP 판타지 RPG를 결합한 캐릭터 시트를 작성하는 전문가입니다.

[입력 정보]
- 이름: %s
- 성별: %s
- 생년월일: %s년 %s월 %s일 %s
- 출생지: %s
- 오행/음양: %s
- 직업 클래스: %s
- 칭호: %s

위 정보를 바탕으로 아래 형식을 **정확히** 지켜 캐릭터 프로필을 작성하세요.
각 항목은 실제 사주 해석(오행 상생상극, 천간지지의 기운)을 최대한 반영해 구체적으로 써야 합니다.

---

【캐릭터 소개】
(이 캐릭터가 어떤 존재인지, 출생지와 오행의 기운이 어떻게 작용했는지 3~4문장으로 서술. K-POP 아이돌의 화려한 비주얼과 전통 무속의 신비로움을 함께 묘사.)

【오행 스탯】
⚡ 영력(靈力): XX / 100  ← 오행 속성에 따라 수치 결정
🗡 전투력(戰鬪力): XX / 100
🌿 생명력(生命力): XX / 100
🧠 술법력(術法力): XX / 100
✨ 도화력(桃花力): XX / 100  ← 매력·카리스마 수치

【대표 스킬 3개】
1. [스킬명] — 스킬 설명 (오행 기운과 클래스에 맞는 구체적 효과 1~2문장)
2. [스킬명] — 스킬 설명
3. [스킬명] — 스킬 설명

【성격 및 기질】
(천간의 음양과 계절의 기운을 반영한 성격 묘사. 장점 2가지, 단점 1가지를 자연스럽게 녹여서 2~3문장.)

【선천적 운명】
(이 사주를 가진 사람이 타고난 사명과 시련. 오행 상생상극 관계를 스토리텔링으로 풀어서 2~3문장.)

---
마크다운 없이 위 양식 그대로 출력하세요. 수치는 오행 속성과 클래스 특성에 맞게 반드시 다르게 설정하세요.
""",
                name, genderKr, year, month, day, time, place, element, className, title);
    }
}
