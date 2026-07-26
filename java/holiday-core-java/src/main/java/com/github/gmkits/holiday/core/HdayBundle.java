package com.github.gmkits.holiday.core;

import com.github.gmkits.holiday.lunar.LunarCalendar;
import com.github.gmkits.holiday.lunar.LunarInfo;
import com.github.gmkits.holiday.spec.CalendarSystem;
import com.github.gmkits.holiday.spec.DayInfo;
import com.github.gmkits.holiday.spec.GanZhiInfo;
import com.github.gmkits.holiday.spec.LunarDateInfo;
import com.github.gmkits.holiday.spec.SolarTermInfo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    private final String sourceVersion;
    private final DayInfo[] dayInfos;
    private final List<DayInfo> yearView;
    private final int[] workdayPrefix;
    private final int[] nextStatutoryIndex;

    HdayBundle(int year, String regionCode, CalendarSystem calendarSystem,
               int dayCount, int majorVersion, int minorVersion,
               DayEntry[] days, String[] strings, int[][][] nameLists,
               String sourceVersion) {
        this.year = year;
        this.regionCode = regionCode;
        this.calendarSystem = calendarSystem;
        this.dayCount = dayCount;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.days = days;
        this.strings = strings;
        this.nameLists = nameLists;
        this.sourceVersion = sourceVersion;
        this.dayInfos = buildDayInfos();
        this.yearView = Collections.unmodifiableList(Arrays.asList(this.dayInfos));
        this.workdayPrefix = buildWorkdayPrefix();
        this.nextStatutoryIndex = buildNextStatutoryIndex();
    }

    /**
     * 返回数据包所属公历年份。
     *
     * @return 数据包所属公历年份
     */
    public int getYear() { return year; }

    /**
     * 返回数据包所属区域代码。
     *
     * @return 数据包所属区域代码
     */
    public String getRegionCode() { return regionCode; }

    /**
     * 返回数据包内实际包含的日期数量。
     *
     * @return 数据包内实际包含的日期数量
     */
    public int getDayCount() { return dayCount; }

    /**
     * 返回 {@code .hday} 格式主版本号。
     *
     * @return {@code .hday} 格式主版本号
     */
    public int getMajorVersion() { return majorVersion; }

    /**
     * 返回 {@code .hday} 格式次版本号。
     *
     * @return {@code .hday} 格式次版本号
     */
    public int getMinorVersion() { return minorVersion; }

    /**
     * 按 dayIndex 直接返回预构建结果。
     *
     * @param dayIndex 从 0 开始的年内日期索引
     * @return 对应的不可变单日信息
     * @throws IndexOutOfBoundsException 索引超出数据包范围时抛出
     */
    public DayInfo getDayInfo(int dayIndex) {
        if (dayIndex < 0 || dayIndex >= dayCount) {
            throw new IndexOutOfBoundsException("dayIndex=" + dayIndex + ", dayCount=" + dayCount);
        }
        return dayInfos[dayIndex];
    }

    /**
     * 按日期查询，超出年份范围时返回 {@code null}。
     *
     * @param date 待查询公历日期
     * @return 对应单日信息；日期不属于当前数据包年份时返回 {@code null}
     */
    public DayInfo getDayInfo(LocalDate date) {
        if (date.getYear() != year) {
            return null;
        }
        int dayIndex = date.getDayOfYear() - 1;
        if (dayIndex >= dayCount) {
            return null;
        }
        return dayInfos[dayIndex];
    }

    /**
     * 返回整年视图。
     *
     * @return 复用内部预构建对象的不可变整年列表
     */
    public List<DayInfo> getDayInfos() {
        return yearView;
    }

    /**
     * 返回闭区间 dayIndex 范围。
     *
     * @param startDayIndex 起始年内索引，包含
     * @param endDayIndex 结束年内索引，包含
     * @return 截断到有效范围后的不可变列表
     */
    public List<DayInfo> getRange(int startDayIndex, int endDayIndex) {
        if (startDayIndex > endDayIndex) {
            return Collections.emptyList();
        }
        int start = Math.max(0, startDayIndex);
        int end = Math.min(dayCount - 1, endDayIndex);
        if (start > end) {
            return Collections.emptyList();
        }
        if (start == 0 && end == dayCount - 1) {
            return yearView;
        }
        return yearView.subList(start, end + 1);
    }

    /**
     * 直接将闭区间 dayIndex 范围追加到目标列表，避免创建中间子视图。
     */
    void appendRangeTo(List<DayInfo> target, int startDayIndex, int endDayIndex) {
        int start = Math.max(0, startDayIndex);
        int end = Math.min(dayCount - 1, endDayIndex);
        if (start > end) {
            return;
        }
        for (int i = start; i <= end; i++) {
            target.add(dayInfos[i]);
        }
    }

    /**
     * 统计闭区间 dayIndex 范围内的工作日数量。
     * 直接在预构建数组上计数，避免创建中间列表。
     *
     * @param startDayIndex 起始年内索引，包含
     * @param endDayIndex 结束年内索引，包含
     * @return 有效范围内的工作日数量
     */
    public int countWorkdays(int startDayIndex, int endDayIndex) {
        int start = Math.max(0, startDayIndex);
        int end = Math.min(dayCount - 1, endDayIndex);
        if (start > end) {
            return 0;
        }
        return workdayPrefix[end + 1] - workdayPrefix[start];
    }

    /**
     * 从指定起点开始扫描下一个法定节假日。
     */
    DayInfo findStatutoryHoliday(int startDayIndex) {
        int start = Math.max(0, startDayIndex);
        if (start >= dayCount) {
            return null;
        }
        int index = nextStatutoryIndex[start];
        return index < 0 ? null : dayInfos[index];
    }

    private DayInfo[] buildDayInfos() {
        DayInfo[] result = new DayInfo[dayCount];
        LocalDate cursor = LocalDate.of(year, 1, 1);
        for (int i = 0; i < dayCount; i++) {
            DayEntry entry = days[i];
            LunarInfo rawLunar = resolveLunarInfo(cursor);
            LunarDateInfo lunar = toLunarDateInfo(rawLunar);
            GanZhiInfo ganZhi = toGanZhiInfo(rawLunar);
            SolarTermInfo solarTerm = SolarTermTable.lookup(cursor.getYear(), i);
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
                    .lunar(lunar)
                    .solarTerm(solarTerm)
                    .ganZhi(ganZhi)
                    .festivals(FestivalResolver.resolve(cursor, lunar, solarTerm))
                    .sourceVersion(sourceVersion)
                    .build();
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    private int[] buildWorkdayPrefix() {
        int[] prefix = new int[dayCount + 1];
        for (int i = 0; i < dayCount; i++) {
            prefix[i + 1] = prefix[i] + (dayInfos[i].isWorkday() ? 1 : 0);
        }
        return prefix;
    }

    private int[] buildNextStatutoryIndex() {
        int[] indexes = new int[dayCount];
        int next = -1;
        for (int i = dayCount - 1; i >= 0; i--) {
            if (dayInfos[i].isStatutoryHoliday()) {
                next = i;
            }
            indexes[i] = next;
        }
        return indexes;
    }

    private LunarInfo resolveLunarInfo(LocalDate date) {
        try {
            return LunarCalendar.solarToLunar(date);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private LunarDateInfo toLunarDateInfo(LunarInfo lunar) {
        if (lunar == null) {
            return null;
        }
        return new LunarDateInfo(
                lunar.getDate().getYear(),
                lunar.getDate().getMonth(),
                lunar.getDate().getDay(),
                lunar.getDate().isLeapMonth(),
                lunar.getMonthName(),
                lunar.getDayName());
    }

    private GanZhiInfo toGanZhiInfo(LunarInfo lunar) {
        if (lunar == null) {
            return null;
        }
        String yearName = lunar.getGanZhiYear();
        if (yearName.endsWith("年")) {
            yearName = yearName.substring(0, yearName.length() - 1);
        }
        return new GanZhiInfo(
                yearName,
                lunar.getTianGan(),
                lunar.getDiZhi(),
                lunar.getShengXiao());
    }

    private Map<String, List<String>> resolveNames(int listIndex) {
        if (listIndex == NO_INDEX || nameLists == null || listIndex >= nameLists.length) {
            return Collections.emptyMap();
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
            List<String> list = result.computeIfAbsent(key, k -> new ArrayList<>());
            list.add(value);
        }
        if (result.isEmpty()) {
            return Collections.emptyMap();
        }
        return result;
    }

    private List<String> resolveLabels(int listIndex) {
        if (listIndex == NO_INDEX || nameLists == null || listIndex >= nameLists.length) {
            return Collections.emptyList();
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
            return Collections.emptyList();
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
