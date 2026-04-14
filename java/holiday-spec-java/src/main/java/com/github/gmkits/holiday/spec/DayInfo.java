package com.github.gmkits.holiday.spec;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
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
        this.labels = b.labels == null ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(b.labels));
        this.sourceVersion = b.sourceVersion;
        this.extensions = b.extensions == null ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, Object>(b.extensions));
    }

    private static Map<String, List<String>> freezeNames(Map<String, List<String>> src) {
        if (src == null || src.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<String>> copy = new LinkedHashMap<String, List<String>>();
        for (Map.Entry<String, List<String>> e : src.entrySet()) {
            copy.put(e.getKey(), Collections.unmodifiableList(new ArrayList<String>(e.getValue())));
        }
        return Collections.unmodifiableMap(copy);
    }

    /** Returns the calendar date. */
    public LocalDate getDate() { return date; }

    /** Returns the region code. */
    public String getRegionCode() { return regionCode; }

    /** Returns the calendar system. */
    public CalendarSystem getCalendarSystem() { return calendarSystem; }

    /** Returns {@code true} if this day is a holiday. */
    public boolean isHoliday() { return holiday; }

    /** Returns {@code true} if this day is a workday. */
    public boolean isWorkday() { return workday; }

    /** Returns {@code true} if this day falls on a weekend. */
    public boolean isWeekend() { return weekend; }

    /** Returns {@code true} if this day is a statutory holiday. */
    public boolean isStatutoryHoliday() { return statutoryHoliday; }

    /** Returns {@code true} if this day is an adjusted (makeup) workday. */
    public boolean isAdjustedWorkday() { return adjustedWorkday; }

    /**
     * Returns the holiday names keyed by locale tag.
     *
     * @return an unmodifiable map; empty if the day has no holiday name
     */
    public Map<String, List<String>> getHolidayNames() { return holidayNames; }

    /**
     * Returns the labels associated with this day.
     *
     * @return an unmodifiable list; empty if no labels
     */
    public List<String> getLabels() { return labels; }

    /** Returns the source data version, or {@code null}. */
    public String getSourceVersion() { return sourceVersion; }

    /** Returns the extension map. */
    public Map<String, Object> getExtensions() { return extensions; }

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
