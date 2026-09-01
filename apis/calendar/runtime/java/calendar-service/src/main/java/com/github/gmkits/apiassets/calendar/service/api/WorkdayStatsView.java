package com.github.gmkits.apiassets.calendar.service.api;

import com.github.gmkits.apiassets.calendar.core.WorkdayStats;

import java.time.LocalDate;

/** HTTP 工作日统计视图。 */
public record WorkdayStatsView(
        LocalDate from,
        LocalDate to,
        String region,
        int calendarDays,
        int workdays,
        int nonWorkdays,
        int weekendDays,
        int statutoryHolidayDays,
        int adjustedWorkdays) {
    public static WorkdayStatsView of(WorkdayStats stats) {
        return new WorkdayStatsView(stats.from(), stats.to(), stats.regionCode(),
                stats.calendarDays(), stats.workdays(), stats.nonWorkdays(), stats.weekendDays(),
                stats.statutoryHolidayDays(), stats.adjustedWorkdays());
    }
}
