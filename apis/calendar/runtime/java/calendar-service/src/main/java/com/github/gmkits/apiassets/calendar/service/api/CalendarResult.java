package com.github.gmkits.apiassets.calendar.service.api;

import java.time.LocalDate;
import java.util.List;

/** 单日以外的统一日期集合响应。 */
public record CalendarResult(
        String region,
        String locale,
        LocalDate from,
        LocalDate to,
        int count,
        List<DayInfoView> items) {
}
