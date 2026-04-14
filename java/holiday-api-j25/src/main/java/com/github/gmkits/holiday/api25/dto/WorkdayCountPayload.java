package com.github.gmkits.holiday.api25.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 工作日统计结果。
 */
@Getter
@Builder
public class WorkdayCountPayload {
    /** 起始日期。 */
    private final String from;
    /** 结束日期。 */
    private final String to;
    /** 区间内工作日天数。 */
    private final int workdays;
    /** 区间内总天数。 */
    private final int totalDays;
    /** 区间内假日天数（休息日 + 节假日）。 */
    private final int holidays;
}
