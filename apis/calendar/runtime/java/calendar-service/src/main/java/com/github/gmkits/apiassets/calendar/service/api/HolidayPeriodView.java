package com.github.gmkits.apiassets.calendar.service.api;

import com.github.gmkits.apiassets.calendar.core.HolidayPeriod;

import java.time.LocalDate;
import java.util.List;

/** HTTP 节假日周期视图。 */
public record HolidayPeriodView(
        String code,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        List<LocalDate> daysOff,
        List<LocalDate> adjustedWorkdays,
        List<LocalDate> statutoryDates,
        String sourceVersion) {
    public static HolidayPeriodView of(HolidayPeriod period, String locale) {
        List<String> localized = period.names().get(locale);
        if ((localized == null || localized.isEmpty()) && "en-US".equals(locale)) {
            localized = period.names().get("zh-CN");
        }
        String name = localized == null || localized.isEmpty()
                ? period.code() : localized.get(0);
        return new HolidayPeriodView(period.code(), name, period.startDate(), period.endDate(),
                period.daysOff(), period.adjustedWorkdays(), period.statutoryDates(),
                period.sourceVersion());
    }
}
