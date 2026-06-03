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
     * DALL-E 3으로 캐릭터 이미지를 생성하고 Base64로 반환합니다.
     */
    public String generateCharacterImage(String name, String gender, String element,
                                         String className, String title) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OPENAI_API_KEY가 설정되지 않아 이미지 생성을 건너뜁니다.");
            return null;
        }

        String prompt = buildPrompt(name, gender, element, className);
        log.info("DALL-E 3 이미지 생성 요청 - 클래스: {}", className);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = Map.of(
                    "model", "dall-e-3",
                    "prompt", prompt,
                    "n", 1,
                    "size", "1024x1024",
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

    private String buildPrompt(String name, String gender, String element, String className) {
        String visualDesc = gender.equals("MALE")
                ? "extremely handsome, charming male K-pop idol"
                : "stunningly beautiful, lovely female K-pop idol";

        String clothing = resolveClothing(className);
        String skillIcons = resolveSkillIcons(element);

        return String.format(
                "High-quality 16-bit pixel art retro game screenshot of a character selection screen. " +
                "In the center, a %s %s named '%s' wearing %s. " +
                "On the right side, three square skill slots with pixel art icons: %s. " +
                "Mystical Korean background under glowing full moon and cosmic sky in deep purple. " +
                "Golden glowing 'SELECT YOUR SHAMAN' text at top. Premium fantasy RPG UI, crisp pixel art.",
                visualDesc, className, name, clothing, skillIcons
        );
    }

    private String resolveClothing(String className) {
        if (className.contains("저승사자")) return "sleek modern black Hanbok and traditional Gat hat";
        if (className.contains("선녀"))   return "elegant modern silver Hanbok with long flowing ribbons";
        if (className.contains("도사"))   return "stylish modern blue and white Hanbok playing jade flute";
        if (className.contains("무당") || className.contains("화무"))
                                          return "sleek modern fusion red and white Hanbok holding brass bell";
        return "modernized traditional Korean Hanbok with glowing gold ornaments";
    }

    private String resolveSkillIcons(String element) {
        if (element.contains("화") || element.contains("Fire"))
            return "burning red paper talisman, elemental fire fan, brass bell";
        if (element.contains("금") || element.contains("Metal"))
            return "glowing neon blue paper talisman, dark steel sword, brass bell";
        return "glowing talisman, mystical oriental weapon, Shamanic brass bell";
    }
}
