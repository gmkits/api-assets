package com.github.gmkits.apiassets.calendar.service.controller;

import com.github.gmkits.apiassets.calendar.core.HolidayService;
import com.github.gmkits.apiassets.calendar.service.ApiException;
import com.github.gmkits.apiassets.calendar.service.CalendarProperties;
import com.github.gmkits.apiassets.calendar.service.ValidatedAssetStore;
import com.github.gmkits.apiassets.calendar.service.api.BatchCalendarRequest;
import com.github.gmkits.apiassets.calendar.service.api.BatchCalendarResult;
import com.github.gmkits.apiassets.calendar.service.api.CalendarQueryOptions;
import com.github.gmkits.apiassets.calendar.service.api.CalendarResult;
import com.github.gmkits.apiassets.calendar.service.api.DayInfoView;
import com.github.gmkits.apiassets.calendar.service.api.HolidayPeriodView;
import com.github.gmkits.apiassets.calendar.service.api.WorkdayStatsView;
import com.github.gmkits.apiassets.calendar.lunar.LunarCalendar;
import com.github.gmkits.apiassets.calendar.lunar.LunarInfo;
import com.github.gmkits.apiassets.calendar.spec.DayInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/** Calendar API v1 的唯一业务入口；v1 路径下的契约由当前资产版本整体替换。 */
@RestController
@RequestMapping("/v1/calendar")
public final class CalendarController {
    private static final Pattern REGION = Pattern.compile("[A-Z]{2}(?:-[A-Z0-9]{1,8})*");
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int MAX_GET_RANGE_DAYS = 366;
    private static final int MAX_BATCH_RANGES = 32;
    private static final int MAX_BATCH_DAYS = 4096;

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
    public DayInfoView day(
            @PathVariable String date,
            @RequestParam(defaultValue = "CN") String region,
            @RequestParam(defaultValue = "zh-CN") String locale,
            @RequestParam(required = false) String fields) {
        CalendarQueryOptions options = options(locale, fields);
        DayInfo info = holidays.getDayInfo(normalizeRegion(region), parseDate(date, "date"));
        return DayInfoView.of(info, options);
    }

