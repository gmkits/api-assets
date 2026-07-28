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
    private final LunarDateInfo lunar;
    private final SolarTermInfo solarTerm;
    private final GanZhiInfo ganZhi;
    private final List<FestivalInfo> festivals;
    private final String sourceVersion;

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
        this.labels = freezeList(b.labels);
        this.lunar = b.lunar;
        this.solarTerm = b.solarTerm;
        this.ganZhi = b.ganZhi;
        this.festivals = freezeList(b.festivals);
        this.sourceVersion = b.sourceVersion;
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

    private static <T> List<T> freezeList(List<T> source) {
        /*
         * 空标签和空节日是绝大多数日期的常态，统一复用 JDK 的不可变空集合，
         * 避免为每一天保留一个空 ArrayList 和包装视图。非空输入仍执行防御性
         * 复制，因此 DayInfo 的不可变约束不受影响。
         */
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(source));
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
     * 返回当天对应的农历日期。
     *
     * @return 农历日期；超出离线农历资产范围时返回 {@code null}
     */
    public LunarDateInfo getLunar() { return lunar; }

    /**
     * 返回当天命中的二十四节气。
     *
     * @return 节气信息；当天不是节气日时返回 {@code null}
     */
    public SolarTermInfo getSolarTerm() { return solarTerm; }

    /**
     * 返回当天农历年的天干、地支、干支纪年和生肖。
     *
     * @return 干支信息；超出离线农历资产范围时返回 {@code null}
     */
    public GanZhiInfo getGanZhi() { return ganZhi; }

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
        private LunarDateInfo lunar;
        private SolarTermInfo solarTerm;
        private GanZhiInfo ganZhi;
        private List<FestivalInfo> festivals;
        private String sourceVersion;

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
         * 设置当天对应的农历日期。
         *
         * @param value 农历日期；无数据时为 {@code null}
         * @return 当前构建器
         */
        public Builder lunar(LunarDateInfo value) { this.lunar = value; return this; }

        /**
         * 设置当天命中的二十四节气。
         *
         * @param value 节气信息；非节气日为 {@code null}
         * @return 当前构建器
         */
        public Builder solarTerm(SolarTermInfo value) { this.solarTerm = value; return this; }

        /**
         * 设置当天农历年的干支信息。
         *
         * @param value 干支信息；无数据时为 {@code null}
         * @return 当前构建器
         */
        public Builder ganZhi(GanZhiInfo value) { this.ganZhi = value; return this; }

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
         * 构建不可变的 {@link DayInfo} 实例。
         *
         * @return 构造后的实例
         */
        public DayInfo build() {
            return new DayInfo(this);
        }
    }
}
