using System.Collections.ObjectModel;

namespace CnHolidayKit;

public sealed class HdayBundle
{
    public const ushort NoIndex = 0xFFFF;

    public sealed record BundleHeader(
        string Magic,
        int MajorVersion,
        int MinorVersion,
        int Flags,
        int Year,
        string RegionCode,
        CalendarSystem CalendarSystem,
        int DayCount,
        int SectionCount);

    public readonly record struct DayEntry(ushort Flags, ushort NameListIndex, ushort LabelListIndex, ushort ExtIndex)
    {
        public const ushort FlagIsHoliday = 0x0001;
        public const ushort FlagIsWorkday = 0x0002;
        public const ushort FlagIsWeekend = 0x0004;
        public const ushort FlagIsStatutoryHoliday = 0x0008;
        public const ushort FlagIsAdjustedWorkday = 0x0010;
        public const ushort FlagHasName = 0x0020;
        public const ushort FlagHasLabel = 0x0040;

        public bool IsHoliday => (Flags & FlagIsHoliday) != 0;
        public bool IsWorkday => (Flags & FlagIsWorkday) != 0;
        public bool IsWeekend => (Flags & FlagIsWeekend) != 0;
        public bool IsStatutoryHoliday => (Flags & FlagIsStatutoryHoliday) != 0;
        public bool IsAdjustedWorkday => (Flags & FlagIsAdjustedWorkday) != 0;
    }

    public readonly record struct NameListPair(ushort KeyIndex, ushort ValueIndex);

    public sealed record NameListEntry(IReadOnlyList<NameListPair> Pairs);

    private static readonly IReadOnlyDictionary<string, IReadOnlyList<string>> EmptyHolidayNames =
        new ReadOnlyDictionary<string, IReadOnlyList<string>>(new Dictionary<string, IReadOnlyList<string>>(StringComparer.Ordinal));

    private static readonly IReadOnlyList<string> EmptyLabels = Array.AsReadOnly(Array.Empty<string>());

    private static readonly IReadOnlyDictionary<string, object> EmptyExtensions =
        new ReadOnlyDictionary<string, object>(new Dictionary<string, object>(StringComparer.Ordinal));

    private readonly DayEntry[] _dayEntries;
    private readonly string[] _strings;
    private readonly NameListEntry[] _nameLists;
    private readonly DayInfo[] _dayInfos;
    private readonly ReadOnlyCollection<DayInfo> _yearView;
    private readonly int[] _workdayPrefix;
    private readonly int[] _nextStatutoryIndex;

    public HdayBundle(
        BundleHeader header,
        IReadOnlyList<DayEntry> dayEntries,
        IReadOnlyList<string> strings,
        IReadOnlyList<NameListEntry> nameLists)
    {
        Header = header ?? throw new ArgumentNullException(nameof(header));
        _dayEntries = dayEntries as DayEntry[] ?? dayEntries.ToArray();
        _strings = strings as string[] ?? strings.ToArray();
        _nameLists = nameLists as NameListEntry[] ?? nameLists.ToArray();

        if (_dayEntries.Length != Header.DayCount)
        {
            throw new ArgumentException($"dayEntries 长度 {_dayEntries.Length} 与 Header.DayCount {Header.DayCount} 不一致", nameof(dayEntries));
        }

        Days = Array.AsReadOnly(_dayEntries);
        Strings = Array.AsReadOnly(_strings);
        NameLists = Array.AsReadOnly(_nameLists);
        _dayInfos = BuildDayInfos();
        _yearView = Array.AsReadOnly(_dayInfos);
        _workdayPrefix = BuildWorkdayPrefix();
        _nextStatutoryIndex = BuildNextStatutoryIndex();
    }

    public BundleHeader Header { get; }

    public int Year => Header.Year;

    public string RegionCode => Header.RegionCode;

    public int DayCount => Header.DayCount;

    public IReadOnlyList<DayEntry> Days { get; }

    public IReadOnlyList<string> Strings { get; }

    public IReadOnlyList<NameListEntry> NameLists { get; }

    public DayInfo GetDayInfo(int dayIndex)
    {
        if (dayIndex < 0 || dayIndex >= _dayInfos.Length)
        {
            throw new ArgumentOutOfRangeException(nameof(dayIndex), $"dayIndex={dayIndex}, dayCount={_dayInfos.Length}");
        }

        return _dayInfos[dayIndex];
    }

    public DayInfo? GetDayInfo(DateOnly date)
    {
        if (date.Year != Year)
        {
            return null;
        }

        var dayIndex = GregorianDateHelper.DayOfYearIndex(date.Year, date.Month, date.Day);
        if (dayIndex < 0 || dayIndex >= _dayInfos.Length)
        {
            return null;
        }

        return _dayInfos[dayIndex];
    }

    public IReadOnlyList<DayInfo> GetDayInfos()
    {
        return _yearView;
    }

    public IReadOnlyList<DayInfo> GetRange(int startDayIndex, int endDayIndex)
    {
        if (startDayIndex > endDayIndex)
        {
            return Array.AsReadOnly(Array.Empty<DayInfo>());
        }

        var start = Math.Max(0, startDayIndex);
        var end = Math.Min(_dayInfos.Length - 1, endDayIndex);
        if (start > end)
        {
            return Array.AsReadOnly(Array.Empty<DayInfo>());
        }

        if (start == 0 && end == _dayInfos.Length - 1)
        {
            return _yearView;
        }

        var slice = new DayInfo[end - start + 1];
        Array.Copy(_dayInfos, start, slice, 0, slice.Length);
        return Array.AsReadOnly(slice);
    }

