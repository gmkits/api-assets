package com.github.gmkits.holiday.spec;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 描述节假日数据包通用元数据的不可变对象。
 *
 * <p>所有字段均遵循 cn-holiday-kit 规范，构造完成后不可变。</p>
 */
@Getter
@ToString
public final class CommonMeta {

    private final String specVersion;
    private final String bundleId;
    private final RegionCode regionCode;
    private final RegionCode parentRegionCode;
    private final int year;
    private final LocalDate validFrom;
    private final LocalDate validTo;
    private final CalendarSystem calendarSystem;
    private final String timezone;
    private final int weekendMask;
    private final List<String> locales;
    private final String sourceVersion;
    private final String generatedAt;
    private final Map<String, Object> extensions;

    /**
     * 创建新的 {@code CommonMeta} 实例。
     *
     * @param specVersion      规范版本（如 {@code "1.0"}）
     * @param bundleId         数据包唯一标识
     * @param regionCode       主区域代码
     * @param parentRegionCode 上级区域代码；如无则为 {@code null}
     * @param year             数据所属年份
     * @param validFrom        有效日期区间起点
     * @param validTo          有效日期区间终点
     * @param calendarSystem   使用的历法体系
     * @param timezone         IANA 时区标识
     * @param weekendMask      表示周末日的位掩码
     * @param locales          支持的语言区域标签
     * @param sourceVersion    源数据版本
     * @param generatedAt      ISO-8601 生成时间戳
     * @param extensions       扩展键值对
     */
    public CommonMeta(String specVersion, String bundleId, RegionCode regionCode,
                      RegionCode parentRegionCode, int year, LocalDate validFrom,
                      LocalDate validTo, CalendarSystem calendarSystem, String timezone,
                      int weekendMask, List<String> locales, String sourceVersion,
                      String generatedAt, Map<String, Object> extensions) {
        this.specVersion = specVersion;
        this.bundleId = bundleId;
        this.regionCode = regionCode;
        this.parentRegionCode = parentRegionCode;
        this.year = year;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.calendarSystem = calendarSystem;
        this.timezone = timezone;
        this.weekendMask = weekendMask;
        this.locales = locales == null ? ImmutableList.of()
                : ImmutableList.copyOf(locales);
        this.sourceVersion = sourceVersion;
        this.generatedAt = generatedAt;
        this.extensions = extensions == null ? ImmutableMap.of()
                : ImmutableMap.copyOf(extensions);
    }
}
