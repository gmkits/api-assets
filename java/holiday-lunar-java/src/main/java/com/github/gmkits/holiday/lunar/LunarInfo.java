package com.github.gmkits.holiday.lunar;

/**
 * 农历完整信息（含天干地支、生肖、中文表示）。
 */
@lombok.Value
public class LunarInfo {
    LunarDate date;
    String tianGan;
    String diZhi;
    String ganZhiYear;
    String shengXiao;
    String monthName;
    String dayName;
    String fullName;
}
