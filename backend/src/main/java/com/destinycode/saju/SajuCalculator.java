package com.destinycode.saju;

import com.destinycode.common.exception.BusinessException;
import com.nlf.calendar.EightChar;
import com.nlf.calendar.Lunar;
import com.nlf.calendar.Solar;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 만세력(萬歲曆) 기준으로 사주팔자(연/월/일/시 干支)와 신살을 계산합니다.
 *
 * - SOLAR: 양력 생일 그대로 절기 기준 사주 산출
 * - LUNAR_PLAIN / LUNAR_LEAP: 음력 생일을 양력으로 환산 후 사주 산출 (윤달은 음수 월로 처리)
 * - 태어난 시간이 없으면 시주(時柱)는 산출하지 않습니다.
 */
@Component
public class SajuCalculator {

    public SajuPillars calculate(String calendarType, int year, int month, int day, String birthTime) {
        boolean hasTime = birthTime != null && !birthTime.isBlank();
        int hour = 0;
        int minute = 0;
        if (hasTime) {
            String[] parts = birthTime.split(":");
            hour = Integer.parseInt(parts[0]);
            minute = Integer.parseInt(parts[1]);
        }

        Lunar lunar;
        try {
            lunar = switch (calendarType) {
                case "SOLAR" -> Solar.fromYmdHms(year, month, day, hour, minute, 0).getLunar();
                case "LUNAR_PLAIN" -> Lunar.fromYmdHms(year, month, day, hour, minute, 0);
                case "LUNAR_LEAP" -> Lunar.fromYmdHms(year, -month, day, hour, minute, 0);
                default -> throw BusinessException.badRequest("달력 기준이 올바르지 않습니다.");
            };
        } catch (IllegalArgumentException e) {
            throw BusinessException.badRequest("생년월일이 만세력 기준으로 유효하지 않습니다.");
        }

        EightChar eightChar = lunar.getEightChar();

        String yearPillar = eightChar.getYear();
        String monthPillar = eightChar.getMonth();
        String dayPillar = eightChar.getDay();
        String timePillar = hasTime ? eightChar.getTime() : null;

        String dayGan = eightChar.getDayGan();
        String dayGanZhi = dayPillar;
        String yearZhi = eightChar.getYearZhi();
        String monthZhi = eightChar.getMonthZhi();
        String dayJiSe = eightChar.getDayDiShi();

        List<String> pillarZhi = new ArrayList<>(List.of(
                eightChar.getYearZhi(), eightChar.getMonthZhi(), eightChar.getDayZhi()
        ));
        if (hasTime) {
            pillarZhi.add(eightChar.getTimeZhi());
        }

        List<String> shenSha = ShenShaCalculator.calculate(dayGan, dayGanZhi, yearZhi, pillarZhi);

        return new SajuPillars(yearPillar, monthPillar, dayPillar, timePillar,
                dayGan, dayGanZhi, monthZhi, dayJiSe, shenSha);
    }
}
