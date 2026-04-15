package com.github.gmkits.holiday.lunar;

/**
 * 农历日期。
 */
@lombok.Value
public class LunarDate {
    int year;
    int month;
    int day;
    boolean leapMonth;
}
