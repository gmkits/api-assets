package com.github.gmkits.holiday.core;

import com.github.gmkits.holiday.spec.CalendarSystem;
import com.github.gmkits.holiday.spec.DayInfo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A parsed {@code .hday} binary bundle containing header metadata,
 * day entries, a string table, and name-list tables.
 *
 * <p>Instances are created by {@link HdayReader} and are immutable.</p>
 */
public final class HdayBundle {

    /** Sentinel value indicating "no index" in the binary format. */
    static final int NO_INDEX = 0xFFFF;

    private final int year;
    private final String regionCode;
    private final CalendarSystem calendarSystem;
    private final int dayCount;
    private final int majorVersion;
    private final int minorVersion;
    private final DayEntry[] days;
    private final String[] strings;
    private final int[][][] nameLists;

    HdayBundle(int year, String regionCode, CalendarSystem calendarSystem,
               int dayCount, int majorVersion, int minorVersion,
               DayEntry[] days, String[] strings, int[][][] nameLists) {
        this.year = year;
        this.regionCode = regionCode;
        this.calendarSystem = calendarSystem;
        this.dayCount = dayCount;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.days = days;
        this.strings = strings;
        this.nameLists = nameLists;
    }

    /** Returns the calendar year of this bundle. */
    public int getYear() { return year; }

    /** Returns the region code. */
    public String getRegionCode() { return regionCode; }

    /** Returns the calendar system. */
    public CalendarSystem getCalendarSystem() { return calendarSystem; }

    /** Returns the number of days in this bundle. */
    public int getDayCount() { return dayCount; }

    /** Returns the major version of the binary format. */
    public int getMajorVersion() { return majorVersion; }

    /** Returns the minor version of the binary format. */
    public int getMinorVersion() { return minorVersion; }

    /**
     * Constructs a {@link DayInfo} for the given zero-based day index.
     *
     * @param dayIndex zero-based day-of-year index (0 = Jan 1)
     * @return the constructed {@link DayInfo}
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public DayInfo getDayInfo(int dayIndex) {
        if (dayIndex < 0 || dayIndex >= dayCount) {
            throw new IndexOutOfBoundsException("dayIndex=" + dayIndex + ", dayCount=" + dayCount);
        }
        DayEntry entry = days[dayIndex];
        LocalDate date = LocalDate.of(year, 1, 1).plusDays(dayIndex);

        Map<String, List<String>> holidayNames = resolveNames(entry.nameListIndex);
        List<String> labels = resolveLabels(entry.labelListIndex);

        return new DayInfo.Builder()
                .date(date)
                .regionCode(regionCode)
                .calendarSystem(calendarSystem)
                .holiday(entry.isHoliday())
                .workday(entry.isWorkday())
                .weekend(entry.isWeekend())
                .statutoryHoliday(entry.isStatutoryHoliday())
                .adjustedWorkday(entry.isAdjustedWorkday())
                .holidayNames(holidayNames)
                .labels(labels)
                .build();
    }

    /**
     * Constructs a {@link DayInfo} for the given date.
     *
     * @param date the date (must fall within this bundle's year)
     * @return the constructed {@link DayInfo}, or {@code null} if out of range
     */
    public DayInfo getDayInfo(LocalDate date) {
        if (date.getYear() != year) {
            return null;
        }
        int dayIndex = date.getDayOfYear() - 1;
        if (dayIndex < 0 || dayIndex >= dayCount) {
            return null;
        }
        return getDayInfo(dayIndex);
    }

    private Map<String, List<String>> resolveNames(int listIndex) {
        if (listIndex == NO_INDEX || nameLists == null || listIndex >= nameLists.length) {
            return Collections.emptyMap();
        }
        int[][] pairs = nameLists[listIndex];
        Map<String, List<String>> result = new LinkedHashMap<String, List<String>>();
        for (int[] pair : pairs) {
            int keyIdx = pair[0];
            int valIdx = pair[1];
            if (valIdx == NO_INDEX || valIdx >= strings.length) {
                continue;
            }
            String key = (keyIdx == NO_INDEX || keyIdx >= strings.length) ? "" : strings[keyIdx];
            String value = strings[valIdx];
            List<String> list = result.get(key);
            if (list == null) {
                list = new ArrayList<String>();
                result.put(key, list);
            }
            list.add(value);
        }
        return result;
    }

    private List<String> resolveLabels(int listIndex) {
        if (listIndex == NO_INDEX || nameLists == null || listIndex >= nameLists.length) {
            return Collections.emptyList();
        }
        int[][] pairs = nameLists[listIndex];
        List<String> result = new ArrayList<String>();
        for (int[] pair : pairs) {
            int valIdx = pair[1];
            if (valIdx != NO_INDEX && valIdx < strings.length) {
                result.add(strings[valIdx]);
            }
        }
        return result;
    }

    /**
     * A single day entry from the binary day table.
     */
    static final class DayEntry {
        static final int FLAG_IS_HOLIDAY           = 0x0001;
        static final int FLAG_IS_WORKDAY            = 0x0002;
        static final int FLAG_IS_WEEKEND            = 0x0004;
        static final int FLAG_IS_STATUTORY_HOLIDAY  = 0x0008;
        static final int FLAG_IS_ADJUSTED_WORKDAY   = 0x0010;
        static final int FLAG_HAS_NAME              = 0x0020;
        static final int FLAG_HAS_LABEL             = 0x0040;

        final int flags;
        final int nameListIndex;
        final int labelListIndex;
        final int extIndex;

        DayEntry(int flags, int nameListIndex, int labelListIndex, int extIndex) {
            this.flags = flags;
            this.nameListIndex = nameListIndex;
            this.labelListIndex = labelListIndex;
            this.extIndex = extIndex;
        }

        boolean isHoliday()          { return (flags & FLAG_IS_HOLIDAY) != 0; }
        boolean isWorkday()          { return (flags & FLAG_IS_WORKDAY) != 0; }
        boolean isWeekend()          { return (flags & FLAG_IS_WEEKEND) != 0; }
        boolean isStatutoryHoliday() { return (flags & FLAG_IS_STATUTORY_HOLIDAY) != 0; }
        boolean isAdjustedWorkday()  { return (flags & FLAG_IS_ADJUSTED_WORKDAY) != 0; }
    }
}
