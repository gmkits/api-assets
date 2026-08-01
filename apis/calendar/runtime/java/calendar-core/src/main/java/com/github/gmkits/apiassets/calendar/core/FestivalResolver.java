package com.github.gmkits.apiassets.calendar.core;

import com.github.gmkits.apiassets.calendar.lunar.LunarCalendar;
import com.github.gmkits.apiassets.calendar.lunar.LunarInfo;
import com.github.gmkits.apiassets.calendar.spec.FestivalInfo;
import com.github.gmkits.apiassets.calendar.spec.LunarDateInfo;
import com.github.gmkits.apiassets.calendar.spec.SolarTermInfo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将公历、农历和节气组合为稳定节日列表。
 */
final class FestivalResolver {

    private static final Map<String, FestivalInfo> FESTIVALS = buildFestivals();

    private FestivalResolver() {
    }

    static List<FestivalInfo> resolve(
            LocalDate date, LunarDateInfo lunar, SolarTermInfo solarTerm) {
        List<FestivalInfo> result = new ArrayList<>(2);
        addFixed(result, date);

        if (solarTerm != null && "清明".equals(solarTerm.getName())) {
            result.add(FESTIVALS.get("TOMB_SWEEPING"));
        }
        if (lunar != null && !lunar.isLeapMonth()) {
            addLunar(result, date, lunar);
        }
        return result.isEmpty()
                ? Collections.<FestivalInfo>emptyList()
                : Collections.unmodifiableList(result);
    }

    private static void addFixed(List<FestivalInfo> result, LocalDate date) {
        int key = date.getMonthValue() * 100 + date.getDayOfMonth();
        switch (key) {
            case 101: result.add(FESTIVALS.get("NEW_YEAR")); break;
            case 308: result.add(FESTIVALS.get("WOMENS_DAY")); break;
            case 312: result.add(FESTIVALS.get("ARBOR_DAY")); break;
            case 501: result.add(FESTIVALS.get("LABOUR_DAY")); break;
            case 504: result.add(FESTIVALS.get("YOUTH_DAY")); break;
            case 601: result.add(FESTIVALS.get("CHILDRENS_DAY")); break;
            case 701: result.add(FESTIVALS.get("CPC_FOUNDING_DAY")); break;
            case 801: result.add(FESTIVALS.get("ARMY_DAY")); break;
            case 903: result.add(FESTIVALS.get("VICTORY_DAY")); break;
            case 910: result.add(FESTIVALS.get("TEACHERS_DAY")); break;
            case 1001: result.add(FESTIVALS.get("NATIONAL_DAY")); break;
            case 1213: result.add(FESTIVALS.get("NATIONAL_MEMORIAL_DAY")); break;
            default: break;
        }
    }

    private static void addLunar(
            List<FestivalInfo> result, LocalDate date, LunarDateInfo lunar) {
        int key = lunar.getMonth() * 100 + lunar.getDay();
        switch (key) {
            case 101: result.add(FESTIVALS.get("SPRING_FESTIVAL")); break;
            case 115: result.add(FESTIVALS.get("LANTERN_FESTIVAL")); break;
            case 202: result.add(FESTIVALS.get("DRAGON_HEADS_RAISING")); break;
            case 505: result.add(FESTIVALS.get("DRAGON_BOAT")); break;
            case 707: result.add(FESTIVALS.get("QIXI")); break;
            case 715: result.add(FESTIVALS.get("GHOST_FESTIVAL")); break;
            case 815: result.add(FESTIVALS.get("MID_AUTUMN")); break;
            case 909: result.add(FESTIVALS.get("DOUBLE_NINTH")); break;
            case 1208: result.add(FESTIVALS.get("LABA")); break;
            default: break;
        }
        if (lunar.getMonth() == 12 && lunar.getDay() >= 29 && isLunarNewYearsEve(date)) {
            result.add(FESTIVALS.get("LUNAR_NEW_YEARS_EVE"));
        }
    }

    private static boolean isLunarNewYearsEve(LocalDate date) {
        try {
            LunarInfo tomorrow = LunarCalendar.solarToLunar(date.plusDays(1));
            return !tomorrow.getDate().isLeapMonth()
                    && tomorrow.getDate().getMonth() == 1
                    && tomorrow.getDate().getDay() == 1;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static Map<String, FestivalInfo> buildFestivals() {
        Map<String, FestivalInfo> result = new LinkedHashMap<>();
        add(result, "NEW_YEAR", "元旦", "New Year's Day");
        add(result, "SPRING_FESTIVAL", "春节", "Spring Festival");
        add(result, "LUNAR_NEW_YEARS_EVE", "除夕", "Lunar New Year's Eve");
        add(result, "LANTERN_FESTIVAL", "元宵节", "Lantern Festival");
        add(result, "DRAGON_HEADS_RAISING", "龙抬头", "Dragon Heads-raising Day");
        add(result, "TOMB_SWEEPING", "清明节", "Tomb-Sweeping Day");
        add(result, "LABOUR_DAY", "劳动节", "Labour Day");
        add(result, "DRAGON_BOAT", "端午节", "Dragon Boat Festival");
        add(result, "QIXI", "七夕节", "Qixi Festival");
        add(result, "GHOST_FESTIVAL", "中元节", "Ghost Festival");
        add(result, "MID_AUTUMN", "中秋节", "Mid-Autumn Festival");
        add(result, "DOUBLE_NINTH", "重阳节", "Double Ninth Festival");
        add(result, "LABA", "腊八节", "Laba Festival");
        add(result, "NATIONAL_DAY", "国庆节", "National Day");
        add(result, "WOMENS_DAY", "妇女节", "International Women's Day");
        add(result, "ARBOR_DAY", "植树节", "Arbor Day");
        add(result, "YOUTH_DAY", "青年节", "Youth Day");
        add(result, "CHILDRENS_DAY", "儿童节", "Children's Day");
        add(result, "CPC_FOUNDING_DAY", "建党节", "CPC Founding Day");
        add(result, "ARMY_DAY", "建军节", "PLA Day");
        add(result, "VICTORY_DAY", "中国人民抗日战争胜利纪念日", "Victory Day");
        add(result, "TEACHERS_DAY", "教师节", "Teachers' Day");
        add(result, "NATIONAL_MEMORIAL_DAY", "南京大屠杀死难者国家公祭日",
                "National Memorial Day");
        return Collections.unmodifiableMap(result);
    }

    private static void add(
            Map<String, FestivalInfo> target, String code, String zh, String en) {
        Map<String, String> names = new LinkedHashMap<>();
        names.put("zh-CN", zh);
        names.put("en-US", en);
        target.put(code, new FestivalInfo(code, names));
    }
}
