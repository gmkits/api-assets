package com.github.gmkits.holiday.spec;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * 节气信息。
 *
 * <p>用于 {@link DayInfo#getExtensions()} 中的 {@code "solarTerm"} 字段，
 * 提供命中日期对应的稳定索引和中文名。</p>
 */
@Getter
@AllArgsConstructor
@ToString
public final class SolarTermInfo {

    private final int index;
    private final String name;
}
