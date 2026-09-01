package com.github.gmkits.apiassets.calendar.core;

import java.time.LocalDate;

/**
 * 闭区间工作日统计结果。
 *
 * <p>统计值由已缓存的年度位图直接计算，不会创建日期结果列表。</p>
 */
public record WorkdayStats(
        LocalDate from,
        LocalDate to,
        String regionCode,
        int calendarDays,
        int workdays,
        int nonWorkdays,
        int weekendDays,
        int statutoryHolidayDays,
        int adjustedWorkdays) {
}
