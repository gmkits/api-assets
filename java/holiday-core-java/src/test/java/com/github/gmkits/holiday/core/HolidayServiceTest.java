package com.github.gmkits.holiday.core;

import com.github.gmkits.holiday.spec.DayInfo;
import com.github.gmkits.holiday.spec.LunarDateInfo;
import com.github.gmkits.holiday.spec.SolarTermInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link HolidayService} using the CN 2025 bundle from
 * the classpath ({@code bundles/CN/2025.hday}).
 */
class HolidayServiceTest {

    private static HolidayService service;

    @BeforeAll
    static void setUp() {
        service = new HolidayServiceBuilder()
                .defaultRegion("CN")
                .enableClasspathFallback(true)
                .build();
    }

    @Test
    void newYearsDay_isHoliday() {
        LocalDate date = LocalDate.of(2025, 1, 1);
        DayInfo info = service.getDayInfo(date);
        assertNotNull(info, "DayInfo for 2025-01-01 should not be null");
        assertTrue(info.isHoliday(), "Jan 1 should be a holiday");
        assertTrue(info.isStatutoryHoliday(), "Jan 1 should be a statutory holiday");
        assertFalse(info.isWorkday(), "Jan 1 should not be a workday");
    }

    @Test
    void newYearsDay_hasNames() {
        DayInfo info = service.getDayInfo(LocalDate.of(2025, 1, 1));
        assertNotNull(info);
        assertFalse(info.getHolidayNames().isEmpty(), "Jan 1 should have holiday names");
        assertNotNull(info.getHolidayNames().get("en-US"), "Should have en-US name");
    }

    @Test
    void newYearsDay_hasLabels() {
        DayInfo info = service.getDayInfo(LocalDate.of(2025, 1, 1));
        assertNotNull(info);
        assertFalse(info.getLabels().isEmpty(), "Jan 1 should have labels");
        assertTrue(info.getLabels().contains("NEW_YEAR"), "Labels should include NEW_YEAR");
    }

    @Test
    void newYearsDay_hasLunarExtension() {
        DayInfo info = service.getDayInfo(LocalDate.of(2025, 1, 1));
        assertNotNull(info);
        Object lunar = info.getExtensions().get("lunar");
        assertTrue(lunar instanceof LunarDateInfo, "Jan 1 should expose lunar extension");

        LunarDateInfo lunarInfo = (LunarDateInfo) lunar;
        assertEquals(2024, lunarInfo.getYear());
        assertEquals(12, lunarInfo.getMonth());
        assertEquals(2, lunarInfo.getDay());
        assertEquals("甲辰年", lunarInfo.getGanZhiYear());
        assertEquals("龙", lunarInfo.getShengXiao());
        assertEquals("腊月", lunarInfo.getMonthName());
        assertEquals("初二", lunarInfo.getDayName());
    }

    @Test
    void liChun_hasSolarTermExtension() {
        DayInfo info = service.getDayInfo(LocalDate.of(2025, 2, 3));
        assertNotNull(info);
        Object solarTerm = info.getExtensions().get("solarTerm");
        assertTrue(solarTerm instanceof SolarTermInfo, "Feb 3 should expose solarTerm extension");

        SolarTermInfo solarTermInfo = (SolarTermInfo) solarTerm;
        assertEquals(2, solarTermInfo.getIndex());
        assertEquals("立春", solarTermInfo.getName());
    }

    @Test
    void nonSolarTermDay_omitsSolarTermExtension() {
        DayInfo info = service.getDayInfo(LocalDate.of(2025, 2, 4));
        assertNotNull(info);
        assertFalse(info.getExtensions().containsKey("solarTerm"));
    }

    @Test
    void normalWorkday() {
        LocalDate date = LocalDate.of(2025, 1, 2); // Thursday
        DayInfo info = service.getDayInfo(date);
        assertNotNull(info);
        assertTrue(info.isWorkday(), "Jan 2 should be a workday");
        assertFalse(info.isHoliday(), "Jan 2 should not be a holiday");
        assertFalse(info.isWeekend(), "Jan 2 should not be a weekend");
    }

