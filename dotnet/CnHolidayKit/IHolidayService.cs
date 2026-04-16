namespace CnHolidayKit;

public interface IHolidayService
{
    DayInfo? GetDayInfo(DateOnly date);

    DayInfo? GetDayInfo(string regionCode, DateOnly date);

    bool IsHoliday(DateOnly date);

    bool IsWorkday(DateOnly date);

    bool IsStatutoryHoliday(DateOnly date);

    bool IsAdjustedWorkday(DateOnly date);

    IReadOnlyList<DayInfo> GetRange(DateOnly from, DateOnly to);

    IReadOnlyList<DayInfo> GetRange(string regionCode, DateOnly from, DateOnly to);

    IReadOnlyList<DayInfo> GetYear(int year);

    IReadOnlyList<DayInfo> GetYear(string regionCode, int year);

    IReadOnlyList<DayInfo> GetMonth(int year, int month);

    IReadOnlyList<DayInfo> GetMonth(string regionCode, int year, int month);

    int CountWorkdays(DateOnly from, DateOnly to);

    int CountWorkdays(string regionCode, DateOnly from, DateOnly to);

    DayInfo? GetNextHoliday(DateOnly from);

    DayInfo? GetNextHoliday(string regionCode, DateOnly from);
}
