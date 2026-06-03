package com.destinycode.user.dto;

import com.destinycode.user.SajuInfo;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SajuResponse {

    private Long id;
    private String name;
    private String gender;
    private String calendarType;
    private Integer birthYear;
    private Integer birthMonth;
    private Integer birthDay;
    private String birthTime;
    private String birthPlace;

    // 가상 캐릭터 메타데이터 (추후 AI 캐릭터 생성의 뼈대가 됨)
    private CharacterSummary characterSummary;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CharacterSummary {
        private String className; // 예: "화염의 마법사", "대지의 기사" 등
        private String title;     // 예: "운명을 개척하는 자"
        private String element;   // 오행 중 하나 (목, 화, 토, 금, 수)
        private String description;
    }

    public static SajuResponse fromEntity(SajuInfo sajuInfo, CharacterSummary characterSummary) {
        return SajuResponse.builder()
                .id(sajuInfo.getId())
                .name(sajuInfo.getName())
                .gender(sajuInfo.getGender())
                .calendarType(sajuInfo.getCalendarType())
                .birthYear(sajuInfo.getBirthYear())
                .birthMonth(sajuInfo.getBirthMonth())
                .birthDay(sajuInfo.getBirthDay())
                .birthTime(sajuInfo.getBirthTime())
                .birthPlace(sajuInfo.getBirthPlace())
                .characterSummary(characterSummary)
                .build();
    }
}
