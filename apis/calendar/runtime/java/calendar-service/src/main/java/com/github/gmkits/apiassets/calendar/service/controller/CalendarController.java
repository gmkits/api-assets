package com.github.gmkits.apiassets.calendar.service.controller;

import com.github.gmkits.apiassets.calendar.service.ApiException;
import com.github.gmkits.apiassets.calendar.service.CalendarProperties;
import com.github.gmkits.apiassets.calendar.service.ValidatedAssetStore;
import com.github.gmkits.apiassets.calendar.core.HolidayService;
import com.github.gmkits.apiassets.calendar.lunar.LunarCalendar;
import com.github.gmkits.apiassets.calendar.lunar.LunarInfo;
import com.github.gmkits.apiassets.calendar.spec.DayInfo;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

/** Calendar API v1 的唯一业务入口。 */
@RestController
@RequestMapping("/v1/calendar")
public final class CalendarController {
    private final HolidayService holidays;
    private final ValidatedAssetStore assets;
    private final CalendarProperties properties;

    public CalendarController(HolidayService holidays, ValidatedAssetStore assets,
                              CalendarProperties properties) {
        this.holidays = holidays;
        this.assets = assets;
        this.properties = properties;
    }

    @GetMapping("/dates/{date}")
    public DayInfo day(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "CN") String region) {
        return holidays.getDayInfo(region, date);
    }

    @GetMapping("/dates")
    public List<DayInfo> range(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "CN") String region) {
        validateRange(from, to);
        return holidays.getRange(region, from, to);
    }

    @GetMapping("/years/{year}")
    public List<DayInfo> year(@PathVariable int year,
                              @RequestParam(defaultValue = "CN") String region) {
        return holidays.getYear(region, year);
    }

    @GetMapping("/workdays/count")
    public WorkdayCount workdays(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "CN") String region) {
        validateRange(from, to);
        return new WorkdayCount(from, to, region, holidays.countWorkdays(region, from, to));
    }

    @GetMapping("/holidays/next")
    public DayInfo nextHoliday(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(defaultValue = "CN") String region) {
        DayInfo result = holidays.getNextHoliday(region, from);
        if (result == null) {
            throw ApiException.notFound("NO_FUTURE_HOLIDAY",
                    "已安装数据中不存在从 " + from + " 开始的后续法定节假日");
        }
        return result;
    }

    @GetMapping("/lunar/from-solar")
    public LunarInfo lunarFromSolar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return LunarCalendar.solarToLunar(date);
    }

    @GetMapping("/solar/from-lunar")
    public SolarDateResult solarFromLunar(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam int day,
            @RequestParam(defaultValue = "false") boolean leapMonth) {
        return new SolarDateResult(LunarCalendar.lunarToSolar(year, month, day, leapMonth));
    }

    @GetMapping("/solar-terms/{year}")
    public SolarTermYear solarTerms(@PathVariable int year) {
        if (year < assets.solarTermStartYear() || year > assets.solarTermEndYear()) {
            throw ApiException.notFound("CALENDAR_DATA_NOT_AVAILABLE",
                    "节气数据仅覆盖 " + assets.solarTermStartYear() + "-" + assets.solarTermEndYear());
        }
        return new SolarTermYear(year, Arrays.asList(LunarCalendar.getSolarTerms(year)));
    }

    @GetMapping("/metadata")
    public Metadata metadata() {
        return new Metadata(
                properties.getReleaseVersion(), "v1", assets.dataVersion(), assets.generatedAt(),
                List.of(new HolidayCoverage(assets.region(), assets.holidayStartYear(),
                        assets.holidayEndYear())),
                new YearCoverage(assets.lunarStartYear(), assets.lunarEndYear()),
                new YearCoverage(assets.solarTermStartYear(), assets.solarTermEndYear()),
                new BinaryFormats(2, 1), properties.getSourceCommit());
    }

    private static void validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) throw ApiException.badRequest("起始日期不能晚于结束日期");
        if (ChronoUnit.DAYS.between(from, to) > 365) {
            throw ApiException.badRequest("日期范围最多包含 366 天");
        }
    }

    public record WorkdayCount(LocalDate from, LocalDate to, String region, int workdays) { }
    public record SolarDateResult(LocalDate date) { }
    public record SolarTermYear(int year, List<LunarCalendar.SolarTermInfo> terms) { }
    public record HolidayCoverage(String region, int startYear, int endYear) { }
    public record YearCoverage(int startYear, int endYear) { }
    public record BinaryFormats(int hday, int calendarData) { }
    public record Metadata(
            String releaseVersion,
            String contractVersion,
            String dataVersion,
            String generatedAt,
            List<HolidayCoverage> holidays,
            YearCoverage lunar,
            YearCoverage solarTerms,
            BinaryFormats binaryFormats,
            String sourceCommit) { }
}
