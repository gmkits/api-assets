package com.github.gmkits.holiday.sdk.j25;

import java.util.List;
import java.util.Map;

/**
 * 单日节假日信息。
 *
 * <p>SDK 内部 DTO，与服务端 {@code DayInfo} 字段对齐；保留 {@code holiday}/{@code workday}
 * 等服务端旧布尔字段，由 {@link HolidayClient} 解析时填充到对应不可变字段。</p>
 *
 * @param date               日期（{@code YYYY-MM-DD}）
 * @param regionCode         地区代码
 * @param calendarSystem     历法（{@code GREGORIAN} / {@code CHINESE_LUNAR}）
 * @param holiday            是否节假日
 * @param workday            是否工作日
 * @param weekend            是否周末
 * @param statutoryHoliday   是否法定节假日
 * @param adjustedWorkday    是否调班工作日
 * @param holidayNames       多语言节假日名称（locale → names）
 * @param labels             标签
 * @param sourceVersion      数据来源版本
 * @param extensions         扩展字段（lunar / solarTerm 等）
 */
public record DayInfo(
        String date,
        String regionCode,
        String calendarSystem,
        boolean holiday,
        boolean workday,
        boolean weekend,
        boolean statutoryHoliday,
        boolean adjustedWorkday,
        Map<String, List<String>> holidayNames,
        List<String> labels,
        String sourceVersion,
        Map<String, Object> extensions
) {
}
