package com.github.gmkits.holiday.lunar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 农历模块测试。
 *
 * - 数据表完整性
 * - 闰月编解码
 * - 公历↔农历互转（已知日期 + CSV 全量）
 * - 闰月互转 / 无效闰月报错 / 小月溢出
 * - 天干地支 / 生肖
 * - 边界与异常
 * - 权威节气范围与越界行为
 */
class LunarCalendarTest {

    // ─── 数据表完整性 ───

    @Test
    void yearRange() {
        assertEquals(1900, LunarCalendar.START_YEAR);
        assertEquals(2100, LunarCalendar.END_YEAR);
    }

    @Test
    void everyYearAndMonthValid() {
        for (int y = LunarCalendar.START_YEAR; y <= LunarCalendar.END_YEAR; y++) {
            int yd = LunarCalendar.yearDays(y);
            assertTrue(yd >= 353 && yd <= 385, y + "年 " + yd + "天");
            for (int m = 1; m <= 12; m++) {
                int md = LunarCalendar.monthDays(y, m);
                assertTrue(md == 29 || md == 30, y + "-" + m + " " + md + "天");
            }
        }
    }

    // ─── 闰月 ───

    @Test
    void leapMonthConsistency() {
        for (int y = LunarCalendar.START_YEAR; y <= LunarCalendar.END_YEAR; y++) {
            int lm = LunarCalendar.leapMonth(y);
            assertTrue(lm >= 0 && lm <= 12, y + "年闰月=" + lm);
            int ld = LunarCalendar.leapMonthDays(y);
            if (lm == 0) {
                assertEquals(0, ld, y + "年无闰月但天数=" + ld);
            } else {
                assertTrue(ld == 29 || ld == 30, y + "年闰月天数=" + ld);
            }
        }
    }

    @Test
    void knownLeapMonths() {
        assertEquals(6, LunarCalendar.leapMonth(2025));
        assertEquals(2, LunarCalendar.leapMonth(2023));
        assertEquals(0, LunarCalendar.leapMonth(2024));
    }

    // ─── 闰月互转 ───

    @Test
    void leapMonthConversion() {
        // 2025 闰六月：闰月和非闰月转到不同公历日
        LocalDate leapM6 = LunarCalendar.lunarToSolar(2025, 6, 1, true);
        LocalDate normalM6 = LunarCalendar.lunarToSolar(2025, 6, 1, false);
        assertNotEquals(leapM6, normalM6);

        // 反查
        LunarInfo backLeap = LunarCalendar.solarToLunar(leapM6);
        assertEquals(6, backLeap.getDate().getMonth());
        assertTrue(backLeap.getDate().isLeapMonth());

        LunarInfo backNormal = LunarCalendar.solarToLunar(normalM6);
        assertEquals(6, backNormal.getDate().getMonth());
        assertFalse(backNormal.getDate().isLeapMonth());
    }

    @Test
    void leapMonth2023RoundTrip() {
        LocalDate solar = LunarCalendar.lunarToSolar(2023, 2, 15, true);
        LunarInfo back = LunarCalendar.solarToLunar(solar);
        assertEquals(2, back.getDate().getMonth());
        assertEquals(15, back.getDate().getDay());
        assertTrue(back.getDate().isLeapMonth());
    }

    // ─── 错误处理 ───

    @Test
    void noLeapMonthWithLeapFlagThrows() {
        // 2024 无闰月
        assertThrows(IllegalArgumentException.class, () -> LunarCalendar.lunarToSolar(2024, 6, 1, true));
        assertThrows(IllegalArgumentException.class, () -> LunarCalendar.lunarToSolar(2024, 1, 1, true));
    }

    @Test
    void wrongLeapMonthThrows() {
        // 2025 闰六月，传闰三月应报错
        assertThrows(IllegalArgumentException.class, () -> LunarCalendar.lunarToSolar(2025, 3, 1, true));
    }

    @Test
    void smallMonthDay30Throws() {
        // 找到一个 29 天月
        for (int m = 1; m <= 12; m++) {
            if (LunarCalendar.monthDays(2025, m) == 29) {
                final int month = m;
                assertThrows(IllegalArgumentException.class, () -> LunarCalendar.lunarToSolar(2025, month, 30));
                return;
            }
        }
        fail("未找到 29 天月");
    }

    @Test
    void outOfRangeThrows() {
        assertThrows(IllegalArgumentException.class, () -> LunarCalendar.yearDays(1899));
        assertThrows(IllegalArgumentException.class, () -> LunarCalendar.yearDays(2101));
        assertThrows(IllegalArgumentException.class, () -> LunarCalendar.solarToLunar(LocalDate.of(1899, 1, 1)));
        assertThrows(IllegalArgumentException.class, () -> LunarCalendar.lunarToSolar(1899, 1, 1));
    }

