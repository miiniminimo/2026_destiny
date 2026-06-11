package com.destinycode.saju;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 만세력 기준 사주팔자(SajuPillars)를 받아 RPG 클래스를 결정합니다.
 *
 * 분류 기준:
 *  - 일간(日干): 본인을 상징하는 천간 → 캐릭터의 본질 클래스
 *  - 월지(月支) 오행: 태어난 계절의 기운 → 일간과의 상생상극으로 클래스 성향 결정
 *  - 일주 지세(十二運星): 일간의 생애 주기 단계 → 클래스 등급/타이틀 수식어
 *  - 신살(神煞)·귀성(貴星): 원국에 발견된 신살 → 캐릭터 특성/스킬
 */
@Component
public class CharacterClassResolver {

    // ─── 천간(일간) ──────────────────────────────────────────────────────────

    public enum Stem {
        GAP_WOOD  ("甲", "木", "양목", "우뚝 선 거목"),
        EUL_WOOD  ("乙", "木", "음목", "유연한 덩굴풀"),
        BYUNG_FIRE("丙", "火", "양화", "타오르는 태양"),
        JUNG_FIRE ("丁", "火", "음화", "은은한 촛불"),
        MU_EARTH  ("戊", "土", "양토", "웅장한 산악"),
        GI_EARTH  ("己", "土", "음토", "비옥한 대지"),
        GYEONG_METAL("庚", "金", "양금", "단단한 강철"),
        SIN_METAL ("辛", "金", "음금", "섬세한 보석"),
        IM_WATER  ("壬", "水", "양수", "웅장한 강과 바다"),
        GYE_WATER ("癸", "水", "음수", "신비로운 이슬과 비");

        final String hanja;
        final String element;
        final String yinYang;
        final String nature;

        Stem(String hanja, String element, String yinYang, String nature) {
            this.hanja = hanja;
            this.element = element;
            this.yinYang = yinYang;
            this.nature = nature;
        }

        static Stem of(String dayGanHanja) {
            for (Stem s : values()) if (s.hanja.equals(dayGanHanja)) return s;
            return GAP_WOOD;
        }
    }

    // ─── 월지 오행 (계절) ───────────────────────────────────────────────────

    /** 12지지가 속한 오행 */
    private static final Map<String, String> ZHI_ELEMENT = Map.ofEntries(
            Map.entry("寅", "木"), Map.entry("卯", "木"),
            Map.entry("巳", "火"), Map.entry("午", "火"),
            Map.entry("申", "金"), Map.entry("酉", "金"),
            Map.entry("亥", "水"), Map.entry("子", "水"),
            Map.entry("辰", "土"), Map.entry("戌", "土"), Map.entry("丑", "土"), Map.entry("未", "土")
    );

    public enum Season {
        SPRING("木", "봄", "생명이 움트는", "각성한"),
        SUMMER("火", "여름", "불꽃이 타오르는", "정열의"),
        AUTUMN("金", "가을", "서릿발 같은", "예리한"),
        WINTER("水", "겨울", "심연의 고요한", "빙설의"),
        TRANSITION("土", "환절기", "기운이 뒤바뀌는", "균형 잡힌");

        final String element;
        final String name;
        final String modifier;      // 클래스명 앞 수식
        final String titlePrefix;   // 타이틀 앞 수식

        Season(String element, String name, String modifier, String titlePrefix) {
            this.element = element;
            this.name = name;
            this.modifier = modifier;
            this.titlePrefix = titlePrefix;
        }

        static Season ofZhi(String monthZhi) {
            String element = ZHI_ELEMENT.getOrDefault(monthZhi, "木");
            for (Season s : values()) if (s.element.equals(element)) return s;
            return SPRING;
        }
    }

    // ─── 일주 지세 (十二運星) ────────────────────────────────────────────────

    public enum LifeStage {
        장생("탄생", "막 깨어난 신성한 기운을 타고났습니다"),
        목욕("성장", "거친 풍파 속에서 단련되고 있습니다"),
        관대("출진", "본격적으로 힘을 펼치기 시작했습니다"),
        건록("정점", "관록이 충만한 기운입니다"),
        제왕("절정", "최고조에 이른 강력한 기운을 지녔습니다"),
        쇠("노련", "노련한 경험에서 우러난 지혜를 지녔습니다"),
        병("고난", "역경을 통해 단단해진 기운입니다"),
        사("환생", "끝과 시작이 공존하는 신비로운 기운입니다"),
        묘("잠재", "깊이 잠재된 거대한 힘을 품고 있습니다"),
        절("전환", "완전히 새로운 국면을 여는 기운입니다"),
        태("태동", "새로운 가능성이 움트는 기운입니다"),
        양("양육", "보살핌 속에서 자라나는 기운입니다");

        final String keyword;
        final String trait;

        LifeStage(String keyword, String trait) {
            this.keyword = keyword;
            this.trait = trait;
        }

        /** lunar-java가 반환하는 간체자 십이운성 명칭을 한글 명칭으로 변환 */
        private static final Map<String, String> CN_TO_KR = Map.ofEntries(
                Map.entry("长生", "장생"), Map.entry("沐浴", "목욕"), Map.entry("冠带", "관대"),
                Map.entry("临官", "건록"), Map.entry("帝旺", "제왕"), Map.entry("衰", "쇠"),
                Map.entry("病", "병"), Map.entry("死", "사"), Map.entry("墓", "묘"),
                Map.entry("绝", "절"), Map.entry("胎", "태"), Map.entry("养", "양")
        );

