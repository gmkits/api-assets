package com.github.gmkits.holiday.core;

import com.github.gmkits.holiday.spec.DayInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 数据规范性测试：验证 2025 和 2026 年 CN 数据的完整性和正确性。
 */
class DataNormTest {

    private static HolidayService service;

    @BeforeAll
    static void setUp() {
        service = new HolidayServiceBuilder()
                .defaultRegion("CN")
                .enableClasspathFallback(true)
                .build();
    }

    // === 2025 年数据规范 ===

    @Test
    void year2025_shouldHave365Days() {
        List<DayInfo> year = service.getYear(2025);
        assertEquals(365, year.size(), "2025 has 365 days");
    }

    @Test
    void year2025_firstAndLastDate() {
        List<DayInfo> year = service.getYear(2025);
        assertEquals(LocalDate.of(2025, 1, 1), year.get(0).getDate());
        assertEquals(LocalDate.of(2025, 12, 31), year.get(364).getDate());
    }

    @Test
    void year2025_allDatesAreRegionCN() {
        List<DayInfo> year = service.getYear(2025);
        for (DayInfo day : year) {
            assertEquals("CN", day.getRegionCode());
        }
    }

    @Test
    void year2025_datesAreContiguous() {
        List<DayInfo> year = service.getYear(2025);
        for (int i = 1; i < year.size(); i++) {
            LocalDate prev = year.get(i - 1).getDate();
            LocalDate curr = year.get(i).getDate();
            assertEquals(prev.plusDays(1), curr, "Dates should be contiguous at index " + i);
        }
    }

    @Test
    void year2025_everyDayHasConsistentFlags() {
        List<DayInfo> year = service.getYear(2025);
        for (DayInfo day : year) {
            // Holiday and adjusted workday should be mutually exclusive
            if (day.isHoliday()) {
                assertFalse(day.isAdjustedWorkday(),
                        day.getDate() + " cannot be both holiday and adjusted workday");
            }
            // Statutory holiday must be a holiday
            if (day.isStatutoryHoliday()) {
                assertTrue(day.isHoliday(),
                        day.getDate() + " statutory holiday must also be holiday");
            }
            // Adjusted workday must be a workday
            if (day.isAdjustedWorkday()) {
                assertTrue(day.isWorkday(),
                        day.getDate() + " adjusted workday must also be workday");
            }
        }
    }

    @Test
    void year2025_statutoryHolidays_haveNames() {
        List<DayInfo> year = service.getYear(2025);
        for (DayInfo day : year) {
            if (day.isStatutoryHoliday()) {
                Map<String, List<String>> names = day.getHolidayNames();
                assertFalse(names.isEmpty(),
                        day.getDate() + " statutory holiday should have names");
            }
        }
    }

    @Test
    void year2025_range_firstWeek() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 7);
        List<DayInfo> range = service.getRange(from, to);
        assertEquals(7, range.size());
        assertEquals(from, range.get(0).getDate());
        assertEquals(to, range.get(6).getDate());
    }

    @Test
    void year2025_knownHolidays() {
        // 元旦
        assertTrue(service.isHoliday(LocalDate.of(2025, 1, 1)));
        // 春节（28-2月4日）
        assertTrue(service.isHoliday(LocalDate.of(2025, 1, 28)));
        assertTrue(service.isHoliday(LocalDate.of(2025, 2, 4)));
        // 清明
        assertTrue(service.isHoliday(LocalDate.of(2025, 4, 4)));
        // 劳动节
        assertTrue(service.isHoliday(LocalDate.of(2025, 5, 1)));
        // 端午
        assertTrue(service.isHoliday(LocalDate.of(2025, 5, 31)));
        // 中秋+国庆（10月1日-8日）
        assertTrue(service.isHoliday(LocalDate.of(2025, 10, 1)));
        assertTrue(service.isHoliday(LocalDate.of(2025, 10, 8)));
    }

    @Test
    void year2025_knownAdjustedWorkdays() {
        // 春节调休
        DayInfo jan26 = service.getDayInfo(LocalDate.of(2025, 1, 26));
        assertNotNull(jan26);
        assertTrue(jan26.isAdjustedWorkday());
        assertTrue(jan26.isWorkday());
    }

    @Test
    void year2025_normalWorkday() {
        // Jan 2 is a Thursday
        DayInfo jan2 = service.getDayInfo(LocalDate.of(2025, 1, 2));
        assertNotNull(jan2);
        assertTrue(jan2.isWorkday());
        assertFalse(jan2.isHoliday());
        assertFalse(jan2.isWeekend());
        assertFalse(jan2.isAdjustedWorkday());
    }

    // === 2026 年数据规范 ===

    @Test
    void year2026_shouldHave365Days() {
        List<DayInfo> year = service.getYear(2026);
        assertEquals(365, year.size(), "2026 has 365 days");
    }

    @Test
    void year2026_newYearIsHoliday() {
        assertTrue(service.isHoliday(LocalDate.of(2026, 1, 1)));
    }

    @Test
    void year2026_springFestival() {
        DayInfo feb17 = service.getDayInfo(LocalDate.of(2026, 2, 17));
        assertNotNull(feb17);
        assertTrue(feb17.isHoliday());
        assertTrue(feb17.getLabels().contains("SPRING_FESTIVAL"));
    }

    @Test
    void year2026_nationalDay() {
        DayInfo oct1 = service.getDayInfo(LocalDate.of(2026, 10, 1));
        assertNotNull(oct1);
        assertTrue(oct1.isHoliday());
        assertTrue(oct1.isStatutoryHoliday());
        assertTrue(oct1.getLabels().contains("NATIONAL_DAY"));
    }

    // === 边界情况 ===

    @Test
    void queryNonExistentYear_shouldReturnNull() {
        assertNull(service.getDayInfo(LocalDate.of(2099, 1, 1)));
    }

    @Test
    void queryInvalidRegion_shouldReturnNull() {
        assertNull(service.getDayInfo("XX", LocalDate.of(2025, 1, 1)));
    }

    @Test
    void getYear_nonExistentYear_shouldReturnEmptyList() {
        List<DayInfo> year = service.getYear(2099);
        assertTrue(year.isEmpty(), "Non-existent year should return empty list");
    }
}
