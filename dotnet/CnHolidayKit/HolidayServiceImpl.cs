namespace CnHolidayKit;

internal sealed class HolidayServiceImpl : IHolidayService
{
    private static readonly IReadOnlyList<DayInfo> EmptyDays = Array.AsReadOnly(Array.Empty<DayInfo>());

    private readonly string _defaultRegion;
    private readonly string _dataDirectory;
    private readonly LruCache<string, HdayBundle> _cache;

    public HolidayServiceImpl(string defaultRegion, string dataDirectory, int cacheSize)
    {
        _defaultRegion = string.IsNullOrWhiteSpace(defaultRegion) ? "CN" : defaultRegion;
        _dataDirectory = dataDirectory ?? throw new ArgumentNullException(nameof(dataDirectory));
        _cache = new LruCache<string, HdayBundle>(cacheSize);
    }

    public DayInfo? GetDayInfo(DateOnly date)
    {
        return GetDayInfo(_defaultRegion, date);
    }

    public DayInfo? GetDayInfo(string regionCode, DateOnly date)
    {
        var bundle = ResolveBundle(regionCode, date.Year);
        return bundle?.GetDayInfo(date);
    }

    public bool IsHoliday(DateOnly date)
    {
        return GetDayInfo(date)?.IsHoliday ?? false;
    }

    public bool IsWorkday(DateOnly date)
    {
        return GetDayInfo(date)?.IsWorkday ?? false;
    }

    public bool IsStatutoryHoliday(DateOnly date)
    {
        return GetDayInfo(date)?.IsStatutoryHoliday ?? false;
    }

    public bool IsAdjustedWorkday(DateOnly date)
    {
        return GetDayInfo(date)?.IsAdjustedWorkday ?? false;
    }

    public IReadOnlyList<DayInfo> GetRange(DateOnly from, DateOnly to)
    {
        return GetRange(_defaultRegion, from, to);
    }

    public IReadOnlyList<DayInfo> GetRange(string regionCode, DateOnly from, DateOnly to)
    {
        if (from > to)
        {
            return EmptyDays;
        }

        var result = new List<DayInfo>(EstimateRangeCapacity(from, to));
        for (var year = from.Year; year <= to.Year; year++)
        {
            var bundle = ResolveBundle(regionCode, year);
            if (bundle is null)
            {
                continue;
            }

            var startIndex = year == from.Year
                ? GregorianDateHelper.DayOfYearIndex(from.Year, from.Month, from.Day)
                : 0;
            var endIndex = year == to.Year
                ? GregorianDateHelper.DayOfYearIndex(to.Year, to.Month, to.Day)
                : bundle.DayCount - 1;
            bundle.AppendRangeTo(result, startIndex, endIndex);
        }

        return result.Count == 0 ? EmptyDays : result.AsReadOnly();
    }

    public IReadOnlyList<DayInfo> GetYear(int year)
    {
        return GetYear(_defaultRegion, year);
    }

    public IReadOnlyList<DayInfo> GetYear(string regionCode, int year)
    {
        var bundle = ResolveBundle(regionCode, year);
        return bundle?.GetDayInfos() ?? EmptyDays;
    }

    public IReadOnlyList<DayInfo> GetMonth(int year, int month)
    {
        return GetMonth(_defaultRegion, year, month);
    }

    public IReadOnlyList<DayInfo> GetMonth(string regionCode, int year, int month)
    {
        var bundle = ResolveBundle(regionCode, year);
        if (bundle is null)
        {
            return EmptyDays;
        }

        var lastDay = new DateOnly(year, month, DateTime.DaysInMonth(year, month));
        return bundle.GetRange(
            GregorianDateHelper.DayOfYearIndex(year, month, 1),
            GregorianDateHelper.DayOfYearIndex(year, month, lastDay.Day));
    }

    public int CountWorkdays(DateOnly from, DateOnly to)
    {
        return CountWorkdays(_defaultRegion, from, to);
    }

    public int CountWorkdays(string regionCode, DateOnly from, DateOnly to)
    {
        if (from > to)
        {
            return 0;
        }

        var count = 0;
        for (var year = from.Year; year <= to.Year; year++)
        {
            var bundle = ResolveBundle(regionCode, year);
            if (bundle is null)
            {
                continue;
            }

            var startIndex = year == from.Year
                ? GregorianDateHelper.DayOfYearIndex(from.Year, from.Month, from.Day)
                : 0;
            var endIndex = year == to.Year
                ? GregorianDateHelper.DayOfYearIndex(to.Year, to.Month, to.Day)
                : bundle.DayCount - 1;
            count += bundle.CountWorkdays(startIndex, endIndex);
        }

        return count;
    }

    public DayInfo? GetNextHoliday(DateOnly from)
    {
        return GetNextHoliday(_defaultRegion, from);
    }

    public DayInfo? GetNextHoliday(string regionCode, DateOnly from)
    {
        for (var year = from.Year; year <= DateOnly.MaxValue.Year; year++)
        {
            var bundle = ResolveBundle(regionCode, year);
            if (bundle is null)
            {
                return null;
            }

            var startIndex = year == from.Year
                ? GregorianDateHelper.DayOfYearIndex(from.Year, from.Month, from.Day)
                : 0;
            var result = bundle.FindStatutoryHoliday(startIndex);
            if (result is not null)
            {
                return result;
            }
        }

        return null;
    }

    private static int EstimateRangeCapacity(DateOnly from, DateOnly to)
    {
        var dayCount = (long)to.DayNumber - from.DayNumber + 1;
        return dayCount >= int.MaxValue ? int.MaxValue : (int)dayCount;
    }

    private HdayBundle? ResolveBundle(string regionCode, int year)
    {
        var normalizedRegion = string.IsNullOrWhiteSpace(regionCode) ? _defaultRegion : regionCode;
        var cacheKey = $"{normalizedRegion}/{year}";
        return _cache.GetOrAdd(cacheKey, _ => LoadBundle(normalizedRegion, year));
    }

    private HdayBundle? LoadBundle(string regionCode, int year)
    {
        var filePath = Path.Combine(_dataDirectory, regionCode, $"{year}.hday");
        if (!File.Exists(filePath))
        {
            return null;
        }

        return HdayReader.Read(filePath);
    }
}
