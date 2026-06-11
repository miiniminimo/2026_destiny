package com.destinycode.saju;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 사주 원국에서 주요 신살(神煞)·귀성(貴星)을 판정합니다.
 *
 * 일간(日干)과 년/월/일/시 지지를 기준으로 한 전통 명식 신살표를 사용합니다.
 */
final class ShenShaCalculator {

    private ShenShaCalculator() {
    }

    /** 천을귀인(天乙貴人): 일간 기준 귀인 지지 */
    private static final Map<String, List<String>> CHEONEUL_GUIIN = Map.of(
            "甲", List.of("丑", "未"), "戊", List.of("丑", "未"), "庚", List.of("丑", "未"),
            "乙", List.of("子", "申"), "己", List.of("子", "申"),
            "丙", List.of("亥", "酉"), "丁", List.of("亥", "酉"),
            "辛", List.of("寅", "午"),
            "壬", List.of("巳", "卯"), "癸", List.of("巳", "卯")
    );

    /** 양인살(羊刃殺): 일간 기준 (양간만 해당) */
    private static final Map<String, String> YANGIN_SAL = Map.of(
            "甲", "卯", "丙", "午", "戊", "午", "庚", "酉", "壬", "子"
    );

    /** 문창귀인(文昌貴人): 일간 기준 */
    private static final Map<String, String> MUNCHANG_GUIIN = Map.ofEntries(
            Map.entry("甲", "巳"), Map.entry("乙", "午"), Map.entry("丙", "申"), Map.entry("丁", "酉"),
            Map.entry("戊", "申"), Map.entry("己", "酉"), Map.entry("庚", "亥"), Map.entry("辛", "子"),
            Map.entry("壬", "寅"), Map.entry("癸", "卯")
    );

    /** 도화살(桃花殺): 년지 삼합 그룹 기준 */
    private static final Map<String, String> DOHWA_SAL = Map.ofEntries(
            Map.entry("申", "酉"), Map.entry("子", "酉"), Map.entry("辰", "酉"),
            Map.entry("寅", "卯"), Map.entry("午", "卯"), Map.entry("戌", "卯"),
            Map.entry("巳", "午"), Map.entry("酉", "午"), Map.entry("丑", "午"),
            Map.entry("亥", "子"), Map.entry("卯", "子"), Map.entry("未", "子")
    );

    /** 역마살(驛馬殺): 년지 삼합 그룹 기준 */
    private static final Map<String, String> YEOKMA_SAL = Map.ofEntries(
            Map.entry("申", "寅"), Map.entry("子", "寅"), Map.entry("辰", "寅"),
            Map.entry("寅", "申"), Map.entry("午", "申"), Map.entry("戌", "申"),
            Map.entry("巳", "亥"), Map.entry("酉", "亥"), Map.entry("丑", "亥"),
            Map.entry("亥", "巳"), Map.entry("卯", "巳"), Map.entry("未", "巳")
    );

    /** 화개살(華蓋殺): 년지 삼합 그룹 기준 */
    private static final Map<String, String> HWAGAE_SAL = Map.ofEntries(
            Map.entry("申", "辰"), Map.entry("子", "辰"), Map.entry("辰", "辰"),
            Map.entry("寅", "戌"), Map.entry("午", "戌"), Map.entry("戌", "戌"),
            Map.entry("巳", "丑"), Map.entry("酉", "丑"), Map.entry("丑", "丑"),
            Map.entry("亥", "未"), Map.entry("卯", "未"), Map.entry("未", "未")
    );

    /** 괴강살(魁罡殺): 일주 干支 자체로 판정 */
    private static final Set<String> GOEGANG_SAL = Set.of("庚辰", "庚戌", "壬辰", "戊戌");

    /**
     * @param dayGan    일간 (예: "甲")
     * @param dayGanZhi 일주 干支 (예: "庚辰")
     * @param yearZhi   년지
     * @param pillarZhi 원국에 존재하는 모든 지지 (년/월/일/(시))
     * @return 발견된 신살/귀성 이름 목록 (중복 없음, 발견 순서 유지)
     */
    static List<String> calculate(String dayGan, String dayGanZhi, String yearZhi, List<String> pillarZhi) {
        List<String> result = new ArrayList<>();

        if (CHEONEUL_GUIIN.getOrDefault(dayGan, List.of()).stream().anyMatch(pillarZhi::contains)) {
            result.add("천을귀인(天乙貴人)");
        }
        if (pillarZhi.contains(YANGIN_SAL.get(dayGan))) {
            result.add("양인살(羊刃殺)");
        }
        if (pillarZhi.contains(MUNCHANG_GUIIN.get(dayGan))) {
            result.add("문창귀인(文昌貴人)");
        }
        if (pillarZhi.contains(DOHWA_SAL.get(yearZhi))) {
            result.add("도화살(桃花殺)");
        }
        if (pillarZhi.contains(YEOKMA_SAL.get(yearZhi))) {
            result.add("역마살(驛馬殺)");
        }
        if (pillarZhi.contains(HWAGAE_SAL.get(yearZhi))) {
            result.add("화개살(華蓋殺)");
        }
        if (GOEGANG_SAL.contains(dayGanZhi)) {
            result.add("괴강살(魁罡殺)");
        }

        return result;
    }
}