        static LifeStage of(String diShi) {
            String normalized = CN_TO_KR.getOrDefault(diShi, diShi);
            for (LifeStage l : values()) if (l.name().equals(normalized)) return l;
            return 장생;
        }
    }

    // ─── 신살/귀성 → RPG 특성 ────────────────────────────────────────────────

    private static final Map<String, String> SHEN_SHA_TRAIT = Map.of(
            "천을귀인(天乙貴人)", "위기의 순간 귀인의 가호를 부르는 '수호 결계' 스킬을 보유합니다",
            "양인살(羊刃殺)", "거침없는 힘을 폭발시키는 '폭주 강타' 스킬을 보유합니다",
            "문창귀인(文昌貴人)", "지식과 영감을 꿰뚫어 보는 '천리안' 스킬을 보유합니다",
            "도화살(桃花殺)", "주위를 매혹하는 '매혹의 오라' 스킬을 보유합니다",
            "역마살(驛馬殺)", "공간을 가르는 '축지 이동' 스킬을 보유합니다",
            "화개살(華蓋殺)", "예술과 영성을 다루는 '영적 공명' 스킬을 보유합니다",
            "괴강살(魁罡殺)", "압도적인 카리스마로 적을 제압하는 '패왕의 위압' 스킬을 보유합니다"
    );

    // ─── 오행 관계 ────────────────────────────────────────────────────────────

    /** 일간 오행과 월지 오행의 상생상극 관계 */
    enum ElementRelation {
        SAME    ("동기"),   // 같은 오행
        GENERATE("상생"),   // 일간이 월지를 생함 (木→火 등)
        DESTROY ("상극"),   // 일간이 월지를 극함
        GENERATED_BY("피생"), // 월지가 일간을 생함
        DESTROYED_BY("피극"); // 월지가 일간을 극함

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
            String baseDescription,
            List<String> shenSha
    ) {}

    public ClassResult resolve(SajuPillars pillars, boolean male) {
        Stem stem      = Stem.of(pillars.dayGan());
        Season season  = Season.ofZhi(pillars.monthZhi());
        LifeStage stage = LifeStage.of(pillars.dayJiSe());
        ElementRelation rel = ElementRelation.of(stem.element, season.element);

        String baseClass = baseClass(stem, male);
        String className = buildClassName(season, stage, baseClass, rel);
        String title      = buildTitle(stem, season, stage, rel, male);
        String desc       = buildDesc(stem, season, stage, rel, pillars);

        return new ClassResult(stem.element + " (" + stem.yinYang + ")", className, title, desc, pillars.shenSha());
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

    private String buildClassName(Season season, LifeStage stage, String base, ElementRelation rel) {
        String prefix = switch (rel) {
            case SAME          -> season.modifier + " ";
            case GENERATE      -> season.titlePrefix + " ";
            case GENERATED_BY  -> "조화로운 ";
            case DESTROY       -> "역경을 넘은 ";
            case DESTROYED_BY  -> "시련 속의 ";
        };
        return prefix + base + " [" + stage.keyword + "]";
    }

    // ─── 타이틀 조합 ──────────────────────────────────────────────────────────

    private String buildTitle(Stem stem, Season season, LifeStage stage,
                               ElementRelation rel, boolean male) {
        String relDesc = switch (rel) {
            case SAME         -> stem.element + "의 순수한 기운이 충만한";
            case GENERATE     -> stem.element + "이 " + season.element + "을 생하는 조화로운";
            case GENERATED_BY -> season.element + "의 보살핌을 받는 풍요로운";
            case DESTROY      -> stem.element + "이 " + season.element + "을 제압하는 강인한";
            case DESTROYED_BY -> season.element + "의 시련을 견디는 불굴의";
        };
        return stage.keyword + "의 " + relDesc + " " + (male ? "영웅" : "영웅");
    }

    // ─── 설명 조합 ────────────────────────────────────────────────────────────

    private String buildDesc(Stem stem, Season season, LifeStage stage, ElementRelation rel, SajuPillars pillars) {
        String stemDesc = stem.hanja + "(" + stem.yinYang + ") " + stem.nature + "의 기운으로 탄생했습니다.";

        String relDesc = switch (rel) {
            case SAME         -> " " + season.name + "의 동기(同氣)가 더해져 " + stem.element +
                                 "의 속성이 극한까지 증폭됩니다.";
            case GENERATE     -> " " + season.name + "의 " + season.element + " 기운이 생(生)을 더해 능력이 두 배로 강화됩니다.";
            case GENERATED_BY -> " " + season.name + "의 " + season.element + " 기운에 의해 생(生)을 받아 풍요로운 영력을 지닙니다.";
            case DESTROY      -> " " + season.name + "의 " + season.element + " 기운을 극(克)하는 강인한 제압력을 지닙니다.";
            case DESTROYED_BY -> " " + season.name + "의 " + season.element + " 기운에 의해 극(克)받으나, 그 시련이 오히려 불굴의 의지를 낳았습니다.";
        };

        StringBuilder sb = new StringBuilder(stemDesc).append(relDesc).append(" ").append(stage.trait).append(".");

        for (String sal : pillars.shenSha()) {
            String trait = SHEN_SHA_TRAIT.get(sal);
            if (trait != null) {
                sb.append(" 또한 [").append(sal).append("]을 타고나 ").append(trait).append(".");
            }
        }

        return sb.toString();
    }
}
