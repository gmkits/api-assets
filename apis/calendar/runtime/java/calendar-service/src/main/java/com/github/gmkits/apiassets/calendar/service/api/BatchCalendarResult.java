package com.github.gmkits.apiassets.calendar.service.api;

import java.time.LocalDate;
import java.util.List;

/** 范围批量查询响应，ranges 是合并后的规范化区间。 */
public record BatchCalendarResult(
        String region,
        String locale,
        List<RangeView> ranges,
        int count,
        List<DayInfoView> items) {
    public record RangeView(LocalDate from, LocalDate to) { }
}
