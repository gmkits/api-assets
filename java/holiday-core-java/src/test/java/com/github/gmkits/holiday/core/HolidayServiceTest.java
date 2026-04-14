package com.github.gmkits.holiday.core;

import com.github.gmkits.holiday.spec.DayInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    }

    @Test
    void regionOverride() {
        DayInfo info = service.getDayInfo("CN", LocalDate.of(2025, 1, 1));
        assertNotNull(info);
        assertEquals("CN", info.getRegionCode());
    }
}
