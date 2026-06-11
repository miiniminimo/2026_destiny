package com.destinycode.saju;

import java.util.List;

/**
 * 만세력 기준으로 산출한 사주팔자(四柱八字) 원국 정보.
 *
 * @param yearPillar  년주 (干支)
 * @param monthPillar 월주 (干支)
 * @param dayPillar   일주 (干支)
 * @param timePillar  시주 (干支), 태어난 시간이 없으면 null
 * @param dayGan      일간 (日干, 본인을 상징하는 천간)
 * @param dayGanZhi   일주 干支 (괴강살 등 판정용)
 * @param monthZhi    월지 (月支)
 * @param dayJiSe     일주 지세 (十二運星, 예: 장생/제왕/묘 등)
 * @param shenSha     원국에서 발견된 신살(神煞)/귀성(貴星) 목록
 */
public record SajuPillars(
        String yearPillar,
        String monthPillar,
        String dayPillar,
        String timePillar,
        String dayGan,
        String dayGanZhi,
        String monthZhi,
        String dayJiSe,
        List<String> shenSha
) {
}
