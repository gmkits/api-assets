package com.github.gmkits.holiday.core;

import com.github.gmkits.holiday.spec.SolarTermInfo;

import java.util.Arrays;

/**
 * 节气查询表：O(1) 查找，支持简繁 locale。
 */
final class SolarTermTable {

    static final int START_YEAR = 1901;
    static final int END_YEAR = 2100;

    private static final SolarTermInfo[] INFOS = buildInfos(SolarTermTableData.SOLAR_TERM_NAMES);
    private static final SolarTermInfo[] INFOS_TW = buildInfos(SolarTermTableData.SOLAR_TERM_NAMES_ZH_TW);
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
            int[] di = SolarTermTableData.SOLAR_TERM_DAY_INDEXES_BY_YEAR[y - START_YEAR];
            for (int i = 0; i < di.length; i++) {
                if (di[i] >= 0 && di[i] < t.length) t[di[i]] = (byte) i;
            }
            tables[y - START_YEAR] = t;
        }
        return tables;
    }

    private static boolean isLeapYear(int y) {
        return (y % 4 == 0 && y % 100 != 0) || y % 400 == 0;
    }
}
