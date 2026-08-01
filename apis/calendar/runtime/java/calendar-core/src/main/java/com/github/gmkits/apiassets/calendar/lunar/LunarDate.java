package com.github.gmkits.apiassets.calendar.lunar;

import java.util.Objects;

/**
 * 不可变的中国农历日期值对象。
 *
 * <p>月份使用 1–12；遇到闰月时，月份值仍为对应的正常月份，
 * 通过 {@link #isLeapMonth()} 区分。</p>
 */
public final class LunarDate {

    private final int year;
    private final int month;
    private final int day;
    private final boolean leapMonth;

    /**
     * 创建农历日期。
     *
     * @param year 农历年
     * @param month 农历月，范围 1–12
     * @param day 农历日，范围 1–30
     * @param leapMonth 是否为闰月
     */
    public LunarDate(int year, int month, int day, boolean leapMonth) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.leapMonth = leapMonth;
    }

    /**
     * 返回农历年。
     *
     * @return 农历年
     */
    public int getYear() { return year; }

    /**
     * 返回农历月。
     *
     * @return 农历月，范围 1–12
     */
    public int getMonth() { return month; }

    /**
     * 返回农历日。
     *
     * @return 农历日，范围 1–30
     */
    public int getDay() { return day; }

    /**
     * 判断当前月份是否为闰月。
     *
     * @return 当前月份为闰月时返回 {@code true}
     */
    public boolean isLeapMonth() { return leapMonth; }

    /**
     * 按年、月、日和闰月标记比较两个农历日期。
     *
     * @param other 待比较对象
     * @return 所有字段相同时返回 {@code true}
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof LunarDate)) return false;
        LunarDate that = (LunarDate) other;
        return year == that.year && month == that.month && day == that.day
                && leapMonth == that.leapMonth;
    }

    /**
     * 返回所有日期字段的组合哈希值。
     *
     * @return 所有日期字段的组合哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(year, month, day, leapMonth);
    }

    /**
     * 返回农历日期的调试字符串。
     *
     * @return 农历日期的调试字符串
     */
    @Override
    public String toString() {
        return "LunarDate{year=" + year + ", month=" + month + ", day=" + day
                + ", leapMonth=" + leapMonth + "}";
    }
}
