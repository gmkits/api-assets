package com.github.gmkits.holiday.spec;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Common metadata describing a holiday data bundle.
 *
 * <p>All fields follow the cn-holiday-kit specification. This class is
 * immutable once constructed.</p>
 */
public final class CommonMeta {

    private final String specVersion;
    private final String bundleId;
    private final RegionCode regionCode;
    private final RegionCode parentRegionCode;
    private final int year;
    private final LocalDate validFrom;
    private final LocalDate validTo;
    private final CalendarSystem calendarSystem;
    private final String timezone;
    private final int weekendMask;
    private final List<String> locales;
    private final String sourceVersion;
    private final String generatedAt;
    private final Map<String, Object> extensions;

    /**
     * Constructs a new {@code CommonMeta} instance.
     *
     * @param specVersion      specification version (e.g. "1.0")
     * @param bundleId         unique bundle identifier
     * @param regionCode       primary region code
     * @param parentRegionCode parent region code, or {@code null}
     * @param year             calendar year
     * @param validFrom        start of the valid date range
     * @param validTo          end of the valid date range
     * @param calendarSystem   calendar system used
     * @param timezone         IANA timezone identifier
     * @param weekendMask      bitmask encoding weekend days
     * @param locales          supported locale tags
     * @param sourceVersion    source data version
     * @param generatedAt      ISO-8601 generation timestamp
     * @param extensions       extra key-value extensions
     */
    public CommonMeta(String specVersion, String bundleId, RegionCode regionCode,
                      RegionCode parentRegionCode, int year, LocalDate validFrom,
                      LocalDate validTo, CalendarSystem calendarSystem, String timezone,
                      int weekendMask, List<String> locales, String sourceVersion,
                      String generatedAt, Map<String, Object> extensions) {
        this.specVersion = specVersion;
        this.bundleId = bundleId;
        this.regionCode = regionCode;
        this.parentRegionCode = parentRegionCode;
        this.year = year;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.calendarSystem = calendarSystem;
        this.timezone = timezone;
        this.weekendMask = weekendMask;
        this.locales = locales == null ? Collections.<String>emptyList()
                : Collections.unmodifiableList(locales);
        this.sourceVersion = sourceVersion;
        this.generatedAt = generatedAt;
        this.extensions = extensions == null ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(extensions);
    }

    /** Returns the specification version. */
    public String getSpecVersion() { return specVersion; }

    /** Returns the unique bundle identifier. */
    public String getBundleId() { return bundleId; }

    /** Returns the primary region code. */
    public RegionCode getRegionCode() { return regionCode; }

    /** Returns the parent region code, or {@code null} if none. */
    public RegionCode getParentRegionCode() { return parentRegionCode; }

    /** Returns the calendar year. */
    public int getYear() { return year; }

    /** Returns the start of the valid date range. */
    public LocalDate getValidFrom() { return validFrom; }

    /** Returns the end of the valid date range. */
    public LocalDate getValidTo() { return validTo; }

    /** Returns the calendar system. */
    public CalendarSystem getCalendarSystem() { return calendarSystem; }

    /** Returns the IANA timezone identifier. */
    public String getTimezone() { return timezone; }

    /** Returns the weekend bitmask. */
    public int getWeekendMask() { return weekendMask; }

    /** Returns the list of supported locales. */
    public List<String> getLocales() { return locales; }

    /** Returns the source data version string. */
    public String getSourceVersion() { return sourceVersion; }

    /** Returns the ISO-8601 generation timestamp. */
    public String getGeneratedAt() { return generatedAt; }

    /** Returns the extension map. */
    public Map<String, Object> getExtensions() { return extensions; }

    @Override
    public String toString() {
        return "CommonMeta{region=" + regionCode + ", year=" + year + "}";
    }
}
