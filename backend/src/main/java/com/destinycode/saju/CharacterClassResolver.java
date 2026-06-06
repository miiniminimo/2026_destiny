package com.destinycode.saju;

import org.springframework.stereotype.Component;

/**
 * 천간(天干) × 계절(季節) × 일진(日辰) 조합으로 RPG 클래스를 결정합니다.
 *
 * 분류 기준:
 *  - 천간: 연도 끝자리 → 10 heavenly stems (甲乙丙丁戊己庚辛壬癸)
 *  - 계절: 생월 → 봄/여름/가을/겨울
 *  - 일진: 생일 → 초순(1-10) / 중순(11-20) / 말순(21-31)
 *
 * 오행 상생상극을 반영해 천간과 계절 오행의 관계로 클래스 성향을 결정:
 *  - 동기(同氣): 순수 속성 극대화
 *  - 상생(相生): 조화로운 강화
 *  - 상극(相克): 역경을 딛은 강인함
 */
@Component
public class CharacterClassResolver {

    // ─── 천간 ─────────────────────────────────────────────────────────────────

    public enum Stem {
        GAP_WOOD  (4, "甲", "木", "양목", "우뚝 선 거목"),
        EUL_WOOD  (5, "乙", "木", "음목", "유연한 덩굴풀"),
        BYUNG_FIRE(6, "丙", "火", "양화", "타오르는 태양"),
        JUNG_FIRE (7, "丁", "火", "음화", "은은한 촛불"),
        MU_EARTH  (8, "戊", "土", "양토", "웅장한 산악"),
        GI_EARTH  (9, "己", "土", "음토", "비옥한 대지"),
        GYEONG_METAL(0, "庚", "金", "양금", "단단한 강철"),
        SIN_METAL (1, "辛", "金", "음금", "섬세한 보석"),
        IM_WATER  (2, "壬", "水", "양수", "웅장한 강과 바다"),
        GYE_WATER (3, "癸", "水", "음수", "신비로운 이슬과 비");

        final int yearDigit;
        final String hanja;
        final String element;
        final String yinYang;
        final String nature;

        Stem(int yearDigit, String hanja, String element, String yinYang, String nature) {
            this.yearDigit = yearDigit;
            this.hanja = hanja;
            this.element = element;
            this.yinYang = yinYang;
            this.nature = nature;
        }

        static Stem of(int birthYear) {
            int d = birthYear % 10;
            for (Stem s : values()) if (s.yearDigit == d) return s;
            return GAP_WOOD;
        }
    }

    // ─── 계절 ─────────────────────────────────────────────────────────────────

    public enum Season {
        SPRING("木", "봄", new int[]{3, 4, 5},  "생명이 움트는",  "각성한"),
        SUMMER("火", "여름", new int[]{6, 7, 8}, "불꽃이 타오르는", "정열의"),
        AUTUMN("金", "가을", new int[]{9, 10, 11},"서릿발 같은",   "예리한"),
        WINTER("水", "겨울", new int[]{12, 1, 2}, "심연의 고요한", "빙설의");

        final String element;
        final String name;
        final int[] months;
        final String modifier;      // 클래스명 앞 수식
        final String titlePrefix;   // 타이틀 앞 수식

        Season(String element, String name, int[] months, String modifier, String titlePrefix) {
            this.element = element;
            this.name = name;
            this.months = months;
            this.modifier = modifier;
            this.titlePrefix = titlePrefix;
        }

        static Season of(int month) {
            for (Season s : values())
                for (int m : s.months) if (m == month) return s;
            return SPRING;
        }
    }

    // ─── 일진 (생일 초순/중순/말순) ──────────────────────────────────────────

    public enum DayPhase {
        EARLY ("초순", "선봉",   "개척자의 기운을 타고났습니다"),
        MIDDLE("중순", "절정",   "전성기의 기운이 충만합니다"),
        LATE  ("말순", "전설적인", "완숙한 경지에 이른 기운입니다");

        final String name;
        final String keyword;
        final String trait;

        DayPhase(String name, String keyword, String trait) {
            this.name = name;
            this.keyword = keyword;
            this.trait = trait;
        }

        static DayPhase of(int day) {
            if (day <= 10)  return EARLY;
            if (day <= 20)  return MIDDLE;
            return LATE;
        }
    }

    // ─── 오행 관계 ────────────────────────────────────────────────────────────

    /** 천간 오행과 계절 오행의 상생상극 관계 */
    enum ElementRelation {
        SAME    ("동기"),   // 같은 오행
        GENERATE("상생"),   // 천간이 계절을 생함 (木→火 등)
        DESTROY ("상극"),   // 천간이 계절을 극함
        GENERATED_BY("피생"), // 계절이 천간을 생함
        DESTROYED_BY("피극"); // 계절이 천간을 극함

        final String label;
        ElementRelation(String label) { this.label = label; }

        static ElementRelation of(String stemEl, String seasonEl) {
            if (stemEl.equals(seasonEl)) return SAME;
            // 상생: 木→火→土→金→水→木
            if (generates(stemEl, seasonEl)) return GENERATE;
            if (generates(seasonEl, stemEl)) return GENERATED_BY;
            if (destroys(stemEl, seasonEl))  return DESTROY;
            return DESTROYED_BY;
        }

