package com.github.gmkits.holiday.spec;

/**
 * 农历日期信息。
 *
 * <p>用于 {@link DayInfo#getExtensions()} 中的 {@code "lunar"} 字段，
 * 提供公历日期对应的农历信息。</p>
 */
public final class LunarDateInfo {

    private final int year;
    private final int month;
    private final int day;
    private final boolean leapMonth;
    private final String ganZhiYear;
    private final String shengXiao;
    private final String monthName;
    private final String dayName;

    public LunarDateInfo(int year, int month, int day, boolean leapMonth,
                         String ganZhiYear, String shengXiao,
                         String monthName, String dayName) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.leapMonth = leapMonth;
        this.ganZhiYear = ganZhiYear;
        this.shengXiao = shengXiao;
        this.monthName = monthName;
        this.dayName = dayName;
    }

    /** 农历年。 */
    public int getYear() { return year; }

    /** 农历月（1-12）。 */
    public int getMonth() { return month; }

    /** 农历日（1-30）。 */
    public int getDay() { return day; }

    /** 是否闰月。 */
    public boolean isLeapMonth() { return leapMonth; }

    /** 干支年名（如"乙巳年"）。 */
    public String getGanZhiYear() { return ganZhiYear; }

    /** 生肖。 */
    public String getShengXiao() { return shengXiao; }

    /** 月份中文名（如"正月"）。 */
    public String getMonthName() { return monthName; }

    /** 日期中文名（如"初一"）。 */
    public String getDayName() { return dayName; }

    @Override
    public String toString() {
        return ganZhiYear + " " + monthName + dayName;
    }
}
