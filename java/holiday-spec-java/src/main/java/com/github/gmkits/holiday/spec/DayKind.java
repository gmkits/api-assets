package com.github.gmkits.holiday.spec;

/**
 * Enumeration of day classification kinds used in holiday calendars.
 */
public enum DayKind {

    /** A holiday established by law (e.g. National Day). */
    STATUTORY_HOLIDAY,

    /** An official public holiday that may not be statutory. */
    OFFICIAL_HOLIDAY,

    /** A workday that compensates for a holiday (makeup day). */
    ADJUSTED_WORKDAY,

    /** A regular workday (Monday–Friday, non-holiday). */
    NORMAL_WORKDAY,

    /** A regular weekend day (Saturday or Sunday). */
    NORMAL_WEEKEND
}
