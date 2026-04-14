package com.github.gmkits.holiday.lunar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 农历模块单元测试。
 */
class LunarCalendarTest {

    @Test
    void yearRange() {
        assertEquals(1900, LunarCalendar.START_YEAR);
        assertEquals(2100, LunarCalendar.END_YEAR);
    }

    @Test
    void everyYearDaysBetween353And385() {
        for (int y = LunarCalendar.START_YEAR; y <= LunarCalendar.END_YEAR; y++) {
            int days = LunarCalendar.yearDays(y);
            assertTrue(days >= 353 && days <= 385,
                    y + " 年天数 " + days + " 超出合理范围 [353, 385]");
        }
    }

    @Test
    void everyMonthDays29Or30() {
        for (int y = LunarCalendar.START_YEAR; y <= LunarCalendar.END_YEAR; y++) {
            for (int m = 1; m <= 12; m++) {
                int days = LunarCalendar.monthDays(y, m);
                assertTrue(days == 29 || days == 30,
                        y + "-" + m + " 天数 " + days + " 不是 29 或 30");
            }
        }
    }

    @Test
    void leapMonthRange() {
        for (int y = LunarCalendar.START_YEAR; y <= LunarCalendar.END_YEAR; y++) {
            int lm = LunarCalendar.leapMonth(y);
            assertTrue(lm >= 0 && lm <= 12, y + " 年闰月 " + lm + " 超出范围");
        }
    }

    @Test
    void noLeapMonthMeansZeroDays() {
        for (int y = LunarCalendar.START_YEAR; y <= LunarCalendar.END_YEAR; y++) {
            if (LunarCalendar.leapMonth(y) == 0) {
                assertEquals(0, LunarCalendar.leapMonthDays(y));
            }
        }
    }

    @Test
    void knownLeapMonths() {
        assertEquals(6, LunarCalendar.leapMonth(2025)); // 2025 闰六月
        assertEquals(2, LunarCalendar.leapMonth(2023)); // 2023 闰二月
        assertEquals(0, LunarCalendar.leapMonth(2024)); // 2024 无闰月
    }

    @ParameterizedTest
    @CsvSource({
        "2025, 1, 29, 2025, 1, 1, false",   // 乙巳年正月初一
        "2024, 2, 10, 2024, 1, 1, false",   // 甲辰年正月初一
        "2023, 1, 22, 2023, 1, 1, false",   // 癸卯年正月初一
        "1900, 1, 31, 1900, 1, 1, false",   // 基准日
    })
    void solarToLunarKnownDates(int sy, int sm, int sd, int ly, int lm, int ld, boolean leap) {
        LunarInfo info = LunarCalendar.solarToLunar(LocalDate.of(sy, sm, sd));
        assertEquals(ly, info.getDate().getYear());
        assertEquals(lm, info.getDate().getMonth());
        assertEquals(ld, info.getDate().getDay());
        assertEquals(leap, info.getDate().isLeapMonth());
    }

    @Test
    void springFestival2025Info() {
        LunarInfo info = LunarCalendar.solarToLunar(LocalDate.of(2025, 1, 29));
        assertEquals("乙巳年", info.getGanZhiYear());
        assertEquals("蛇", info.getShengXiao());
        assertEquals("正月", info.getMonthName());
        assertEquals("初一", info.getDayName());
    }

    @Test
    void lunarToSolarRoundTrip() {
        LocalDate result = LunarCalendar.lunarToSolar(2025, 1, 1);
        assertEquals(LocalDate.of(2025, 1, 29), result);

        result = LunarCalendar.lunarToSolar(2024, 1, 1);
        assertEquals(LocalDate.of(2024, 2, 10), result);
    }

    @Test
    void roundTripConsistency2000to2050() {
        for (int year = 2000; year <= 2050; year++) {
            LunarInfo info = LunarCalendar.solarToLunar(LocalDate.of(year, 2, 1));
            LocalDate back = LunarCalendar.lunarToSolar(
                    info.getDate().getYear(), info.getDate().getMonth(),
                    info.getDate().getDay(), info.getDate().isLeapMonth());
            assertEquals(LocalDate.of(year, 2, 1), back, year + " 年往返失败");
        }
    }

    @Test
    void ganZhiCycle() {
        assertEquals("甲子", LunarCalendar.getGanZhi(1984));
        assertEquals("甲子", LunarCalendar.getGanZhi(1984 + 60));
        assertEquals("鼠", LunarCalendar.getShengXiao(1984));
    }

    @Test
    void ganZhi2025() {
        assertEquals("乙", LunarCalendar.getTianGan(2025));
        assertEquals("巳", LunarCalendar.getDiZhi(2025));
        assertEquals("乙巳", LunarCalendar.getGanZhi(2025));
        assertEquals("蛇", LunarCalendar.getShengXiao(2025));
    }

    @Test
    void monthAndDayNames() {
        assertEquals("正月", LunarCalendar.getMonthName(1, false));
        assertEquals("腊月", LunarCalendar.getMonthName(12, false));
        assertEquals("闰四月", LunarCalendar.getMonthName(4, true));
        assertEquals("初一", LunarCalendar.getDayName(1));
        assertEquals("十五", LunarCalendar.getDayName(15));
        assertEquals("三十", LunarCalendar.getDayName(30));
    }

    @Test
    void outOfRangeThrows() {
        assertThrows(IllegalArgumentException.class, () -> LunarCalendar.yearDays(1899));
        assertThrows(IllegalArgumentException.class, () -> LunarCalendar.yearDays(2101));
        assertThrows(IllegalArgumentException.class, () -> LunarCalendar.monthDays(2025, 0));
        assertThrows(IllegalArgumentException.class, () -> LunarCalendar.monthDays(2025, 13));
        assertThrows(IllegalArgumentException.class, () -> LunarCalendar.lunarToSolar(2025, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> LunarCalendar.lunarToSolar(2025, 1, 31));
        assertThrows(IllegalArgumentException.class,
                () -> LunarCalendar.solarToLunar(LocalDate.of(1899, 1, 1)));
    }

    @Test
    void midAutumn2025() {
        // 2025 中秋：农历八月十五
        LunarInfo info = LunarCalendar.solarToLunar(LocalDate.of(2025, 10, 6));
        assertEquals(8, info.getDate().getMonth());
        assertEquals(15, info.getDate().getDay());
    }
}