    @GetMapping("/dates")
    public CalendarResult range(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "CN") String region,
            @RequestParam(defaultValue = "zh-CN") String locale,
            @RequestParam(required = false) String fields) {
        LocalDate start = parseDate(from, "from");
        LocalDate end = parseDate(to, "to");
        validateRange(start, end, MAX_GET_RANGE_DAYS);
        String normalizedRegion = normalizeRegion(region);
        CalendarQueryOptions options = options(locale, fields);
        return collection(normalizedRegion, options, start, end,
                holidays.getRange(normalizedRegion, start, end));
    }

    @PostMapping("/dates:batch")
    public BatchCalendarResult batch(@RequestBody BatchCalendarRequest body) {
        if (body == null || body.ranges() == null || body.ranges().isEmpty()) {
            throw ApiException.badRequest("ranges 不能为空");
        }
        if (body.ranges().size() > MAX_BATCH_RANGES) {
            throw ApiException.badRequest("ranges 最多支持 " + MAX_BATCH_RANGES + " 个范围");
        }
        String region = normalizeRegion(body.region() == null ? "CN" : body.region());
        CalendarQueryOptions options = CalendarQueryOptions.of(body.locale(), body.fields());
        List<Range> normalized = normalizeRanges(body.ranges());
        long totalDays = 0;
        List<DayInfoView> items = new ArrayList<>();
        for (Range range : normalized) {
            totalDays += ChronoUnit.DAYS.between(range.from(), range.to()) + 1;
            if (totalDays > MAX_BATCH_DAYS) {
                throw ApiException.badRequest("批量范围合并后最多支持 " + MAX_BATCH_DAYS + " 天");
            }
            for (DayInfo info : holidays.getRange(region, range.from(), range.to())) {
                items.add(DayInfoView.of(info, options));
            }
        }
        List<BatchCalendarResult.RangeView> ranges = normalized.stream()
                .map(range -> new BatchCalendarResult.RangeView(range.from(), range.to()))
                .toList();
        return new BatchCalendarResult(region, options.locale(), ranges, items.size(),
                Collections.unmodifiableList(items));
    }

    @GetMapping("/months/{year}/{month}")
    public CalendarResult month(
            @PathVariable int year,
            @PathVariable int month,
            @RequestParam(defaultValue = "CN") String region,
            @RequestParam(defaultValue = "zh-CN") String locale,
            @RequestParam(required = false) String fields) {
        validatePathYear(year);
        if (month < 1 || month > 12) throw ApiException.badRequest("月份超出范围: " + month);
        LocalDate start;
        LocalDate end;
        try {
            start = LocalDate.of(year, month, 1);
            end = start.withDayOfMonth(start.lengthOfMonth());
        } catch (DateTimeException exception) {
            throw ApiException.badRequest("日期参数无效");
        }
        String normalizedRegion = normalizeRegion(region);
        CalendarQueryOptions options = options(locale, fields);
        return collection(normalizedRegion, options, start, end,
                holidays.getMonth(normalizedRegion, year, month));
    }

    @GetMapping("/years/{year}")
    public CalendarResult year(
            @PathVariable int year,
            @RequestParam(defaultValue = "CN") String region,
            @RequestParam(defaultValue = "zh-CN") String locale,
            @RequestParam(required = false) String fields) {
        validatePathYear(year);
        String normalizedRegion = normalizeRegion(region);
        CalendarQueryOptions options = options(locale, fields);
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        return collection(normalizedRegion, options, start, end,
                holidays.getYear(normalizedRegion, year));
    }

    @GetMapping("/workdays/count")
    public WorkdayStatsView workdays(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "CN") String region) {
        LocalDate start = parseDate(from, "from");
        LocalDate end = parseDate(to, "to");
        validateRange(start, end, MAX_GET_RANGE_DAYS);
        return WorkdayStatsView.of(holidays.getWorkdayStats(normalizeRegion(region), start, end));
    }

    @GetMapping("/holidays")
    public HolidayYearResult holidayPeriods(
            @RequestParam int year,
            @RequestParam(defaultValue = "CN") String region,
            @RequestParam(defaultValue = "zh-CN") String locale) {
        validatePathYear(year);
        String normalizedRegion = normalizeRegion(region);
        String normalizedLocale = CalendarQueryOptions.normalizeLocale(locale);
        List<HolidayPeriodView> periods = holidays.getHolidayPeriods(normalizedRegion, year).stream()
                .map(period -> HolidayPeriodView.of(period, normalizedLocale))
                .toList();
        return new HolidayYearResult(year, normalizedRegion, normalizedLocale, periods);
    }

    @GetMapping("/holidays/next")
    public DayInfoView nextHoliday(
            @RequestParam String from,
            @RequestParam(defaultValue = "CN") String region,
            @RequestParam(defaultValue = "zh-CN") String locale) {
        LocalDate start = parseDate(from, "from");
        String normalizedRegion = normalizeRegion(region);
        DayInfo result = holidays.getNextHoliday(normalizedRegion, start);
        if (result == null) {
            throw ApiException.notFound("NO_FUTURE_HOLIDAY",
                    "已安装数据中不存在从 " + start + " 开始的后续法定节假日");
        }
        return DayInfoView.of(result, CalendarQueryOptions.of(locale, (String) null));
    }

    @GetMapping("/regions")
    public RegionCatalog regions() {
        RegionInfo info = new RegionInfo(
                assets.region(), "中国大陆", "Asia/Shanghai", List.of("SAT", "SUN"),
                List.of("zh-CN", "en-US"),
                new YearCoverage(assets.holidayStartYear(), assets.holidayEndYear()),
                new YearCoverage(assets.lunarStartYear(), assets.lunarEndYear()),
                new YearCoverage(assets.solarTermStartYear(), assets.solarTermEndYear()));
        return new RegionCatalog(List.of(info));
    }

    @GetMapping("/lunar/from-solar")
    public LunarInfo lunarFromSolar(@RequestParam String date) {
        return LunarCalendar.solarToLunar(parseDate(date, "date"));
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
                true,
                List.of(new HolidayCoverage(assets.region(), assets.holidayStartYear(),
                        assets.holidayEndYear())),
                new YearCoverage(assets.lunarStartYear(), assets.lunarEndYear()),
                new YearCoverage(assets.solarTermStartYear(), assets.solarTermEndYear()),
                new BinaryFormats(2, 1), properties.getSourceCommit());
    }

    private static CalendarQueryOptions options(String locale, String fields) {
        return CalendarQueryOptions.of(locale, fields);
    }

    private static CalendarResult collection(
            String region, CalendarQueryOptions options, LocalDate from, LocalDate to,
            List<DayInfo> source) {
        List<DayInfoView> items = source.stream()
                .map(day -> DayInfoView.of(day, options))
                .toList();
        return new CalendarResult(region, options.locale(), from, to, items.size(), items);
    }

    private static LocalDate parseDate(String value, String name) {
        if (value == null || !value.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw ApiException.badRequest(name + " 必须使用 yyyy-MM-dd");
        }
        try {
            return LocalDate.parse(value, ISO_DATE);
        } catch (DateTimeException exception) {
            throw ApiException.badRequest(name + " 不是有效日期: " + value);
        }
    }

    private static String normalizeRegion(String region) {
        if (region == null || !REGION.matcher(region).matches()) {
            throw ApiException.badRequest("不支持的地区: " + region);
        }
        return region;
    }

    private static void validatePathYear(int year) {
        if (year < 1 || year > 9999) {
            throw ApiException.badRequest("年份超出范围: " + year);
        }
    }

    private static void validateRange(LocalDate from, LocalDate to, int maximumDays) {
        if (from.isAfter(to)) throw ApiException.badRequest("起始日期不能晚于结束日期");
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days > maximumDays) {
            throw ApiException.badRequest("日期范围最多包含 " + maximumDays + " 天");
        }
    }

    private static List<Range> normalizeRanges(List<BatchCalendarRequest.RangeRequest> requests) {
        List<Range> ranges = new ArrayList<>(requests.size());
        for (BatchCalendarRequest.RangeRequest request : requests) {
            if (request == null) throw ApiException.badRequest("ranges 不能包含 null");
            LocalDate from = parseDate(request.from(), "ranges.from");
            LocalDate to = parseDate(request.to(), "ranges.to");
            if (from.isAfter(to)) throw ApiException.badRequest("批量范围起始日期不能晚于结束日期");
            ranges.add(new Range(from, to));
        }
        ranges.sort(Comparator.comparing(Range::from).thenComparing(Range::to));
        List<Range> merged = new ArrayList<>();
        for (Range current : ranges) {
            if (merged.isEmpty()) {
                merged.add(current);
                continue;
            }
            Range previous = merged.get(merged.size() - 1);
            if (ChronoUnit.DAYS.between(previous.to(), current.from()) <= 1) {
                if (current.to().isAfter(previous.to())) {
                    merged.set(merged.size() - 1, new Range(previous.from(), current.to()));
                }
            } else {
                merged.add(current);
            }
        }
        return merged;
    }

    private record Range(LocalDate from, LocalDate to) { }

    public record HolidayYearResult(int year, String region, String locale,
                                    List<HolidayPeriodView> holidays) { }
    public record RegionCatalog(List<RegionInfo> regions) { }
    public record RegionInfo(String code, String name, String timezone,
                             List<String> weekendMask, List<String> locales,
                             YearCoverage holidays, YearCoverage lunar,
                             YearCoverage solarTerms) { }
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
            boolean breakingChange,
            List<HolidayCoverage> holidays,
            YearCoverage lunar,
            YearCoverage solarTerms,
            BinaryFormats binaryFormats,
            String sourceCommit) { }
}
