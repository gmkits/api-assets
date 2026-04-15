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
 * Information about a single calendar day within a holiday dataset.
 *
 * <p>All boolean fields are always present (never {@code null}).
 * Instances are immutable once constructed via the {@link Builder}.</p>
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
     * Builder for constructing {@link DayInfo} instances.
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

        /** Sets the date. */
        public Builder date(LocalDate date) { this.date = date; return this; }

        /** Sets the region code. */
        public Builder regionCode(String regionCode) { this.regionCode = regionCode; return this; }

        /** Sets the calendar system. */
        public Builder calendarSystem(CalendarSystem cs) { this.calendarSystem = cs; return this; }

        /** Sets the holiday flag. */
        public Builder holiday(boolean v) { this.holiday = v; return this; }

        /** Sets the workday flag. */
        public Builder workday(boolean v) { this.workday = v; return this; }

        /** Sets the weekend flag. */
        public Builder weekend(boolean v) { this.weekend = v; return this; }

        /** Sets the statutory-holiday flag. */
        public Builder statutoryHoliday(boolean v) { this.statutoryHoliday = v; return this; }

        /** Sets the adjusted-workday flag. */
        public Builder adjustedWorkday(boolean v) { this.adjustedWorkday = v; return this; }

        /** Sets the holiday names map. */
        public Builder holidayNames(Map<String, List<String>> names) { this.holidayNames = names; return this; }

        /** Sets the label list. */
        public Builder labels(List<String> labels) { this.labels = labels; return this; }

        /** Sets the source version. */
        public Builder sourceVersion(String v) { this.sourceVersion = v; return this; }

        /** Sets the extensions map. */
        public Builder extensions(Map<String, Object> ext) { this.extensions = ext; return this; }

        /**
         * Builds an immutable {@link DayInfo}.
         *
         * @return the constructed instance
         */
        public DayInfo build() {
            return new DayInfo(this);
        }
    }
}