    @Test
    void saturday_isWeekend() {
        LocalDate date = LocalDate.of(2025, 1, 4); // Saturday
        DayInfo info = service.getDayInfo(date);
        assertNotNull(info);
        assertTrue(info.isWeekend(), "Jan 4 (Saturday) should be a weekend");
        assertFalse(info.isWorkday(), "Jan 4 should not be a workday");
    }

    @Test
    void adjustedWorkday_springFestivalMakeup() {
        // Jan 26 (Sunday) is a makeup workday for Spring Festival 2025
        LocalDate date = LocalDate.of(2025, 1, 26);
        DayInfo info = service.getDayInfo(date);
        assertNotNull(info);
        assertTrue(info.isAdjustedWorkday(), "Jan 26 should be an adjusted workday");
        assertTrue(info.isWorkday(), "Jan 26 should be a workday");
        assertTrue(info.isWeekend(), "Jan 26 is still a calendar weekend day");
        assertFalse(info.isHoliday(), "Jan 26 should not be a holiday");
    }

    @Test
    void springFestival_isHoliday() {
        // Jan 28 is the start of the Spring Festival holiday 2025
        LocalDate date = LocalDate.of(2025, 1, 28);
        DayInfo info = service.getDayInfo(date);
        assertNotNull(info);
        assertTrue(info.isHoliday(), "Jan 28 should be a holiday (Spring Festival)");
    }

    @Test
    void isHoliday_convenience() {
        assertTrue(service.isHoliday(LocalDate.of(2025, 1, 1)));
        assertFalse(service.isHoliday(LocalDate.of(2025, 1, 2)));
    }

    @Test
    void isWorkday_convenience() {
        assertTrue(service.isWorkday(LocalDate.of(2025, 1, 2)));
        assertFalse(service.isWorkday(LocalDate.of(2025, 1, 1)));
    }

    @Test
    void getRange_returnsCorrectCount() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 7);
        List<DayInfo> range = service.getRange(from, to);
        assertEquals(7, range.size(), "Range should have 7 days");
    }

    @Test
    void getYear_returns365Days() {
        List<DayInfo> year = service.getYear(2025);
        assertEquals(365, year.size(), "2025 has 365 days");
        assertThrows(UnsupportedOperationException.class, () -> year.remove(0));
    }

    @Test
    void regionOverride() {
        DayInfo info = service.getDayInfo("CN", LocalDate.of(2025, 1, 1));
        assertNotNull(info);
        assertEquals("CN", info.getRegionCode());
    }

    @Test
    void official2026Schedule_matchesGovernmentNotice() {
        assertTrue(service.isHoliday(LocalDate.of(2026, 2, 23)));
        assertAdjustedWorkday(LocalDate.of(2026, 2, 28));
        assertAdjustedWorkday(LocalDate.of(2026, 5, 9));
        assertAdjustedWorkday(LocalDate.of(2026, 9, 20));
        assertStatutoryHoliday(LocalDate.of(2026, 2, 19));
        assertStatutoryHoliday(LocalDate.of(2026, 5, 2));

        assertFalse(service.isAdjustedWorkday(LocalDate.of(2026, 2, 22)));
        assertFalse(service.isAdjustedWorkday(LocalDate.of(2026, 4, 26)));
        assertFalse(service.isAdjustedWorkday(LocalDate.of(2026, 9, 28)));
    }

    @Test
    void revisedStatutorySchedule2025_includesAddedDays() {
        assertStatutoryHoliday(LocalDate.of(2025, 1, 31));
        assertStatutoryHoliday(LocalDate.of(2025, 5, 2));
    }

    private static void assertAdjustedWorkday(LocalDate date) {
        DayInfo info = service.getDayInfo(date);
        assertNotNull(info);
        assertTrue(info.isAdjustedWorkday(), date + " should be an adjusted workday");
        assertTrue(info.isWorkday(), date + " should be a workday");
        assertFalse(info.isHoliday(), date + " should not be a holiday");
    }

    private static void assertStatutoryHoliday(LocalDate date) {
        DayInfo info = service.getDayInfo(date);
        assertNotNull(info);
        assertTrue(info.isStatutoryHoliday(), date + " should be a statutory holiday");
        assertTrue(info.isHoliday(), date + " should be a holiday");
        assertFalse(info.isWorkday(), date + " should not be a workday");
    }
}
