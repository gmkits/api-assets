package com.github.gmkits.holiday.lunar;

/**
 * 农历日期。
 */
public final class LunarDate {

    private final int year;
    private final int month;
    private final int day;
    private final boolean leapMonth;

    public LunarDate(int year, int month, int day, boolean leapMonth) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.leapMonth = leapMonth;
    }

    /** 农历年。 */
    public int getYear() { return year; }

    /** 农历月（1-12）。 */
    public int getMonth() { return month; }

    /** 农历日（1-30）。 */
    public int getDay() { return day; }

    /** 是否闰月。 */
    public boolean isLeapMonth() { return leapMonth; }

    @Override
    public String toString() {
        return "LunarDate{year=" + year + ", month=" + month
                + ", day=" + day + ", leapMonth=" + leapMonth + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LunarDate)) return false;
        LunarDate that = (LunarDate) o;
        return year == that.year && month == that.month
                && day == that.day && leapMonth == that.leapMonth;
    }

    @Override
    public int hashCode() {
        int h = 17;
        h = 31 * h + year;
        h = 31 * h + month;
        h = 31 * h + day;
        h = 31 * h + (leapMonth ? 1 : 0);
        return h;
    }
}
