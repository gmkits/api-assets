using System.Collections.ObjectModel;
using System.Linq;

namespace CnHolidayKit;

public sealed class DayInfo
{
    public const string LunarExtensionKey = "lunar";
    public const string SolarTermExtensionKey = "solarTerm";

    private static readonly IReadOnlyDictionary<string, IReadOnlyList<string>> EmptyHolidayNames =
        new ReadOnlyDictionary<string, IReadOnlyList<string>>(new Dictionary<string, IReadOnlyList<string>>(StringComparer.Ordinal));

    private static readonly IReadOnlyList<string> EmptyLabels = Array.AsReadOnly(Array.Empty<string>());

    private static readonly IReadOnlyDictionary<string, object> EmptyExtensions =
        new ReadOnlyDictionary<string, object>(new Dictionary<string, object>(StringComparer.Ordinal));

    public DayInfo(
        DateOnly date,
        string regionCode,
        CalendarSystem calendarSystem,
        bool isHoliday,
        bool isWorkday,
        bool isWeekend,
        bool isStatutoryHoliday,
        bool isAdjustedWorkday,
        IReadOnlyDictionary<string, IReadOnlyList<string>>? holidayNames = null,
        IReadOnlyList<string>? labels = null,
        string? sourceVersion = null,
        IReadOnlyDictionary<string, object>? extensions = null)
    {
        Date = date;
        RegionCode = regionCode ?? throw new ArgumentNullException(nameof(regionCode));
        CalendarSystem = calendarSystem;
        IsHoliday = isHoliday;
        IsWorkday = isWorkday;
        IsWeekend = isWeekend;
        IsStatutoryHoliday = isStatutoryHoliday;
        IsAdjustedWorkday = isAdjustedWorkday;
        HolidayNames = FreezeHolidayNames(holidayNames);
        Labels = FreezeList(labels);
        SourceVersion = sourceVersion ?? string.Empty;
        Extensions = FreezeExtensions(extensions);
    }

    public DateOnly Date { get; }

    public string RegionCode { get; }

    public CalendarSystem CalendarSystem { get; }

    public bool IsHoliday { get; }

    public bool IsOfficialHoliday => IsHoliday && HolidayNames.Count > 0;

    public bool IsWorkday { get; }

    public bool IsWeekend { get; }

    public bool IsStatutoryHoliday { get; }

    public bool IsAdjustedWorkday { get; }

    public IReadOnlyDictionary<string, IReadOnlyList<string>> HolidayNames { get; }

    public IReadOnlyList<string> Labels { get; }

    public string SourceVersion { get; }

    public IReadOnlyDictionary<string, object> Extensions { get; }

    public LunarDateInfo? LunarDate => GetExtension<LunarDateInfo>(LunarExtensionKey);

    public SolarTermInfo? SolarTerm => GetExtension<SolarTermInfo>(SolarTermExtensionKey);

    public T? GetExtension<T>(string key) where T : class
    {
        if (!Extensions.TryGetValue(key, out var value))
        {
            return null;
        }

        return value as T;
    }

    public override string ToString()
    {
        return $"{Date:yyyy-MM-dd} [{RegionCode}] Holiday={IsHoliday}, Workday={IsWorkday}";
    }

    private static IReadOnlyDictionary<string, IReadOnlyList<string>> FreezeHolidayNames(
        IReadOnlyDictionary<string, IReadOnlyList<string>>? holidayNames)
    {
        if (holidayNames is null || holidayNames.Count == 0)
        {
            return EmptyHolidayNames;
        }

        var copy = new Dictionary<string, IReadOnlyList<string>>(holidayNames.Count, StringComparer.Ordinal);
        foreach (var pair in holidayNames)
        {
            copy[pair.Key] = FreezeList(pair.Value);
        }

        return new ReadOnlyDictionary<string, IReadOnlyList<string>>(copy);
    }

    private static IReadOnlyList<string> FreezeList(IEnumerable<string>? values)
    {
        if (values is null)
        {
            return EmptyLabels;
        }

        var materialized = values as string[] ?? values.ToArray();
        if (materialized.Length == 0)
        {
            return EmptyLabels;
        }

        return Array.AsReadOnly(materialized.ToArray());
    }

    private static IReadOnlyDictionary<string, object> FreezeExtensions(
        IReadOnlyDictionary<string, object>? extensions)
    {
        if (extensions is null || extensions.Count == 0)
        {
            return EmptyExtensions;
        }

        return new ReadOnlyDictionary<string, object>(new Dictionary<string, object>(extensions, StringComparer.Ordinal));
    }
}
