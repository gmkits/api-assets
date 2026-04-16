package com.github.gmkits.holiday.spec;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.Getter;

import java.time.LocalDate;
import java.util.ArrayList;
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
@Getter
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
        this.labels = b.labels == null ? ImmutableList.of()
                : ImmutableList.copyOf(b.labels);
        this.sourceVersion = b.sourceVersion;
        this.extensions = b.extensions == null ? ImmutableMap.of()
                : ImmutableMap.copyOf(b.extensions);
    }

    private static Map<String, List<String>> freezeNames(Map<String, List<String>> src) {
        if (src == null || src.isEmpty()) {
            return ImmutableMap.of();
        }
        ImmutableMap.Builder<String, List<String>> copy = ImmutableMap.builder();
        for (Map.Entry<String, List<String>> e : src.entrySet()) {
            copy.put(e.getKey(), ImmutableList.copyOf(e.getValue()));
        }
        return copy.build();
    }

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
        private String sourceVersion;
        private Map<String, Object> extensions;

        /** 设置日期。 */
        public Builder date(LocalDate date) { this.date = date; return this; }

        /** 设置区域代码。 */
        public Builder regionCode(String regionCode) { this.regionCode = regionCode; return this; }

        /** 设置历法体系。 */
        public Builder calendarSystem(CalendarSystem cs) { this.calendarSystem = cs; return this; }

        /** 设置节假日标记。 */
        public Builder holiday(boolean v) { this.holiday = v; return this; }

        /** 设置工作日标记。 */
        public Builder workday(boolean v) { this.workday = v; return this; }

        /** 设置周末标记。 */
        public Builder weekend(boolean v) { this.weekend = v; return this; }

        /** 设置法定节假日标记。 */
        public Builder statutoryHoliday(boolean v) { this.statutoryHoliday = v; return this; }

        /** 设置调休补班标记。 */
        public Builder adjustedWorkday(boolean v) { this.adjustedWorkday = v; return this; }

        /** 设置节假日名称映射。 */
        public Builder holidayNames(Map<String, List<String>> names) { this.holidayNames = names; return this; }

        /** 设置标签列表。 */
        public Builder labels(List<String> labels) { this.labels = labels; return this; }

        /** 设置源数据版本。 */
        public Builder sourceVersion(String v) { this.sourceVersion = v; return this; }

        /** 设置扩展映射。 */
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
