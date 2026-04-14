package com.github.gmkits.holiday.lunar;

/**
 * 农历完整信息（含天干地支、生肖、中文表示）。
 */
public final class LunarInfo {

    private final LunarDate date;
    private final String tianGan;
    private final String diZhi;
    private final String ganZhiYear;
    private final String shengXiao;
    private final String monthName;
    private final String dayName;
    private final String fullName;

    LunarInfo(LunarDate date, String tianGan, String diZhi,
              String ganZhiYear, String shengXiao,
              String monthName, String dayName, String fullName) {
        this.date = date;
        this.tianGan = tianGan;
        this.diZhi = diZhi;
        this.ganZhiYear = ganZhiYear;
        this.shengXiao = shengXiao;
        this.monthName = monthName;
        this.dayName = dayName;
        this.fullName = fullName;
    }

    /** 农历日期。 */
    public LunarDate getDate() { return date; }

    /** 天干。 */
    public String getTianGan() { return tianGan; }

    /** 地支。 */
    public String getDiZhi() { return diZhi; }

    /** 干支年名（如"乙巳年"）。 */
    public String getGanZhiYear() { return ganZhiYear; }

    /** 生肖。 */
    public String getShengXiao() { return shengXiao; }

    /** 月份中文名（如"正月"、"闰四月"）。 */
    public String getMonthName() { return monthName; }

    /** 日期中文名（如"初一"、"十五"）。 */
    public String getDayName() { return dayName; }

    /** 完整中文表示（如"乙巳年 正月初一"）。 */
    public String getFullName() { return fullName; }

    @Override
    public String toString() {
        return fullName;
    }
}
