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
                    "max_tokens", 4096,
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

이 프로젝트의 핵심 목표는 "어려운 사주 명리학 용어를 쉬운 게임 용어로 번역해서 보여주는 것"입니다.
따라서 아래 [입력 정보]에 한자(甲乙丙丁戊己庚辛壬癸, 寅卯辰巳午未申酉戌亥子丑 등)와 명리학 전문 용어
(일간, 십이운성, 비견·겁재·식신·상관·편재·정재·편관·정관·편인·정인, 신살/귀성의 한자 표기 등)가
포함되어 있더라도, **출력 보고서에는 한자와 명리학 전문 용어를 그대로 노출하지 마세요.**
대신 아래 [용어 변환 가이드]를 참고하여 RPG 게임 속 능력치·특성·스킬 이름처럼 쉽고 직관적인 표현으로
완전히 풀어서 설명하세요.

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

[용어 변환 가이드]
- 오행(木火土金水)은 "성장/생명력(木)", "열정/추진력(火)", "안정/포용력(土)", "결단력/명예(金)", "지혜/유연함(水)" 같은
  쉬운 기운 이름으로 표현하세요.
- 십성(비견·겁재·식신·상관·편재·정재·편관·정관·편인·정인)은 한자/명칭을 쓰지 말고, 아래처럼 능력치 개념으로 변환하세요.
  - 비견·겁재 → "자아/경쟁 능력치" (독립심, 자기주장, 경쟁심)
  - 식신·상관 → "표현/창작 능력치" (재능, 끼, 표현력)
  - 편재·정재 → "현실/관리 능력치" (재물 감각, 실행력, 현실 감각)
  - 편관·정관 → "리더십/책임 능력치" (규율, 책임감, 통솔력)
  - 편인·정인 → "직관/학습 능력치" (영감, 통찰력, 학습 능력)
- 십이운성(일주 지세)은 "캐릭터의 성장 단계"로 표현하세요 (예: 갓 태어난 신인 단계, 한창 활약하는 전성기 단계,
  경험이 쌓인 노련한 단계 등).
- 신살/귀성은 한자 이름을 그대로 쓰지 말고 "특수 패시브 스킬" 이름처럼 별명을 새로 지어서 표현하세요
  (예: 역마살 → "이동의 축복", 도화살 → "매력 발산 오라", 천을귀인 → "수호의 가호" 등 자유롭게 재해석).

위 사주 원국을 바탕으로 아래 형식을 **정확히** 지켜 분석 보고서를 작성하세요.
각 항목은 위에 주어진 사주팔자, 일간, 십이운성, 신살/귀성 정보를 최대한 구체적으로 반영해야 하지만,
한자나 명리학 전문 용어는 절대 그대로 노출하지 말고 위 가이드에 따라 쉬운 게임 용어로 완전히 변환하세요.

---

【캐릭터 소개】
(이 캐릭터가 어떤 존재인지, 출생지와 기운이 어떻게 작용했는지 3~4문장으로 서술. K-POP 아이돌의 화려한 비주얼과 전통 무속의 신비로움을 함께 묘사. 한자 사용 금지.)

【능력치 분석】
위 사주를 [용어 변환 가이드]의 5가지 능력치(자아/경쟁, 표현/창작, 현실/관리, 리더십/책임, 직관/학습)로 변환했을 때
어떤 능력치가 매우 높은지(과다), 높은지(발달), 적정한지, 부족한지를 짚어주고, 이를 바탕으로 핵심 성향과 태도를
2~3문장으로 쉽게 설명하세요. 한자나 "비견", "정관" 같은 명리학 용어는 쓰지 마세요.

【특수 능력 분석】
위에 주어진 신살/귀성이 있다면, 그것을 한자 이름이 아닌 새로운 "패시브 스킬" 이름으로 재해석하여
그 스킬이 만들어내는 독특한 매력·성향·행동 패턴을 무속적 RPG 세계관에 녹여 2~3문장으로 설명하세요.
신살이 없다면 캐릭터의 성장 단계(십이운성)를 바탕으로 고유 스킬을 하나 창작해 설명하세요.

【종합 보고서】
- 핵심 기질: (비유적 표현으로 한 문장 정의)
- 장점: (2가지)
- 보완할 점: (1가지)
- 한 줄 타이틀: (이 인물에게 어울리는 칭호 형태의 한 줄)
- 핵심 키워드: (3개, 쉼표로 구분)
- 대화형 질문: (사용자와의 소통을 유도하는 질문 1개)

---
마크다운 기호(#, *, - 등) 없이 위 양식의 【】 제목만 사용해 그대로 출력하세요.
한자(漢字)는 단 한 글자도 출력하지 마세요. 내용은 오행 속성과 클래스 특성에 맞게 캐릭터마다 반드시 다르게 작성하세요.
""",
                name, genderKr, year, month, day, time, place, element, className, title,
                pillarsText, pillars.dayGan(), pillars.dayJiSe(), shenShaText);
    }
}
