package com.github.gmkits.holiday.core;

import com.github.gmkits.holiday.spec.CalendarSystem;
import com.github.gmkits.holiday.spec.DayInfo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * `.hday` bundle 的内存表示。
 *
 * <p>对象在构造阶段就会把每日数据预组装为 {@link DayInfo} 数组，后续单日、区间、整年查询均直接复用，
 * 避免热点路径上重复创建 {@link LocalDate}、名称映射和标签集合。</p>
 */
public final class HdayBundle {

    /** 二进制格式中的“无索引”哨兵值。 */
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
    private final DayInfo[] dayInfos;
    private final List<DayInfo> yearView;

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
        this.dayInfos = buildDayInfos();
        this.yearView = ImmutableList.copyOf(this.dayInfos);
    }

    public int getYear() { return year; }

    public String getRegionCode() { return regionCode; }

    public CalendarSystem getCalendarSystem() { return calendarSystem; }

    public int getDayCount() { return dayCount; }

    public int getMajorVersion() { return majorVersion; }

    public int getMinorVersion() { return minorVersion; }

    /**
     * 按 dayIndex 直接返回预构建结果。
     */
    public DayInfo getDayInfo(int dayIndex) {
        if (dayIndex < 0 || dayIndex >= dayCount) {
            throw new IndexOutOfBoundsException("dayIndex=" + dayIndex + ", dayCount=" + dayCount);
        }
        return dayInfos[dayIndex];
    }

    /**
     * 按日期查询，超出年份范围时返回 {@code null}。
     */
    public DayInfo getDayInfo(LocalDate date) {
        if (date.getYear() != year) {
            return null;
        }
        int dayIndex = date.getDayOfYear() - 1;
        if (dayIndex < 0 || dayIndex >= dayCount) {
            return null;
        }
        return dayInfos[dayIndex];
    }

    /**
     * 返回整年视图。
     */
    public List<DayInfo> getDayInfos() {
        return yearView;
    }

    /**
     * 返回闭区间 dayIndex 范围。
     */
    public List<DayInfo> getRange(int startDayIndex, int endDayIndex) {
        if (startDayIndex > endDayIndex) {
            return ImmutableList.of();
        }
        int start = Math.max(0, startDayIndex);
        int end = Math.min(dayCount - 1, endDayIndex);
        if (start > end) {
            return ImmutableList.of();
        }
        return new ArrayList<>(yearView.subList(start, end + 1));
    }

    /**
     * 统计闭区间 dayIndex 范围内的工作日数量。
     * 直接在预构建数组上计数，避免创建中间列表。
     */
    public int countWorkdays(int startDayIndex, int endDayIndex) {
        int start = Math.max(0, startDayIndex);
        int end = Math.min(dayCount - 1, endDayIndex);
        if (start > end) {
            return 0;
        }
        int count = 0;
        for (int i = start; i <= end; i++) {
            if (dayInfos[i].isWorkday()) {
                count++;
            }
        }
        return count;
    }

    private DayInfo[] buildDayInfos() {
        DayInfo[] result = new DayInfo[dayCount];
        LocalDate cursor = LocalDate.of(year, 1, 1);
        for (int i = 0; i < dayCount; i++) {
            DayEntry entry = days[i];
            result[i] = new DayInfo.Builder()
                    .date(cursor)
                    .regionCode(regionCode)
                    .calendarSystem(calendarSystem)
                    .holiday(entry.isHoliday())
                    .workday(entry.isWorkday())
                    .weekend(entry.isWeekend())
                    .statutoryHoliday(entry.isStatutoryHoliday())
                    .adjustedWorkday(entry.isAdjustedWorkday())
                    .holidayNames(resolveNames(entry.nameListIndex))
                    .labels(resolveLabels(entry.labelListIndex))
                    .build();
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    private Map<String, List<String>> resolveNames(int listIndex) {
        if (listIndex == NO_INDEX || nameLists == null || listIndex >= nameLists.length) {
            return ImmutableMap.of();
        }
        int[][] pairs = nameLists[listIndex];
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (int[] pair : pairs) {
            int keyIdx = pair[0];
            int valIdx = pair[1];
            if (keyIdx == NO_INDEX) {
                continue;
            }
            if (valIdx == NO_INDEX) {
                continue;
            }
            if (keyIdx >= strings.length || valIdx >= strings.length) {
                continue;
            }
            String key = strings[keyIdx];
            String value = strings[valIdx];
            List<String> list = result.get(key);
            if (list == null) {
                list = new ArrayList<>();
                result.put(key, list);
            }
            list.add(value);
        }
        if (result.isEmpty()) {
            return ImmutableMap.of();
        }
        return result;
    }

    private List<String> resolveLabels(int listIndex) {
        if (listIndex == NO_INDEX || nameLists == null || listIndex >= nameLists.length) {
            return ImmutableList.of();
        }
        int[][] pairs = nameLists[listIndex];
        List<String> result = new ArrayList<>();
        for (int[] pair : pairs) {
            int keyIdx = pair[0];
            int valIdx = pair[1];
            if (keyIdx != NO_INDEX) {
                continue;
            }
            if (valIdx == NO_INDEX || valIdx >= strings.length) {
                continue;
            }
            result.add(strings[valIdx]);
        }
        if (result.isEmpty()) {
            return ImmutableList.of();
        }
        return result;
    }

    /**
     * 二进制 day table 中的单日记录。
     */
    static final class DayEntry {
        static final int FLAG_IS_HOLIDAY = 0x0001;
        static final int FLAG_IS_WORKDAY = 0x0002;
        static final int FLAG_IS_WEEKEND = 0x0004;
        static final int FLAG_IS_STATUTORY_HOLIDAY = 0x0008;
        static final int FLAG_IS_ADJUSTED_WORKDAY = 0x0010;
        static final int FLAG_HAS_NAME = 0x0020;
        static final int FLAG_HAS_LABEL = 0x0040;

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

        boolean isHoliday() { return (flags & FLAG_IS_HOLIDAY) != 0; }

        boolean isWorkday() { return (flags & FLAG_IS_WORKDAY) != 0; }

        boolean isWeekend() { return (flags & FLAG_IS_WEEKEND) != 0; }

        boolean isStatutoryHoliday() { return (flags & FLAG_IS_STATUTORY_HOLIDAY) != 0; }

        boolean isAdjustedWorkday() { return (flags & FLAG_IS_ADJUSTED_WORKDAY) != 0; }
    }
}
