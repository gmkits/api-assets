namespace CnHolidayKit;

public sealed record LunarDate(int Year, int Month, int Day, bool IsLeapMonth);

public sealed record LunarDateInfo(
    int Year,
    int Month,
    int Day,
    bool IsLeapMonth,
    string GanZhiYear,
    string ShengXiao,
    string MonthName,
    string DayName);

public sealed record LunarInfo(
    int Year,
    int Month,
    int Day,
    bool IsLeapMonth,
    string TianGan,
    string DiZhi,
    string GanZhiYear,
    string ShengXiao,
    string MonthName,
    string DayName,
    string FullName)
{
    public LunarDate Date => new(Year, Month, Day, IsLeapMonth);

    public LunarDateInfo ToDateInfo()
    {
        return new LunarDateInfo(Year, Month, Day, IsLeapMonth, GanZhiYear, ShengXiao, MonthName, DayName);
    }
}
