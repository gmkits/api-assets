package com.github.gmkits.apiassets.calendar.spec;

/**
 * 农历日期信息。
 *
 * <p>作为 {@link DayInfo#getLunar()} 的稳定值对象，仅描述农历日期。
 * 天干地支和生肖由 {@link DayInfo#getGanZhi()} 单独提供。</p>
 */
public final class LunarDateInfo {

    private final int year;
    private final int month;
    private final int day;
    private final boolean leapMonth;
    private final String monthName;
    private final String dayName;

    /**
     * 创建完整农历日期信息。
     *
     * @param year 农历年
     * @param month 农历月，范围 1–12
     * @param day 农历日，范围 1–30
     * @param leapMonth 是否为闰月
     * @param monthName 农历月份中文名
     * @param dayName 农历日期中文名
     */
    public LunarDateInfo(int year, int month, int day, boolean leapMonth,
                         String monthName, String dayName) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.leapMonth = leapMonth;
        this.monthName = monthName;
        this.dayName = dayName;
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
     * 返回农历月份中文名。
     *
     * @return 农历月份中文名
     */
    public String getMonthName() { return monthName; }

    /**
     * 返回农历日期中文名。
     *
     * @return 农历日期中文名
     */
    public String getDayName() { return dayName; }

    /**
     * 返回农历信息的调试字符串。
     *
     * @return 农历信息的调试字符串
     */
    @Override
    public String toString() {
        return "LunarDateInfo{year=" + year + ", month=" + month + ", day=" + day
                + ", leapMonth=" + leapMonth + "}";
    }
}
