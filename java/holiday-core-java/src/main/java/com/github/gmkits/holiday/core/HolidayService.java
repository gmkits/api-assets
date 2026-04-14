package com.github.gmkits.holiday.core;

import com.github.gmkits.holiday.spec.DayInfo;

import java.time.LocalDate;
import java.util.List;

/**
 * 节假日查询服务主接口。
 *
 * <p>实现类会从预编译的 {@code .hday} bundle 中解析指定地区、指定年份的节假日数据。</p>
 */
public interface HolidayService {

    /**
     * 使用默认地区查询单日信息。
     */
    DayInfo getDayInfo(LocalDate date);

    /**
     * 查询指定地区的单日信息。
     */
    DayInfo getDayInfo(String regionCode, LocalDate date);

    /**
     * 使用默认地区判断是否休息日。
     */
    boolean isHoliday(LocalDate date);

    /**
     * 使用默认地区判断是否工作日。
     */
    boolean isWorkday(LocalDate date);

    /**
     * 使用默认地区判断是否法定节假日。
     */
    boolean isStatutoryHoliday(LocalDate date);

    /**
     * 使用默认地区判断是否调休补班。
     */
    boolean isAdjustedWorkday(LocalDate date);

    /**
     * 使用默认地区查询闭区间范围内的日期。
     */
    List<DayInfo> getRange(LocalDate from, LocalDate to);

    /**
     * 查询指定地区的闭区间范围。
     */
    List<DayInfo> getRange(String regionCode, LocalDate from, LocalDate to);

    /**
     * 使用默认地区查询整年。
     */
    List<DayInfo> getYear(int year);

    /**
     * 查询指定地区的整年数据。
     */
    List<DayInfo> getYear(String regionCode, int year);

    /**
     * 查询指定月份的所有日期信息（默认地区）。
     */
    List<DayInfo> getMonth(int year, int month);

    /**
     * 查询指定地区指定月份的所有日期信息。
     */
    List<DayInfo> getMonth(String regionCode, int year, int month);

    /**
     * 统计闭区间内的工作日天数（默认地区）。
     */
    int countWorkdays(LocalDate from, LocalDate to);

    /**
     * 统计指定地区闭区间内的工作日天数。
     */
    int countWorkdays(String regionCode, LocalDate from, LocalDate to);

    /**
     * 从指定日期（含）起查找下一个法定节假日（默认地区）。
     */
    DayInfo getNextHoliday(LocalDate from);

    /**
     * 从指定日期（含）起查找指定地区的下一个法定节假日。
     */
    DayInfo getNextHoliday(String regionCode, LocalDate from);
}
