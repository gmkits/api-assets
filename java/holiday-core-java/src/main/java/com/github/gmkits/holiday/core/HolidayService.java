package com.github.gmkits.holiday.core;

import com.github.gmkits.holiday.spec.DayInfo;

import java.time.LocalDate;
import java.util.List;

/**
 * Primary service interface for querying holiday information.
 *
 * <p>Implementations resolve day data from pre-compiled {@code .hday} bundles
 * for a given region and year.</p>
 */
public interface HolidayService {

    /**
     * Returns day information for the given date using the default region.
     *
     * @param date the date to query
     * @return day information, or {@code null} if no data is available
     */
    DayInfo getDayInfo(LocalDate date);

    /**
     * Returns day information for the given date and region.
     *
     * @param regionCode the region code (e.g. "CN")
     * @param date       the date to query
     * @return day information, or {@code null} if no data is available
     */
    DayInfo getDayInfo(String regionCode, LocalDate date);

    /**
     * Checks whether the given date is a holiday in the default region.
     *
     * @param date the date to check
     * @return {@code true} if the date is a holiday
     */
    boolean isHoliday(LocalDate date);

    /**
     * Checks whether the given date is a workday in the default region.
     *
     * @param date the date to check
     * @return {@code true} if the date is a workday
     */
    boolean isWorkday(LocalDate date);

    /**
     * Checks whether the given date is a statutory holiday in the default region.
     *
     * @param date the date to check
     * @return {@code true} if the date is a statutory holiday
     */
    boolean isStatutoryHoliday(LocalDate date);

    /**
     * Checks whether the given date is an adjusted (makeup) workday in the default region.
     *
     * @param date the date to check
     * @return {@code true} if the date is an adjusted workday
     */
    boolean isAdjustedWorkday(LocalDate date);

    /**
     * Returns day information for every day in the given date range (inclusive)
     * using the default region.
     *
     * @param from range start (inclusive)
     * @param to   range end (inclusive)
     * @return list of {@link DayInfo} for each day in the range
     */
    List<DayInfo> getRange(LocalDate from, LocalDate to);

    /**
     * Returns day information for every day in the given year using the default region.
     *
     * @param year the calendar year
     * @return list of {@link DayInfo} for the entire year
     */
    List<DayInfo> getYear(int year);
}
