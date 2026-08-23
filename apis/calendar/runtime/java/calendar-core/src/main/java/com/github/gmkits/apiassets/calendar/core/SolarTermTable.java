package com.github.gmkits.apiassets.calendar.core;

import com.github.gmkits.apiassets.calendar.lunar.LunarCalendar;
import com.github.gmkits.apiassets.calendar.spec.SolarTermInfo;

import java.time.LocalDate;

/**
 * 节气查询适配器：直接解码年度 48-bit 表，支持简繁 locale。
 */
final class SolarTermTable {

    static final int START_YEAR = 1901;
    static final int END_YEAR = 2100;

    private static final String[] NAMES_ZH_CN = {
            "小寒", "大寒", "立春", "雨水", "惊蛰", "春分",
            "清明", "谷雨", "立夏", "小满", "芒种", "夏至",
            "小暑", "大暑", "立秋", "处暑", "白露", "秋分",
            "寒露", "霜降", "立冬", "小雪", "大雪", "冬至"
    };
    private static final String[] NAMES_ZH_TW = {
            "小寒", "大寒", "立春", "雨水", "驚蟄", "春分",
            "清明", "穀雨", "立夏", "小滿", "芒種", "夏至",
            "小暑", "大暑", "立秋", "處暑", "白露", "秋分",
            "寒露", "霜降", "立冬", "小雪", "大雪", "冬至"
    };
    private static final SolarTermInfo[] INFOS = buildInfos(NAMES_ZH_CN);
    private static final SolarTermInfo[] INFOS_TW = buildInfos(NAMES_ZH_TW);
    private SolarTermTable() {}

    static SolarTermInfo lookup(int year, int dayIndex) {
        return lookup(year, dayIndex, "zh-CN");
    }

    static SolarTermInfo lookup(int year, int dayIndex, String locale) {
        if (year < START_YEAR || year > END_YEAR) return null;
        int dayCount = LocalDate.of(year, 1, 1).lengthOfYear();
        if (dayIndex < 0 || dayIndex >= dayCount) return null;
        LocalDate date = LocalDate.ofYearDay(year, dayIndex + 1);
        String name = LunarCalendar.getSolarTerm(date);
        if (name == null) return null;
        int first = (date.getMonthValue() - 1) * 2;
        int index = NAMES_ZH_CN[first].equals(name) ? first : first + 1;
        return "zh-TW".equals(locale) ? INFOS_TW[index] : INFOS[index];
    }

    /**
     * 一次构建指定年份的节气索引，数组下标就是 day-of-year 减一。
     *
     * <p>年度 bundle 构建会调用该方法一次，避免为每一天重复创建
     * {@link LocalDate} 并重新解码同一年度的 48-bit 节气表。</p>
     */
    static SolarTermInfo[] forYear(int year, String locale) {
        if (year < START_YEAR || year > END_YEAR) return null;
        int dayCount = LocalDate.of(year, 1, 1).lengthOfYear();
        SolarTermInfo[] byDay = new SolarTermInfo[dayCount];
        LunarCalendar.SolarTermInfo[] terms = LunarCalendar.getSolarTerms(year);
        SolarTermInfo[] infos = "zh-TW".equals(locale) ? INFOS_TW : INFOS;
        for (int index = 0; index < terms.length; index++) {
            int dayIndex = terms[index].getDate().getDayOfYear() - 1;
            byDay[dayIndex] = infos[index];
        }
        return byDay;
    }

    static SolarTermInfo[] forYear(int year) {
        return forYear(year, "zh-CN");
    }

    private static SolarTermInfo[] buildInfos(String[] names) {
        SolarTermInfo[] out = new SolarTermInfo[names.length];
        for (int i = 0; i < names.length; i++) out[i] = new SolarTermInfo(i, names[i]);
        return out;
    }

}