    internal void AppendRangeTo(ICollection<DayInfo> target, int startDayIndex, int endDayIndex)
    {
        var start = Math.Max(0, startDayIndex);
        var end = Math.Min(_dayInfos.Length - 1, endDayIndex);
        if (start > end)
        {
            return;
        }

        for (var index = start; index <= end; index++)
        {
            target.Add(_dayInfos[index]);
        }
    }

    internal int CountWorkdays(int startDayIndex, int endDayIndex)
    {
        var start = Math.Max(0, startDayIndex);
        var end = Math.Min(_dayInfos.Length - 1, endDayIndex);
        if (start > end)
        {
            return 0;
        }

        return _workdayPrefix[end + 1] - _workdayPrefix[start];
    }

    internal DayInfo? FindStatutoryHoliday(int startDayIndex)
    {
        var start = Math.Max(0, startDayIndex);
        if (start >= _dayInfos.Length)
        {
            return null;
        }

        var index = _nextStatutoryIndex[start];
        return index < 0 ? null : _dayInfos[index];
    }

    private int[] BuildWorkdayPrefix()
    {
        var prefix = new int[_dayInfos.Length + 1];
        for (var index = 0; index < _dayInfos.Length; index++)
        {
            prefix[index + 1] = prefix[index] + (_dayInfos[index].IsWorkday ? 1 : 0);
        }

        return prefix;
    }

    private int[] BuildNextStatutoryIndex()
    {
        var result = new int[_dayInfos.Length];
        var next = -1;
        for (var index = _dayInfos.Length - 1; index >= 0; index--)
        {
            if (_dayInfos[index].IsStatutoryHoliday)
            {
                next = index;
            }

            result[index] = next;
        }

        return result;
    }

    private DayInfo[] BuildDayInfos()
    {
        var result = new DayInfo[_dayEntries.Length];
        var cursor = new DateOnly(Year, 1, 1);
        for (var dayIndex = 0; dayIndex < _dayEntries.Length; dayIndex++)
        {
            var entry = _dayEntries[dayIndex];
            result[dayIndex] = new DayInfo(
                cursor,
                RegionCode,
                Header.CalendarSystem,
                entry.IsHoliday,
                entry.IsWorkday,
                entry.IsWeekend,
                entry.IsStatutoryHoliday,
                entry.IsAdjustedWorkday,
                ResolveNames(entry.NameListIndex),
                ResolveLabels(entry.LabelListIndex),
                string.Empty,
                ResolveExtensions(cursor, dayIndex));
            cursor = cursor.AddDays(1);
        }

        return result;
    }

    private IReadOnlyDictionary<string, object> ResolveExtensions(DateOnly date, int dayIndex)
    {
        Dictionary<string, object>? extensions = null;

        if (LunarCalendar.IsSolarDateSupported(date))
        {
            extensions ??= new Dictionary<string, object>(2, StringComparer.Ordinal);
            extensions[DayInfo.LunarExtensionKey] = LunarCalendar.SolarToLunar(date).ToDateInfo();
        }

        var solarTerm = SolarTermTable.Lookup(date.Year, dayIndex);
        if (solarTerm is not null)
        {
            extensions ??= new Dictionary<string, object>(2, StringComparer.Ordinal);
            extensions[DayInfo.SolarTermExtensionKey] = solarTerm;
        }

        return extensions is null
            ? EmptyExtensions
            : new ReadOnlyDictionary<string, object>(extensions);
    }

    private IReadOnlyDictionary<string, IReadOnlyList<string>> ResolveNames(ushort listIndex)
    {
        if (listIndex == NoIndex || listIndex >= _nameLists.Length)
        {
            return EmptyHolidayNames;
        }

        var names = new Dictionary<string, List<string>>(StringComparer.Ordinal);
        foreach (var pair in _nameLists[listIndex].Pairs)
        {
            if (pair.KeyIndex == NoIndex || pair.ValueIndex == NoIndex)
            {
                continue;
            }

            if (pair.KeyIndex >= _strings.Length || pair.ValueIndex >= _strings.Length)
            {
                continue;
            }

            var locale = _strings[pair.KeyIndex];
            var name = _strings[pair.ValueIndex];
            if (!names.TryGetValue(locale, out var values))
            {
                values = new List<string>();
                names[locale] = values;
            }

            values.Add(name);
        }

        if (names.Count == 0)
        {
            return EmptyHolidayNames;
        }

        var frozen = new Dictionary<string, IReadOnlyList<string>>(names.Count, StringComparer.Ordinal);
        foreach (var pair in names)
        {
            frozen[pair.Key] = pair.Value.AsReadOnly();
        }

        return new ReadOnlyDictionary<string, IReadOnlyList<string>>(frozen);
    }

    private IReadOnlyList<string> ResolveLabels(ushort listIndex)
    {
        if (listIndex == NoIndex || listIndex >= _nameLists.Length)
        {
            return EmptyLabels;
        }

        var labels = new List<string>();
        foreach (var pair in _nameLists[listIndex].Pairs)
        {
            if (pair.KeyIndex != NoIndex || pair.ValueIndex == NoIndex || pair.ValueIndex >= _strings.Length)
            {
                continue;
            }

            labels.Add(_strings[pair.ValueIndex]);
        }

        return labels.Count == 0 ? EmptyLabels : labels.AsReadOnly();
    }
}
