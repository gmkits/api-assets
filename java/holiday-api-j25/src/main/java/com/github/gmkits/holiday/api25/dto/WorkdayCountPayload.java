package com.github.gmkits.holiday.api25.dto;

import lombok.Builder;
import lombok.Value;

/**
 * 工作日统计结果。
 */
@Value
@Builder
public class WorkdayCountPayload {
    /** 起始日期。 */
    String from;
    /** 结束日期。 */
    String to;
    /** 区间内工作日天数。 */
    int workdays;
    /** 区间内总天数。 */
    int totalDays;
    /** 区间内假日天数（休息日 + 节假日）。 */
    int holidays;
}
