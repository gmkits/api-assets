package com.github.gmkits.apiassets.calendar.core;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 按稳定节日标签聚合的一年度节假日周期。
 *
 * <p>名称仍保留多语言原始值，HTTP 层根据请求 locale 选择展示名称。</p>
 */
public record HolidayPeriod(
        String code,
        Map<String, List<String>> names,
        LocalDate startDate,
        LocalDate endDate,
        List<LocalDate> daysOff,
        List<LocalDate> adjustedWorkdays,
        List<LocalDate> statutoryDates,
        String sourceVersion) {

    public HolidayPeriod {
        Map<String, List<String>> copiedNames = new LinkedHashMap<>();
        if (names != null) {
            names.forEach((locale, values) -> copiedNames.put(locale, immutable(values)));
        }
        names = Collections.unmodifiableMap(copiedNames);
        daysOff = immutable(daysOff);
        adjustedWorkdays = immutable(adjustedWorkdays);
        statutoryDates = immutable(statutoryDates);
    }

    private static <T> List<T> immutable(List<T> values) {
        return values == null || values.isEmpty()
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(values));
    }
}
