namespace CnHolidayKit;

public static class SolarTermTable
{
    public const int StartYear = 1901;
    public const int EndYear = 2100;

    private static readonly SolarTermInfo[] SolarTermInfos = BuildSolarTermInfos();
    private static readonly sbyte[][] LookupTables = BuildLookupTables();

    public static SolarTermInfo? Lookup(DateOnly date)
    {
        var dayIndex = date.DayNumber - new DateOnly(date.Year, 1, 1).DayNumber;
        return Lookup(date.Year, dayIndex);
    }

    public static SolarTermInfo? Lookup(int year, int dayIndex)
    {
        if (year < StartYear || year > EndYear)
        {
            return null;
        }

        var lookupTable = LookupTables[year - StartYear];
        if (dayIndex < 0 || dayIndex >= lookupTable.Length)
        {
            return null;
        }

        var solarTermIndex = lookupTable[dayIndex];
        return solarTermIndex < 0 ? null : SolarTermInfos[solarTermIndex];
    }

    private static SolarTermInfo[] BuildSolarTermInfos()
    {
        var result = new SolarTermInfo[SolarTermData.SolarTermNames.Length];
        for (var index = 0; index < SolarTermData.SolarTermNames.Length; index++)
        {
            result[index] = new SolarTermInfo(index, SolarTermData.SolarTermNames[index]);
        }

        return result;
    }

    private static sbyte[][] BuildLookupTables()
    {
        var result = new sbyte[EndYear - StartYear + 1][];
        for (var year = StartYear; year <= EndYear; year++)
        {
            var lookupTable = new sbyte[GregorianDateHelper.IsLeapYear(year) ? 366 : 365];
            Array.Fill(lookupTable, (sbyte)-1);

            var dayIndexes = SolarTermData.SolarTermDayIndexesByYear[year - StartYear];
            for (var termIndex = 0; termIndex < dayIndexes.Length; termIndex++)
            {
                var dayIndex = dayIndexes[termIndex];
                if (dayIndex < 0 || dayIndex >= lookupTable.Length)
                {
                    throw new InvalidOperationException($"节气 dayIndex 超出范围: year={year}, dayIndex={dayIndex}, dayCount={lookupTable.Length}");
                }

                lookupTable[dayIndex] = (sbyte)termIndex;
            }

            result[year - StartYear] = lookupTable;
        }

        return result;
    }
}
