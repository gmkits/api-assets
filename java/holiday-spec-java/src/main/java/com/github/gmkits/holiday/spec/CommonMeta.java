package com.github.gmkits.holiday.spec;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 描述节假日数据包通用元数据的不可变对象。
 *
 * <p>所有字段均遵循 cn-holiday-kit 规范，构造完成后不可变。</p>
 */
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
        this.locales = locales == null ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<>(locales));
        this.sourceVersion = sourceVersion;
        this.generatedAt = generatedAt;
        this.extensions = extensions == null ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(extensions));
    }

    /**
     * 返回数据遵循的规范版本。
     *
     * @return 数据遵循的规范版本
     */
    public String getSpecVersion() { return specVersion; }

    /**
     * 返回数据包唯一标识。
     *
     * @return 数据包唯一标识
     */
    public String getBundleId() { return bundleId; }

    /**
     * 返回数据包所属的主区域代码。
     *
     * @return 数据包所属的主区域代码
     */
    public RegionCode getRegionCode() { return regionCode; }

    /**
     * 返回上级区域代码。
     *
     * @return 上级区域代码；没有上级区域时返回 {@code null}
     */
    public RegionCode getParentRegionCode() { return parentRegionCode; }

    /**
     * 返回数据包所属公历年份。
     *
     * @return 数据包所属公历年份
     */
    public int getYear() { return year; }

    /**
     * 返回数据有效区间的起始日期。
     *
     * @return 数据有效区间的起始日期
     */
    public LocalDate getValidFrom() { return validFrom; }

    /**
     * 返回数据有效区间的结束日期。
     *
     * @return 数据有效区间的结束日期
     */
    public LocalDate getValidTo() { return validTo; }

    /**
     * 返回数据包采用的历法体系。
     *
     * @return 数据包采用的历法体系
     */
    public CalendarSystem getCalendarSystem() { return calendarSystem; }

    /**
     * 返回 IANA 时区标识。
     *
     * @return IANA 时区标识
     */
    public String getTimezone() { return timezone; }

    /**
     * 返回表示周末星期的位掩码。
     *
     * @return 表示周末星期的位掩码
     */
    public int getWeekendMask() { return weekendMask; }

    /**
     * 返回受支持的语言区域标签。
     *
     * @return 不可变的受支持语言区域标签列表
     */
    public List<String> getLocales() { return locales; }

    /**
     * 返回上游数据源版本。
     *
     * @return 上游数据源版本
     */
    public String getSourceVersion() { return sourceVersion; }

    /**
     * 返回数据包生成时间。
     *
     * @return ISO-8601 格式的数据包生成时间
     */
    public String getGeneratedAt() { return generatedAt; }

    /**
     * 返回扩展字段。
     *
     * @return 不可变扩展字段映射
     */
    public Map<String, Object> getExtensions() { return extensions; }

    /**
     * 返回便于日志查看的元数据摘要。
     *
     * @return 包含 bundle、区域和年份的字符串
     */
    @Override
    public String toString() {
        return "CommonMeta{bundleId='" + bundleId + "', regionCode=" + regionCode
                + ", year=" + year + ", sourceVersion='" + sourceVersion + "'}";
    }
}
