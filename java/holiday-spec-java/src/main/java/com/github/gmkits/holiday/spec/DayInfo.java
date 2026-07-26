package com.github.gmkits.holiday.spec;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 节假日数据集中单个日历日的信息。
 *
 * <p>所有布尔字段始终存在（绝不会是 {@code null}）。
 * 实例通过 {@link Builder} 构造后保持不可变。</p>
 */
public final class DayInfo {

    private final LocalDate date;
    private final String regionCode;
    private final CalendarSystem calendarSystem;
    private final boolean holiday;
    private final boolean workday;
    private final boolean weekend;
    private final boolean statutoryHoliday;
    private final boolean adjustedWorkday;
    private final Map<String, List<String>> holidayNames;
    private final List<String> labels;
    private final List<FestivalInfo> festivals;
    private final String sourceVersion;
    private final Map<String, Object> extensions;

    private DayInfo(Builder b) {
        this.date = Objects.requireNonNull(b.date, "date");
        this.regionCode = Objects.requireNonNull(b.regionCode, "regionCode");
        this.calendarSystem = b.calendarSystem;
        this.holiday = b.holiday;
        this.workday = b.workday;
        this.weekend = b.weekend;
        this.statutoryHoliday = b.statutoryHoliday;
        this.adjustedWorkday = b.adjustedWorkday;
        this.holidayNames = freezeNames(b.holidayNames);
        this.labels = b.labels == null ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<>(b.labels));
        this.festivals = b.festivals == null ? Collections.<FestivalInfo>emptyList()
                : Collections.unmodifiableList(new ArrayList<>(b.festivals));
        this.sourceVersion = b.sourceVersion;
        this.extensions = b.extensions == null ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(b.extensions));
    }

    private static Map<String, List<String>> freezeNames(Map<String, List<String>> src) {
        if (src == null || src.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<String>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> e : src.entrySet()) {
            copy.put(e.getKey(), Collections.unmodifiableList(new ArrayList<>(e.getValue())));
        }
        return Collections.unmodifiableMap(copy);
    }

    /**
     * 返回当前记录对应的公历日期。
     *
     * @return 当前记录对应的公历日期
     */
    public LocalDate getDate() { return date; }

    /**
     * 返回当前记录所属的区域代码。
     *
     * @return 当前记录所属的区域代码
     */
    public String getRegionCode() { return regionCode; }

    /**
     * 返回当前记录采用的历法体系。
     *
     * @return 当前记录采用的历法体系
     */
    public CalendarSystem getCalendarSystem() { return calendarSystem; }

    /**
     * 判断当天是否为休息日。
     *
     * <p>自然周末和官方放假安排均返回 {@code true}；调休补班日返回
     * {@code false}。如需判断是否属于官方安排，请使用
     * {@link #isOfficialHoliday()}。</p>
     *
     * @return 当天无需工作时返回 {@code true}
     */
    public boolean isHoliday() { return holiday; }

    /**
     * 判断当天是否属于官方公布的放假安排。
     *
     * @return 当天是带有官方节假日名称的休息日时返回 {@code true}
     */
    public boolean isOfficialHoliday() {
        return holiday && !holidayNames.isEmpty();
    }

    /**
     * 判断当天是否需要工作。
     *
     * @return 当天需要工作时返回 {@code true}
     */
    public boolean isWorkday() { return workday; }

    /**
     * 判断当天是否为自然周末。
     *
     * @return 当天是自然周末时返回 {@code true}
     */
    public boolean isWeekend() { return weekend; }

    /**
     * 判断当天是否属于国家法定节假日。
     *
     * @return 当天属于国家法定节假日时返回 {@code true}
     */
    public boolean isStatutoryHoliday() { return statutoryHoliday; }

    /**
     * 判断当天是否为调休补班日。
     *
     * @return 当天是调休补班日时返回 {@code true}
     */
    public boolean isAdjustedWorkday() { return adjustedWorkday; }

    /**
     * 返回按语言区域组织的节日名称。
     *
     * @return 按语言区域组织的不可变节日名称映射
     */
    public Map<String, List<String>> getHolidayNames() { return holidayNames; }

    /**
     * 返回节日标签。
     *
     * @return 不可变节日标签列表
     */
    public List<String> getLabels() { return labels; }

    /**
     * 返回当天命中的传统节日、公共节日和纪念日。
     *
     * <p>此列表不代表当天一定放假，工作状态以 {@link #isWorkday()} 为准。</p>
     *
     * @return 不可变节日列表
     */
    public List<FestivalInfo> getFestivals() { return festivals; }

    /**
     * 返回生成当前记录的上游数据版本。
     *
     * @return 生成当前记录的上游数据版本
     */
    public String getSourceVersion() { return sourceVersion; }

    /**
     * 返回不可变扩展字段映射。
     *
     * <p>当前中国日期资产可能包含键 {@code "lunar"} 和
     * {@code "solarTerm"}，值分别为 {@link LunarDateInfo} 和
     * {@link SolarTermInfo}。</p>
     *
     * @return 不可变扩展字段映射
     */
    public Map<String, Object> getExtensions() { return extensions; }

    /**
     * 返回单日信息的简要调试字符串。
     *
     * @return 单日信息的简要调试字符串
     */
    @Override
    public String toString() {
        return "DayInfo{date=" + date + ", region=" + regionCode
                + ", holiday=" + holiday + ", workday=" + workday + "}";
    }

    /**
     * 用于构造 {@link DayInfo} 实例的构建器。
     */
    public static final class Builder {
        private LocalDate date;
        private String regionCode;
        private CalendarSystem calendarSystem;
        private boolean holiday;
        private boolean workday;
        private boolean weekend;
        private boolean statutoryHoliday;
        private boolean adjustedWorkday;
        private Map<String, List<String>> holidayNames;
        private List<String> labels;
        private List<FestivalInfo> festivals;
        private String sourceVersion;
        private Map<String, Object> extensions;

        /**
         * 创建一个尚未设置字段的构建器。
         */
        public Builder() {
        }

        /**
         * 设置日期。
         *
         * @param date 公历日期，不能为空
         * @return 当前构建器
         */
        public Builder date(LocalDate date) { this.date = date; return this; }

        /**
         * 设置区域代码。
         *
         * @param regionCode 区域代码，例如 {@code CN}
         * @return 当前构建器
         */
        public Builder regionCode(String regionCode) { this.regionCode = regionCode; return this; }

        /**
         * 设置历法体系。
         *
         * @param cs 历法体系
         * @return 当前构建器
         */
        public Builder calendarSystem(CalendarSystem cs) { this.calendarSystem = cs; return this; }

        /**
         * 设置放假标记。
         *
         * @param v 当天放假时为 {@code true}
         * @return 当前构建器
         */
        public Builder holiday(boolean v) { this.holiday = v; return this; }

        /**
         * 设置工作日标记。
         *
         * @param v 当天工作时为 {@code true}
         * @return 当前构建器
         */
        public Builder workday(boolean v) { this.workday = v; return this; }

        /**
         * 设置自然周末标记。
         *
         * @param v 当天为周六或周日时为 {@code true}
         * @return 当前构建器
         */
        public Builder weekend(boolean v) { this.weekend = v; return this; }

        /**
         * 设置法定节假日标记。
         *
         * @param v 当天属于法定节假日时为 {@code true}
         * @return 当前构建器
         */
        public Builder statutoryHoliday(boolean v) { this.statutoryHoliday = v; return this; }

        /**
         * 设置调休补班标记。
         *
         * @param v 当天为调休补班日时为 {@code true}
         * @return 当前构建器
         */
        public Builder adjustedWorkday(boolean v) { this.adjustedWorkday = v; return this; }

        /**
         * 设置按语言区域组织的节日名称。
         *
         * @param names 节日名称映射；构建时执行防御性复制
         * @return 当前构建器
         */
        public Builder holidayNames(Map<String, List<String>> names) { this.holidayNames = names; return this; }

        /**
         * 设置节日标签列表。
         *
         * @param labels 标签列表；构建时执行防御性复制
         * @return 当前构建器
         */
        public Builder labels(List<String> labels) { this.labels = labels; return this; }

        /**
         * 设置当天命中的节日列表。
         *
         * @param festivals 节日列表；构建时执行防御性复制
         * @return 当前构建器
         */
        public Builder festivals(List<FestivalInfo> festivals) {
            this.festivals = festivals;
            return this;
        }

        /**
         * 设置上游数据版本。
         *
         * @param v 数据源版本
         * @return 当前构建器
         */
        public Builder sourceVersion(String v) { this.sourceVersion = v; return this; }

        /**
         * 设置扩展字段映射。
         *
         * @param ext 扩展映射；构建时执行防御性复制
         * @return 当前构建器
         */
        public Builder extensions(Map<String, Object> ext) { this.extensions = ext; return this; }

        /**
         * 构建不可变的 {@link DayInfo} 实例。
         *
         * @return 构造后的实例
         */
        public DayInfo build() {
            return new DayInfo(this);
        }
    }
}