    @Test
    void invalidMonthOrDayThrows() {
        assertThrows(IllegalArgumentException.class, () -> LunarCalendar.monthDays(2025, 0));
        assertThrows(IllegalArgumentException.class, () -> LunarCalendar.monthDays(2025, 13));
        assertThrows(IllegalArgumentException.class, () -> LunarCalendar.lunarToSolar(2025, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> LunarCalendar.lunarToSolar(2025, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> LunarCalendar.lunarToSolar(2025, 1, 31));
    }

    // ─── 已知日期 ───

    @ParameterizedTest
    @CsvSource({
            "2025,1,29, 2025,1,1,false, 乙巳年,蛇",
            "2024,2,10, 2024,1,1,false, 甲辰年,龙",
            "2023,1,22, 2023,1,1,false, 癸卯年,兔",
            "1900,1,31, 1900,1,1,false, 庚子年,鼠",
            "2025,10,6, 2025,8,15,false, 乙巳年,蛇",
    })
    void knownDates(int sy, int sm, int sd, int ly, int lm, int ld, boolean leap,
                    String ganZhi, String shengXiao) {
        LunarInfo info = LunarCalendar.solarToLunar(LocalDate.of(sy, sm, sd));
        assertEquals(ly, info.getDate().getYear());
        assertEquals(lm, info.getDate().getMonth());
        assertEquals(ld, info.getDate().getDay());
        assertEquals(leap, info.getDate().isLeapMonth());
        assertEquals(ganZhi, info.getGanZhiYear());
        assertEquals(shengXiao, info.getShengXiao());
    }

    // ─── 边界日期 ───

    @Test
    void baseDate() {
        LunarInfo info = LunarCalendar.solarToLunar(LocalDate.of(1900, 1, 31));
        assertEquals(1900, info.getDate().getYear());
        assertEquals(1, info.getDate().getMonth());
        assertEquals(1, info.getDate().getDay());
    }

    @Test
    void nearEndOf2100() {
        // 能转换到 2100 年末附近
        LunarInfo info = LunarCalendar.solarToLunar(LocalDate.of(2101, 1, 28));
        assertEquals(2100, info.getDate().getYear());
    }

    // ─── CSV 全量验证 ───

    @ParameterizedTest
    @CsvFileSource(resources = "/lunar-golden.csv", numLinesToSkip = 1)
    void csvSolarToLunar(String solarDate, int ly, int lm, int ld, int isLeapMonth) {
        String[] parts = solarDate.split("-");
        int sy = Integer.parseInt(parts[0]);
        int sm = Integer.parseInt(parts[1]);
        int sd = Integer.parseInt(parts[2]);
        boolean leap = isLeapMonth == 1;
        LunarInfo info = LunarCalendar.solarToLunar(LocalDate.of(sy, sm, sd));
        assertEquals(ly, info.getDate().getYear(), sy + "-" + sm + "-" + sd + " year");
        assertEquals(lm, info.getDate().getMonth(), sy + "-" + sm + "-" + sd + " month");
        assertEquals(ld, info.getDate().getDay(), sy + "-" + sm + "-" + sd + " day");
        assertEquals(leap, info.getDate().isLeapMonth(), sy + "-" + sm + "-" + sd + " leap");
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/lunar-golden.csv", numLinesToSkip = 1)
    void csvLunarToSolar(String solarDate, int ly, int lm, int ld, int isLeapMonth) {
        String[] parts = solarDate.split("-");
        int sy = Integer.parseInt(parts[0]);
        int sm = Integer.parseInt(parts[1]);
        int sd = Integer.parseInt(parts[2]);
        boolean leap = isLeapMonth == 1;
        LocalDate result = LunarCalendar.lunarToSolar(ly, lm, ld, leap);
        assertEquals(LocalDate.of(sy, sm, sd), result,
                "lunar(" + ly + "," + lm + "," + ld + "," + leap + ")");
    }

    // ─── 天干地支 ───

    @Test
    void ganZhiCycle() {
        assertEquals("甲子", LunarCalendar.getGanZhi(1984));
        assertEquals("鼠", LunarCalendar.getShengXiao(1984));
        assertEquals("甲子", LunarCalendar.getGanZhi(2044));
        assertEquals("甲子", LunarCalendar.getGanZhi(2104));
    }

    @Test
    void ganZhi2025() {
        assertEquals("乙", LunarCalendar.getTianGan(2025));
        assertEquals("巳", LunarCalendar.getDiZhi(2025));
        assertEquals("乙巳", LunarCalendar.getGanZhi(2025));
        assertEquals("蛇", LunarCalendar.getShengXiao(2025));
    }

    // ─── 名称 ───

    @Test
    void names() {
        assertEquals("正月", LunarCalendar.getMonthName(1, false));
        assertEquals("腊月", LunarCalendar.getMonthName(12, false));
        assertEquals("闰四月", LunarCalendar.getMonthName(4, true));
        assertEquals("初一", LunarCalendar.getDayName(1));
        assertEquals("十五", LunarCalendar.getDayName(15));
        assertEquals("三十", LunarCalendar.getDayName(30));
    }

    // ─── 二十四节气 ───

    @Test
    void solarTermNamesLength() {
        assertEquals(24, LunarCalendar.SOLAR_TERM_NAMES.length);
    }

    @Test
    void getSolarTermsReturns24() {
        LunarCalendar.SolarTermInfo[] terms = LunarCalendar.getSolarTerms(2025);
        assertEquals(24, terms.length);
        for (LunarCalendar.SolarTermInfo term : terms) {
            assertNotNull(term.getName(), "节气应有名称");
            assertEquals(2025, term.getDate().getYear(), term.getName() + " 年份应为 2025");
            assertTrue(term.getDate().getMonthValue() >= 1 && term.getDate().getMonthValue() <= 12);
            assertTrue(term.getDate().getDayOfMonth() >= 1 && term.getDate().getDayOfMonth() <= 31);
        }
    }

    @Test
    void solarTerms2025KnownDates() {
        // 2025 年已知节气（来源：香港天文台 / 紫金山天文台数据，精确匹配）
        int[][] known = {
            // {黄经索引, 期望月, 期望日}
            {0, 1, 5},   // 小寒
            {1, 1, 20},  // 大寒
            {2, 2, 3},   // 立春
            {5, 3, 20},  // 春分
            {11, 6, 21}, // 夏至
            {17, 9, 23}, // 秋分
            {23, 12, 21},// 冬至
        };
        LunarCalendar.SolarTermInfo[] terms = LunarCalendar.getSolarTerms(2025);
        for (int[] row : known) {
            LunarCalendar.SolarTermInfo term = terms[row[0]];
            assertEquals(row[1], term.getDate().getMonthValue(),
                term.getName() + " 月份不匹配");
            assertEquals(row[2], term.getDate().getDayOfMonth(),
                term.getName() + " 日期不匹配（期望 " + row[1] + "-" + row[2]
                    + "，实际 " + term.getDate().getMonthValue() + "-" + term.getDate().getDayOfMonth() + "）");
        }
    }

    @Test
    void getSolarTermOnTermDay() {
        LunarCalendar.SolarTermInfo[] terms = LunarCalendar.getSolarTerms(2025);
        LunarCalendar.SolarTermInfo firstTerm = terms[0];
        String result = LunarCalendar.getSolarTerm(firstTerm.getDate());
        assertEquals(firstTerm.getName(), result);
    }

    @Test
    void getSolarTermOnNonTermDay() {
        // 2025-01-01 通常不是节气日
        String result = LunarCalendar.getSolarTerm(LocalDate.of(2025, 1, 1));
        assertNull(result);
    }

    @Test
    void solarTermsRejectEstimatedYears() {
        assertThrows(IllegalArgumentException.class,
                () -> LunarCalendar.getSolarTerms(1900));
        assertThrows(IllegalArgumentException.class,
                () -> LunarCalendar.getSolarTerms(2101));
        assertThrows(IllegalArgumentException.class,
                () -> LunarCalendar.getSolarTerm(LocalDate.of(1900, 1, 6)));
    }

    @Test
    void solarTermsChronologicalOrder() {
        LunarCalendar.SolarTermInfo[] terms = LunarCalendar.getSolarTerms(2025);
        for (int i = 1; i < terms.length; i++) {
            assertTrue(!terms[i].getDate().isBefore(terms[i - 1].getDate()),
                "节气顺序错误: " + terms[i - 1].getName() + " 应早于 " + terms[i].getName());
        }
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/solar-terms.csv", numLinesToSkip = 1)
    void solarTermsGoldenCsv(String solarDate, int termIndex, String sourceName) {
        LocalDate expected = LocalDate.parse(solarDate);
        int year = expected.getYear();
        LunarCalendar.SolarTermInfo[] terms = LunarCalendar.getSolarTerms(year);
        LunarCalendar.SolarTermInfo term = terms[termIndex];
        String expectedName = simplifySolarTermName(sourceName);
        assertEquals(expectedName, term.getName(), year + " term " + termIndex + " 名称不匹配");
        assertEquals(expected, term.getDate(), year + " " + expectedName + " 日期不匹配");
    }

    private static String simplifySolarTermName(String name) {
        if ("驚蟄".equals(name)) return "惊蛰";
        if ("穀雨".equals(name)) return "谷雨";
        if ("小滿".equals(name)) return "小满";
        if ("芒種".equals(name)) return "芒种";
        if ("處暑".equals(name)) return "处暑";
        return name;
    }
}
