package com.destinycode.user;

import com.destinycode.user.dto.SajuRequest;
import com.destinycode.user.dto.SajuResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SajuService {

    private final SajuRepository sajuRepository;
    private final UserRepository userRepository;

    @Transactional
    public SajuResponse saveSajuInfo(String email, SajuRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 기존 사주 정보가 있으면 업데이트, 없으면 신규 생성
        SajuInfo sajuInfo = sajuRepository.findByUser(user)
                .orElse(SajuInfo.builder().user(user).build());

        sajuInfo.setName(request.getName());
        sajuInfo.setGender(request.getGender());
        sajuInfo.setCalendarType(request.getCalendarType());
        sajuInfo.setBirthYear(request.getBirthYear());
        sajuInfo.setBirthMonth(request.getBirthMonth());
        sajuInfo.setBirthDay(request.getBirthDay());
        sajuInfo.setBirthTime(request.getBirthTime());
        sajuInfo.setBirthPlace(request.getBirthPlace());

        SajuInfo savedSajuInfo = sajuRepository.save(sajuInfo);

        // 사주 맞춤 가상 캐릭터 메타데이터 분석 생성
        SajuResponse.CharacterSummary summary = generateMockCharacter(request);

        return SajuResponse.fromEntity(savedSajuInfo, summary);
    }

    @Transactional(readOnly = true)
    public SajuResponse getSajuInfo(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return sajuRepository.findByUser(user)
                .map(sajuInfo -> {
                    // 저장된 사주를 조회할 때 DTO로 변환
                    SajuRequest req = SajuRequest.builder()
                            .name(sajuInfo.getName())
                            .gender(sajuInfo.getGender())
                            .calendarType(sajuInfo.getCalendarType())
                            .birthYear(sajuInfo.getBirthYear())
                            .birthMonth(sajuInfo.getBirthMonth())
                            .birthDay(sajuInfo.getBirthDay())
                            .birthTime(sajuInfo.getBirthTime())
                            .birthPlace(sajuInfo.getBirthPlace())
                            .build();

                    SajuResponse.CharacterSummary summary = generateMockCharacter(req);
                    return SajuResponse.fromEntity(sajuInfo, summary);
                })
                .orElse(null);
    }

    /**
     * 사주 데이터를 만세력 기준과 유사하게 재미있는 룰로 해석하여 가상 RPG 캐릭터 요약을 생성합니다.
     */
    private SajuResponse.CharacterSummary generateMockCharacter(SajuRequest req) {
        int yearEndDigit = req.getBirthYear() % 10;
        String element;
        String className;
        String title;
        String description;

        // 1. 태어난 연도 끝자리로 오행(속성) 및 동양 무속 클래스 분류
        switch (yearEndDigit) {
            case 0: case 1:
                element = "금 (Metal)";
                className = req.getGender().equals("MALE") ? "명부의 저승사자(使者)" : "성스러운 삼신선녀(三神仙女)";
                title = "명부의 룰을 다스리는 초절정 비주얼";
                description = "탄생 연도의 예리하고 차가운 금(金)의 기운을 얻어, K-POP 센터 멤버처럼 조각 같고 매혹적인 냉미남/냉미녀 비주얼을 자랑합니다. 날카로운 철검과 음양의 부적을 활용해 사악한 영을 정화합니다.";
                break;
            case 2: case 3:
                element = "수 (Water)";
                className = req.getGender().equals("MALE") ? "대나무숲 청룡도사" : "심연의 용신무녀(龍神巫女)";
                title = "신령스러운 해류를 지배하는 카리스마";
                description = "탄생 연도의 깊고 고요한 수(水)의 영성을 타고나 독보적인 아우라를 풍깁니다. 용신의 방울을 흔들며 비를 부르고 어둠을 차단하는 세련되고 화려한 주술로 전장을 장악합니다.";
                break;
            case 4: case 5:
                element = "목 (Wood)";
                className = req.getGender().equals("MALE") ? "청운의 옥피리 도사" : "버드나무 백호선녀";
                title = "동방의 생명력을 개척하는 수려한 선인";
                description = "탄생 연도의 푸르른 목(木)의 청명하고 싱그러운 생명력을 물려받아, K-POP 요정돌 같은 화사하고 매력적인 오라를 지닙니다. 신령의 부채와 피리 소리로 아군의 영혼을 치유하고 적의 운명을 뒤바꿉니다.";
                break;
            case 6: case 7:
                element = "화 (Fire)";
                className = req.getGender().equals("MALE") ? "벼락 불꽃의 천우신장(神將)" : "붉은 봉황의 지옥 화무(巫堂)";
                title = "신당의 불꽃을 각성한 화려한 리드 보컬";
                description = "탄생 연도의 뜨겁고 찬란한 화(火)의 불꽃을 내려받아, 무대 위 독무를 펼치는 K-POP 아이돌처럼 매혹적이고 강렬한 비주얼을 지닙니다. 오색 깃발과 불타는 방울을 흔들어 온갖 악귀를 일시에 퇴마합니다.";
                break;
            default: // 8, 9
                element = "토 (Earth)";
                className = req.getGender().equals("MALE") ? "바위 성벽의 태수장군(大將軍)" : "지리산 산신녀(山神女)";
                title = "대지의 무게를 견디는 든든한 리더";
                description = "탄생 연도의 묵직하고 강건한 토(土)의 기운이 깃들어, 팀을 든든하게 이끄는 K-POP 리더처럼 강건하고 수려한 성품을 가졌습니다. 지리산 영산의 기운으로 든든한 가호의 장벽을 칩니다.";
                break;
        }

        // 2. 태어난 시간에 따라 무속 및 지간(地支)의 음양 에너지 설명 추가
        String timeModifier = "";
        if (req.getBirthTime() != null) {
            String[] parts = req.getBirthTime().split(":");
            int hour = Integer.parseInt(parts[0]);
            if (hour >= 6 && hour < 12) {
                timeModifier = " 인시/묘시(여명)에 동트는 태양의 정기를 받아 사령을 쫓는 광명 오라가 발동합니다.";
            } else if (hour >= 12 && hour < 18) {
                timeModifier = " 사시/오시(정오)의 가장 뜨거운 천간 양기를 온몸에 가득 채워, 치명타 시 작렬하는 신성한 벼락 대미지가 추가됩니다.";
            } else if (hour >= 18 && hour < 24) {
                timeModifier = " 신시/유시(황혼)의 매혹적인 도화 기운을 흡수하여 적들을 혼란에 빠뜨리는 영혼 매혹 패시브가 극대화되었습니다.";
            } else {
                timeModifier = " 자시/축시(심야)의 명부의 문이 열리는 기운 속에서 영안(靈眼)을 뜨게 되어, 은신한 적의 약점을 꿰뚫어 보는 안목이 생겼습니다.";
            }
        } else {
            timeModifier = " 태어난 시간이 신비로운 안개 장막에 싸여 있어, 예측 불가능하고 조화로운 변칙 영력 부적술을 장착했습니다.";
        }

        description += timeModifier + " 대한민국 영산과 줄기의 정기가 깃든 " + req.getBirthPlace() + "의 신령스러운 가호가 모태에 스며들어 직업 시너지가 대폭 활성화되었습니다.";

        return SajuResponse.CharacterSummary.builder()
                .element(element)
                .className(className)
                .title(title)
                .description(description)
                .build();
    }
}
