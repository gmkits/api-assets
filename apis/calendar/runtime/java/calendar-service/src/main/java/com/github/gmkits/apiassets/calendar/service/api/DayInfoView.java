package com.github.gmkits.apiassets.calendar.service.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.gmkits.apiassets.calendar.spec.CalendarSystem;
import com.github.gmkits.apiassets.calendar.spec.DayInfo;
import com.github.gmkits.apiassets.calendar.spec.FestivalInfo;
import com.github.gmkits.apiassets.calendar.spec.GanZhiInfo;
import com.github.gmkits.apiassets.calendar.spec.LunarDateInfo;
import com.github.gmkits.apiassets.calendar.spec.SolarTermInfo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** HTTP 契约中的单日视图，和核心领域对象完全解耦。 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class DayInfoView {
    private final LocalDate date;
    private final String regionCode;
    private final String locale;
    private final boolean localeFallback;
    private final CalendarSystem calendarSystem;
    private final boolean holiday;
    private final boolean officialHoliday;
    private final boolean workday;
    private final boolean weekend;
    private final boolean statutoryHoliday;
    private final boolean adjustedWorkday;
    private final List<String> holidayNames;
    private final List<String> labels;
    private final LunarDateInfo lunar;
    private final SolarTermInfo solarTerm;
    private final GanZhiInfo ganZhi;
    private final List<FestivalView> festivals;
    private final String sourceVersion;

    private DayInfoView(DayInfo day, CalendarQueryOptions options) {
        this.date = day.getDate();
        this.regionCode = day.getRegionCode();
        this.locale = options.locale();
        this.calendarSystem = day.getCalendarSystem();
        this.holiday = day.isHoliday();
        this.officialHoliday = day.isOfficialHoliday();
        this.workday = day.isWorkday();
        this.weekend = day.isWeekend();
        this.statutoryHoliday = day.isStatutoryHoliday();
        this.adjustedWorkday = day.isAdjustedWorkday();
        this.sourceVersion = day.getSourceVersion();

        boolean fallback = false;
        if (options.includes(CalendarQueryOptions.Field.HOLIDAY_NAMES)) {
            NameSelection names = select(day.getHolidayNames(), options.locale());
            this.holidayNames = names.values();
            fallback |= names.fallback();
        } else {
            this.holidayNames = null;
        }
        this.labels = options.includes(CalendarQueryOptions.Field.LABELS)
                ? day.getLabels() : null;
        this.lunar = options.includes(CalendarQueryOptions.Field.LUNAR)
                ? day.getLunar() : null;
        this.solarTerm = options.includes(CalendarQueryOptions.Field.SOLAR_TERM)
                ? day.getSolarTerm() : null;
        this.ganZhi = options.includes(CalendarQueryOptions.Field.GAN_ZHI)
                ? day.getGanZhi() : null;
        if (options.includes(CalendarQueryOptions.Field.FESTIVALS)) {
            List<FestivalView> selected = new ArrayList<>(day.getFestivals().size());
            for (FestivalInfo festival : day.getFestivals()) {
                NameSelection name = selectSingle(festival.getNames(), options.locale());
                selected.add(new FestivalView(festival.getCode(), name.values().isEmpty()
                        ? "" : name.values().get(0)));
                fallback |= name.fallback();
            }
            this.festivals = selected.isEmpty()
                    ? Collections.emptyList() : Collections.unmodifiableList(selected);
        } else {
            this.festivals = null;
        }
        this.localeFallback = fallback;
    }

    public static DayInfoView of(DayInfo day, CalendarQueryOptions options) {
        return new DayInfoView(day, options);
    }

    private static NameSelection select(Map<String, List<String>> values, String locale) {
        List<String> selected = values.get(locale);
        if (selected != null && !selected.isEmpty()) {
            return new NameSelection(List.copyOf(selected), false);
        }
        if ("en-US".equals(locale)) {
            List<String> fallback = values.get("zh-CN");
            if (fallback != null && !fallback.isEmpty()) {
                return new NameSelection(List.copyOf(fallback), true);
            }
        }
        return new NameSelection(Collections.emptyList(), false);
    }

    private static NameSelection selectSingle(Map<String, String> values, String locale) {
        String selected = values.get(locale);
        if (selected != null && !selected.isBlank()) {
            return new NameSelection(List.of(selected), false);
        }
        if ("en-US".equals(locale)) {
            String fallback = values.get("zh-CN");
            if (fallback != null && !fallback.isBlank()) {
                return new NameSelection(List.of(fallback), true);
            }
        }
        return new NameSelection(Collections.emptyList(), false);
    }

    private record NameSelection(List<String> values, boolean fallback) { }

    public record FestivalView(String code, String name) { }

    public LocalDate getDate() { return date; }
    public String getRegionCode() { return regionCode; }
    public String getLocale() { return locale; }
    @JsonProperty("localeFallback") public boolean isLocaleFallback() { return localeFallback; }
    public CalendarSystem getCalendarSystem() { return calendarSystem; }
    @JsonProperty("isHoliday") public boolean isHoliday() { return holiday; }
    @JsonProperty("isOfficialHoliday") public boolean isOfficialHoliday() { return officialHoliday; }
    @JsonProperty("isWorkday") public boolean isWorkday() { return workday; }
    @JsonProperty("isWeekend") public boolean isWeekend() { return weekend; }
    @JsonProperty("isStatutoryHoliday") public boolean isStatutoryHoliday() { return statutoryHoliday; }
    @JsonProperty("isAdjustedWorkday") public boolean isAdjustedWorkday() { return adjustedWorkday; }
    public List<String> getHolidayNames() { return holidayNames; }
    public List<String> getLabels() { return labels; }
    public LunarDateInfo getLunar() { return lunar; }
    public SolarTermInfo getSolarTerm() { return solarTerm; }
    public GanZhiInfo getGanZhi() { return ganZhi; }
    public List<FestivalView> getFestivals() { return festivals; }
    public String getSourceVersion() { return sourceVersion; }
}
