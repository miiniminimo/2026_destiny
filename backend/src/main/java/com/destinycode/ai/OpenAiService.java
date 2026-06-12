package com.destinycode.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
public class OpenAiService {

    private static final String API_URL = "https://api.openai.com/v1/images/generations";

    private final String apiKey;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiService(@Value("${ai.openai.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * DALL-E 3으로 "내가 신이라면?" 컨셉의 캐릭터 일러스트를 생성하고 Base64로 반환합니다.
     */
    public String generateCharacterImage(String name, String gender, String element,
                                         String className, String title, String description) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OPENAI_API_KEY가 설정되지 않아 이미지 생성을 건너뜁니다.");
            return null;
        }

        String prompt = buildPrompt(name, gender, element, className, title, description);
        log.info("DALL-E 3 이미지 생성 요청 - 클래스: {}", className);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = Map.of(
                    "model", "dall-e-3",
                    "prompt", prompt,
                    "n", 1,
                    "size", "1024x1792",
                    "response_format", "url"
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(API_URL, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String imageUrl = root.path("data").get(0).path("url").asText();
                log.info("DALL-E 3 이미지 생성 성공");

                byte[] imageBytes = restTemplate.getForObject(imageUrl, byte[].class);
                if (imageBytes != null) {
                    return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
                }
            }
        } catch (Exception e) {
            log.error("OpenAI DALL-E 3 이미지 생성 오류: {}", e.getMessage(), e);
        }

        return null;
    }

    private String buildPrompt(String name, String gender, String element, String className,
                                String title, String description) {
        String genderKr = "MALE".equals(gender) ? "남성" : "여성";
        String trimmedDesc = description.length() > 600 ? description.substring(0, 600) : description;

        return String.format("""
                masterpiece, ultra detailed, 8K, emotionally layered illustration, watercolor fashion illustration, \
                poetic visual atmosphere, museum-grade illustration aesthetic, soft luxury aesthetic, \
                high-end fashion magazine aesthetic, elegant minimal composition, emotionally powerful visual storytelling.

                [화풍] 투명한 수채화, 수채화 염색, 잉크 자국, 손으로 그린 질감, 선명하고 섬세한 필체, 선명하고 섬세한 스케치, \
                초고화질, 색채의 투명감, 캐릭터의 특징 선명함, 자연스러운 감정 표정, 역동적인 자세, 전신 구성, 의상 디테일이 \
                섬세하고 부드러운 영화급 빛과 그림자, 강한 분위기감, 높은 디테일, 얼굴 디테일이 섬세하다.

                [캐릭터 설정 - 내가 신이라면?]
                이 인물은 %s이며, 사주 분석 결과 아래와 같은 신격으로 재탄생한다.
                - 오행 속성: %s
                - 신격(클래스): %s
                - 칭호: %s
                - 성격/능력/배경 분석:
                %s

                위 분석을 바탕으로 이 인물이 신이 되었을 때의 모습을 그려라. 신의 이름, 성격, 신분, 능력, 상징물, 배경이야기가 \
                의상, 표정, 눈빛, 빛과 그림자, 배경 구성에 자연스럽게 드러나야 한다. 평범하고 구원자적인 "신은 세상을 사랑한다" \
                식의 클리셰가 아니라, 위 분석에서 드러난 인물 고유의 개성과 어두운 면까지 극단적으로 반영한 독자적인 신격으로 \
                표현하라.

                [지면 포맷]
                그림을 중심으로 하고 텍스트와 정보를 보조로 배치한다. 모든 텍스트는 한글로, 명확하고 읽을 수 있게 표기한다. \
                제목 "내가 신이라면?"과 함께 신의 이름, 성격, 신분, 능력, 상징물, 배경이야기, 취향, 싫어하는 것, 고전 명언을 \
                포함한다.

                [금지] 일본어, 문자 흐림, 간체 중국어, 오타, 문자 혼란, 3DCG 느낌, 과도한 AI 미녀감, 과도한 사진 수준의 사실감.
                """,
                genderKr, element, className, title, trimmedDesc);
    }
}
