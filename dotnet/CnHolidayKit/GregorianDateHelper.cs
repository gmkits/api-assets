namespace CnHolidayKit;

internal static class GregorianDateHelper
{
    private static readonly int[] MonthOffsets = [0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334];
    private static readonly int[] LeapMonthOffsets = [0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335];
    private static readonly (int Month, int Day)[] NonLeapMonthDayTable = BuildMonthDayTable(leap: false);
    private static readonly (int Month, int Day)[] LeapMonthDayTable = BuildMonthDayTable(leap: true);

    public static bool IsLeapYear(int year)
    {
        return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
    }

    public static int DayOfYearIndex(int year, int month, int day)
    {
        var offsets = IsLeapYear(year) ? LeapMonthOffsets : MonthOffsets;
        return offsets[month - 1] + day - 1;
    }

    public static (int Month, int Day) MonthDayFromIndex(int year, int dayIndex)
    {
        var table = IsLeapYear(year) ? LeapMonthDayTable : NonLeapMonthDayTable;
        if (dayIndex < 0 || dayIndex >= table.Length)
        {
            throw new ArgumentOutOfRangeException(nameof(dayIndex), $"dayIndex 超出范围: {dayIndex}");
        }

        return table[dayIndex];
    }

    private static (int Month, int Day)[] BuildMonthDayTable(bool leap)
    {
        var monthLengths = leap
            ? new[] { 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 }
            : new[] { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

        var table = new (int Month, int Day)[leap ? 366 : 365];
        var index = 0;
        for (var month = 1; month <= 12; month++)
        {
            for (var day = 1; day <= monthLengths[month - 1]; day++)
            {
                table[index++] = (month, day);
            }
        }

        return table;
    }
}