        private static boolean generates(String from, String to) {
            return switch (from) {
                case "木" -> "火".equals(to);
                case "火" -> "土".equals(to);
                case "土" -> "金".equals(to);
                case "金" -> "水".equals(to);
                case "水" -> "木".equals(to);
                default   -> false;
            };
        }

        private static boolean destroys(String from, String to) {
            return switch (from) {
                case "木" -> "土".equals(to);
                case "火" -> "金".equals(to);
                case "土" -> "水".equals(to);
                case "金" -> "木".equals(to);
                case "水" -> "火".equals(to);
                default   -> false;
            };
        }
    }

    // ─── 메인 해석 ────────────────────────────────────────────────────────────

    public record ClassResult(
            String element,
            String className,
            String title,
            String baseDescription
    ) {}

    public ClassResult resolve(int birthYear, int birthMonth, int birthDay, boolean male) {
        Stem stem       = Stem.of(birthYear);
        Season season   = Season.of(birthMonth);
        DayPhase phase  = DayPhase.of(birthDay);
        ElementRelation rel = ElementRelation.of(stem.element, season.element);

        String baseClass = baseClass(stem, male);
        String className = buildClassName(season, phase, baseClass, rel);
        String title     = buildTitle(stem, season, phase, rel, male);
        String desc      = buildDesc(stem, season, phase, rel);

        return new ClassResult(stem.element + " (" + stem.yinYang + ")", className, title, desc);
    }

    // ─── 클래스명 조합 ────────────────────────────────────────────────────────

    private String baseClass(Stem stem, boolean male) {
        return switch (stem) {
            case GAP_WOOD   -> male ? "청룡검사(靑龍劍士)"   : "청룡선녀(靑龍仙女)";
            case EUL_WOOD   -> male ? "옥피리 도사(玉笛道士)" : "버드나무 선녀(仙女)";
            case BYUNG_FIRE -> male ? "천우신장(天佑神將)"    : "봉황화무(鳳凰巫堂)";
            case JUNG_FIRE  -> male ? "촛불부적사(符籍士)"    : "촛불무녀(燭巫女)";
            case MU_EARTH   -> male ? "태수장군(太守將軍)"    : "산신여장군(山神女將)";
            case GI_EARTH   -> male ? "황토도사(黃土道士)"    : "지모신녀(地母神女)";
            case GYEONG_METAL -> male ? "저승사자(使者)"      : "삼신선녀(三神仙女)";
            case SIN_METAL  -> male ? "보석검사(寶石劍士)"    : "백옥선녀(白玉仙女)";
            case IM_WATER   -> male ? "청룡수신(靑龍水神)"    : "용신무녀(龍神巫女)";
            case GYE_WATER  -> male ? "이슬도사(露道士)"      : "빗속무녀(雨巫女)";
        };
    }

    private String buildClassName(Season season, DayPhase phase, String base, ElementRelation rel) {
        String prefix = switch (rel) {
            case SAME          -> season.modifier + " ";
            case GENERATE      -> season.titlePrefix + " ";
            case GENERATED_BY  -> "조화로운 ";
            case DESTROY       -> "역경을 넘은 ";
            case DESTROYED_BY  -> "시련 속의 ";
        };
        return prefix + base + " [" + phase.keyword + "]";
    }

    // ─── 타이틀 조합 ──────────────────────────────────────────────────────────

    private String buildTitle(Stem stem, Season season, DayPhase phase,
                               ElementRelation rel, boolean male) {
        String relDesc = switch (rel) {
            case SAME         -> stem.element + "의 순수한 기운이 충만한";
            case GENERATE     -> stem.element + "이 " + season.element + "을 생하는 조화로운";
            case GENERATED_BY -> season.element + "의 보살핌을 받는 풍요로운";
            case DESTROY      -> stem.element + "이 " + season.element + "을 제압하는 강인한";
            case DESTROYED_BY -> season.element + "의 시련을 견디는 불굴의";
        };
        return phase.keyword + "의 " + relDesc + " " + (male ? "영웅" : "영웅");
    }

    // ─── 설명 조합 ────────────────────────────────────────────────────────────

    private String buildDesc(Stem stem, Season season, DayPhase phase, ElementRelation rel) {
        String stemDesc = stem.hanja + "(" + stem.yinYang + ") " + stem.nature + "의 기운으로 탄생했습니다.";

        String relDesc = switch (rel) {
            case SAME         -> " " + season.name + "의 동기(同氣)가 더해져 " + stem.element +
                                 "의 속성이 극한까지 증폭됩니다.";
            case GENERATE     -> " " + season.name + "의 " + season.element + " 기운이 생(生)을 더해 능력이 두 배로 강화됩니다.";
            case GENERATED_BY -> " " + season.name + "의 " + season.element + " 기운에 의해 생(生)을 받아 풍요로운 영력을 지닙니다.";
            case DESTROY      -> " " + season.name + "의 " + season.element + " 기운을 극(克)하는 강인한 제압력을 지닙니다.";
            case DESTROYED_BY -> " " + season.name + "의 " + season.element + " 기운에 의해 극(克)받으나, 그 시련이 오히려 불굴의 의지를 낳았습니다.";
        };

        return stemDesc + relDesc + " " + phase.trait + ".";
    }
}
