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
     *
     * @param date 待查询公历日期
     * @return 单日信息
     * @throws HolidayDataUnavailableException 对应年度数据不存在时
     */
    DayInfo getDayInfo(LocalDate date);

    /**
     * 查询指定地区的单日信息。
     *
     * @param regionCode 区域代码，例如 {@code CN}
     * @param date 待查询公历日期
     * @return 单日信息
     * @throws HolidayDataUnavailableException 对应地区或年度数据不存在时
     */
    DayInfo getDayInfo(String regionCode, LocalDate date);

    /**
     * 使用默认地区判断是否休息日。
     *
     * @param date 待查询公历日期
     * @return 当天在放假安排中为休息日时返回 {@code true}
     * @throws HolidayDataUnavailableException 对应年度数据不存在时
     */
    boolean isHoliday(LocalDate date);

    /**
     * 使用默认地区判断是否工作日。
     *
     * @param date 待查询公历日期
     * @return 当天需要工作时返回 {@code true}
     * @throws HolidayDataUnavailableException 对应年度数据不存在时
     */
    boolean isWorkday(LocalDate date);

    /**
     * 使用默认地区判断是否法定节假日。
     *
     * @param date 待查询公历日期
     * @return 当天属于法定节假日时返回 {@code true}
     * @throws HolidayDataUnavailableException 对应年度数据不存在时
     */
    boolean isStatutoryHoliday(LocalDate date);

    /**
     * 使用默认地区判断是否调休补班。
     *
     * @param date 待查询公历日期
     * @return 当天为调休补班日时返回 {@code true}
     * @throws HolidayDataUnavailableException 对应年度数据不存在时
     */
    boolean isAdjustedWorkday(LocalDate date);

    /**
     * 使用默认地区查询闭区间范围内的日期。
     *
     * @param from 起始日期，包含
     * @param to 结束日期，包含
     * @return 按日期升序排列的不可变结果列表
     * @throws HolidayDataUnavailableException 区间内任一年度数据不存在时
     */
    List<DayInfo> getRange(LocalDate from, LocalDate to);

    /**
     * 查询指定地区的闭区间范围。
     *
     * @param regionCode 区域代码
     * @param from 起始日期，包含
     * @param to 结束日期，包含
     * @return 按日期升序排列的不可变结果列表
     * @throws HolidayDataUnavailableException 区间内任一地区或年度数据不存在时
     */
    List<DayInfo> getRange(String regionCode, LocalDate from, LocalDate to);

    /**
     * 使用默认地区查询整年。
     *
     * @param year 公历年份
     * @return 整年不可变结果列表
     * @throws HolidayDataUnavailableException 数据不存在时
     */
    List<DayInfo> getYear(int year);

    /**
     * 查询指定地区的整年数据。
     *
     * @param regionCode 区域代码
     * @param year 公历年份
     * @return 整年不可变结果列表
     * @throws HolidayDataUnavailableException 数据不存在时
     */
    List<DayInfo> getYear(String regionCode, int year);

    /**
     * 查询指定月份的所有日期信息（默认地区）。
     *
     * @param year 公历年份
     * @param month 公历月份，范围 1–12
     * @return 指定月份的不可变结果列表
     * @throws HolidayDataUnavailableException 对应年度数据不存在时
     */
    List<DayInfo> getMonth(int year, int month);

    /**
     * 查询指定地区指定月份的所有日期信息。
     *
     * @param regionCode 区域代码
     * @param year 公历年份
     * @param month 公历月份，范围 1–12
     * @return 指定月份的不可变结果列表
     * @throws HolidayDataUnavailableException 对应地区或年度数据不存在时
     */
    List<DayInfo> getMonth(String regionCode, int year, int month);

    /**
     * 统计闭区间内的工作日天数（默认地区）。
     *
     * @param from 起始日期，包含
     * @param to 结束日期，包含
     * @return 闭区间内的工作日数量
     * @throws HolidayDataUnavailableException 区间内任一年度数据不存在时
     */
    int countWorkdays(LocalDate from, LocalDate to);

    /**
     * 统计指定地区闭区间内的工作日天数。
     *
     * @param regionCode 区域代码
     * @param from 起始日期，包含
     * @param to 结束日期，包含
     * @return 闭区间内的工作日数量
     * @throws HolidayDataUnavailableException 区间内任一地区或年度数据不存在时
     */
    int countWorkdays(String regionCode, LocalDate from, LocalDate to);

    /**
     * 从指定日期（含）起查找下一个法定节假日（默认地区）。
     *
     * @param from 搜索起始日期，包含
     * @return 下一个法定节假日；可用数据范围内没有结果时返回 {@code null}
     * @throws HolidayDataUnavailableException 起始年度数据不存在时
     */
    DayInfo getNextHoliday(LocalDate from);

    /**
     * 从指定日期（含）起查找指定地区的下一个法定节假日。
     *
     * @param regionCode 区域代码
     * @param from 搜索起始日期，包含
     * @return 下一个法定节假日；可用数据范围内没有结果时返回 {@code null}
     * @throws HolidayDataUnavailableException 起始年度或已声明的后续数据不存在时
     */
    DayInfo getNextHoliday(String regionCode, LocalDate from);

    /**
     * 清空已加载的年度 bundle，使后续查询从数据源重新加载。
     *
     * <p>替换外部 {@code .hday} 文件后调用此方法即可热更新数据，无需重建服务实例。</p>
     */
    void clearCache();
}
