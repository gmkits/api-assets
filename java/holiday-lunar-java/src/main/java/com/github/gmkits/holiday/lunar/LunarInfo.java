package com.github.gmkits.holiday.lunar;

import java.util.Objects;

/**
 * 不可变的农历完整信息。
 *
 * <p>除数值日期外，还包含天干、地支、干支纪年、生肖和中文日期表示，
 * 适合直接用于 API 响应或界面展示。</p>
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

    /**
     * 创建完整农历信息。
     *
     * @param date 农历数值日期
     * @param tianGan 年天干
     * @param diZhi 年地支
     * @param ganZhiYear 完整干支纪年
     * @param shengXiao 生肖
     * @param monthName 月份中文名
     * @param dayName 日期中文名
     * @param fullName 完整中文日期
     */
    public LunarInfo(LunarDate date, String tianGan, String diZhi, String ganZhiYear,
                     String shengXiao, String monthName, String dayName, String fullName) {
        this.date = date;
        this.tianGan = tianGan;
        this.diZhi = diZhi;
        this.ganZhiYear = ganZhiYear;
        this.shengXiao = shengXiao;
        this.monthName = monthName;
        this.dayName = dayName;
        this.fullName = fullName;
    }

    /**
     * 返回农历数值日期。
     *
     * @return 农历数值日期
     */
    public LunarDate getDate() { return date; }

    /**
     * 返回年天干。
     *
     * @return 年天干
     */
    public String getTianGan() { return tianGan; }

    /**
     * 返回年地支。
     *
     * @return 年地支
     */
    public String getDiZhi() { return diZhi; }

    /**
     * 返回完整干支纪年。
     *
     * @return 完整干支纪年，例如“甲辰年”
     */
    public String getGanZhiYear() { return ganZhiYear; }

    /**
     * 返回生肖中文名。
     *
     * @return 生肖中文名
     */
    public String getShengXiao() { return shengXiao; }

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
     * 返回完整农历中文日期。
     *
     * @return 完整农历中文日期
     */
    public String getFullName() { return fullName; }

    /**
     * 按全部字段比较两个农历信息对象。
     *
     * @param other 待比较对象
     * @return 全部字段相同时返回 {@code true}
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof LunarInfo)) return false;
        LunarInfo that = (LunarInfo) other;
        return Objects.equals(date, that.date)
                && Objects.equals(tianGan, that.tianGan)
                && Objects.equals(diZhi, that.diZhi)
                && Objects.equals(ganZhiYear, that.ganZhiYear)
                && Objects.equals(shengXiao, that.shengXiao)
                && Objects.equals(monthName, that.monthName)
                && Objects.equals(dayName, that.dayName)
                && Objects.equals(fullName, that.fullName);
    }

    /**
     * 返回全部字段的组合哈希值。
     *
     * @return 全部字段的组合哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(date, tianGan, diZhi, ganZhiYear, shengXiao,
                monthName, dayName, fullName);
    }

    /**
     * 返回完整农历信息的调试字符串。
     *
     * @return 完整农历信息的调试字符串
     */
    @Override
    public String toString() {
        return "LunarInfo{date=" + date + ", ganZhiYear='" + ganZhiYear
                + "', shengXiao='" + shengXiao + "', fullName='" + fullName + "'}";
    }
}
