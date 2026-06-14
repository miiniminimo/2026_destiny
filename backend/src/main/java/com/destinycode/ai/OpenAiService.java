package com.destinycode.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
     * gpt-image-1로 사주 분석 기반 신격 캐릭터 일러스트를 생성하고 Base64로 반환합니다.
     */
    public String generateCharacterImage(String name, String gender, String element,
                                         String className, String title, String description) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OPENAI_API_KEY가 설정되지 않아 이미지 생성을 건너뜁니다.");
            return null;
        }
        if (description == null || description.isBlank()) {
            log.warn("사주 분석 결과가 없어 이미지 생성을 건너뜁니다.");
            return null;
        }

        String prompt = buildPrompt(name, gender, element, className, title, description);
        log.info("gpt-image-1 이미지 생성 요청 - 클래스: {}", className);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = Map.of(
                    "model", "gpt-image-1",
                    "prompt", prompt,
                    "n", 1,
                    "size", "1024x1536",
                    "quality", "high"
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(API_URL, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String base64Json = root.path("data").get(0).path("b64_json").asText();
                log.info("gpt-image-1 이미지 생성 성공");

                if (base64Json != null && !base64Json.isBlank()) {
                    return "data:image/png;base64," + base64Json;
                }
            }
        } catch (Exception e) {
            log.error("OpenAI gpt-image-1 이미지 생성 오류: {}", e.getMessage(), e);
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
                high-end fashion magazine aesthetic, elegant ornate composition, emotionally powerful visual storytelling, \
                intricate gold linework, fantasy character concept art, full-body dynamic pose.

                [화풍] 투명한 수채화, 수채화 염색, 잉크 자국, 손으로 그린 질감, 선명하고 섬세한 필체, 선명하고 섬세한 스케치, \
                초고화질, 색채의 투명감, 캐릭터의 특징 선명함, 자연스러운 감정 표정, 역동적인 자세, 전신 구성, 의상 디테일이 \
                섬세하고 부드러운 영화급 빛과 그림자, 강한 분위기감, 높은 디테일, 얼굴 디테일이 섬세하다. \
                금색/은색의 섬세한 장식 선과 동양 전통 문양이 옷과 배경에 정교하게 둘러져 있다.

                [세계관/스타일 방향성] 이 작품의 세계관은 한국 전통 무속(巫俗) 신화를 기반으로 한 동양풍이다. \
                서양식 그림 리퍼(긴 낫을 든 해골/후드 차림), 서양 마법사 로브, 타로 카드, 서양 별자리 마법진, \
                고딕풍 까마귀·촛대 등 서양 오컬트 도상은 사용하지 말 것. 대신 한복/도포/곤룡포/무복 등을 변형한 \
                동양풍 의상, 한지·단청·오방색(청·적·황·백·흑) 배색, 동양 용·호랑이·구미호·청룡·삼족오 같은 동양 \
                설화 속 영적 동물, 부채·방울·한지 부적·연꽃·구름 문양 같은 동양 무속 상징, 동양화 느낌의 \
                구름·산수·달 배경을 활용하여 신격을 표현한다.

                [사주 분석 기반 인물 정보]
                성별: %s
                오행 속성: %s
                신격(클래스): %s
                칭호: %s
                사주 분석(성격, 행동, 세계관, 가치관, 사랑관, 분위기, 감정의 온도, 사고 성향, 흥미, 선악관, 어두운 면 포함):
                %s

                [핵심 콘셉트]
                위 사주 분석을 바탕으로, 이 인물이 신격(神格)을 얻은 모습을 그려낸다.
                단, "신은 세상을 사랑한다"는 식의 평범하고 구원자적인 클리셰는 금지한다.
                이 신이 사람들을 사랑하는 방식은 보통 사람의 이해 범위를 벗어나도 좋다. \
                신은 구원할 수도 있지만, 그 구원은 신 자신의 논리와 신념에 따른 것일 뿐이다. \
                백성들은 이 신을 두려워하거나, 숭배하거나, 동경할 수 있으며, 그 관계는 일반적인 선의의 범주를 벗어날 수도 있다. \
                신의 성격은 사주 분석에서 드러난 인물의 특성과 일치해야 하며, 그 개성과 어두운 면까지 극단적으로 증폭하여 \
                독자적인 신격으로 표현하라. 신의 모습, 옷차림, 표정, 눈빛, 빛과 그림자의 표현, 배경은 모두 위 분석에서 \
                자연스럽게 도출되어야 한다.

                [구성 요소] 인물 주변에는 그 신격의 능력과 상징을 암시하는 동양풍 사물들(예: 한지 부적, 방울, \
                청동 거울, 연꽃, 학·호랑이·용·구미호 같은 영적 동물, 향로, 비단 천, 죽간/고서, 구름 문양 등 - \
                사주 분석에 맞게 자유롭게 선정)을 정교하게 배치하고, 인물의 뒤에는 금색/은색의 동양 전통 \
                문양(단청, 태극, 구름, 산수)으로 이루어진 신성한 빛의 원이나 후광을 그려 화면을 풍성하고 \
                화려하게 채운다. 인물은 단순히 서 있는 자세가 아니라, 시선·손짓·옷자락의 흐름에서 감정과 \
                서사가 느껴지는 역동적인 포즈를 취한다.

                [지면 포맷] 오직 인물과 위에서 설명한 상징적 구성 요소·배경 장식만으로 화면을 가득 채운다. \
                제목, 이름, 능력, 배경이야기 등 어떤 텍스트나 타이포그래피, UI 요소, 캡션, 패널 구분선도 \
                이미지에 절대 포함하지 않는다.

                [금지] 모든 형태의 글자/문자(한글, 영어, 한자, 일본어 등), 텍스트 박스, 워터마크, 로고, 3DCG 느낌, \
                과도한 AI 미녀감, 과도한 사진 수준의 사실감, 단순하고 빈 배경.
                """,
                genderKr, element, className, title, trimmedDesc);
    }
}
