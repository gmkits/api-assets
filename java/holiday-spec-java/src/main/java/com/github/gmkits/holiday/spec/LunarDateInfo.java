package com.github.gmkits.holiday.spec;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * 农历日期信息。
 *
 * <p>用于 {@link DayInfo#getExtensions()} 中的 {@code "lunar"} 字段，
 * 提供公历日期对应的农历信息。</p>
 */
@Getter
@AllArgsConstructor
@ToString
public final class LunarDateInfo {

    private final int year;
    private final int month;
    private final int day;
    private final boolean leapMonth;
    private final String ganZhiYear;
    private final String shengXiao;
    private final String monthName;
    private final String dayName;
}
