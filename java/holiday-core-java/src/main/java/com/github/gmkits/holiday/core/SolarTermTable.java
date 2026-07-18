package com.github.gmkits.holiday.core;

import com.github.gmkits.holiday.lunar.LunarCalendar;
import com.github.gmkits.holiday.spec.SolarTermInfo;

import java.util.Arrays;

/**
 * 节气查询表：O(1) 查找，支持简繁 locale。
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
    private static final byte[][] LOOKUP = buildLookup();

    private SolarTermTable() {}

    static SolarTermInfo lookup(int year, int dayIndex) {
        return lookup(year, dayIndex, "zh-CN");
    }

    static SolarTermInfo lookup(int year, int dayIndex, String locale) {
        if (year < START_YEAR || year > END_YEAR) return null;
        byte[] table = LOOKUP[year - START_YEAR];
        if (dayIndex < 0 || dayIndex >= table.length) return null;
        int idx = table[dayIndex];
        if (idx < 0) return null;
        return "zh-TW".equals(locale) ? INFOS_TW[idx] : INFOS[idx];
    }

    private static SolarTermInfo[] buildInfos(String[] names) {
        SolarTermInfo[] out = new SolarTermInfo[names.length];
        for (int i = 0; i < names.length; i++) out[i] = new SolarTermInfo(i, names[i]);
        return out;
    }

    private static byte[][] buildLookup() {
        int count = END_YEAR - START_YEAR + 1;
        byte[][] tables = new byte[count][];
        for (int y = START_YEAR; y <= END_YEAR; y++) {
            byte[] t = new byte[isLeapYear(y) ? 366 : 365];
            Arrays.fill(t, (byte) -1);
            LunarCalendar.SolarTermInfo[] terms = LunarCalendar.getSolarTerms(y);
            for (int i = 0; i < terms.length; i++) {
                int dayIndex = terms[i].getDate().getDayOfYear() - 1;
                if (dayIndex >= 0 && dayIndex < t.length) t[dayIndex] = (byte) i;
            }
            tables[y - START_YEAR] = t;
        }
        return tables;
    }

    private static boolean isLeapYear(int y) {
        return (y % 4 == 0 && y % 100 != 0) || y % 400 == 0;
    }
}
