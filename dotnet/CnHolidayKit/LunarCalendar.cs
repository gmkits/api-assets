namespace CnHolidayKit;

public static class LunarCalendar
{
    public const int StartYear = 1900;
    public const int EndYear = 2100;

    public static readonly DateOnly MinSupportedSolarDate = new(1900, 1, 31);
    public static readonly DateOnly MaxSupportedSolarDate;

    private const int LeapMonthBigMask = 0x10000;
    private const int YearCount = EndYear - StartYear + 1;

    private static readonly int[] LunarData =
    {
        0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2,
        0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977,
        0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970,
        0x06566, 0x0d4a0, 0x0ea50, 0x16a95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950,
        0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557,
        0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5b0, 0x14573, 0x052b0, 0x0a9a8, 0x0e950, 0x06aa0,
        0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0,
        0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b6a0, 0x195a6,
        0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570,
        0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x05ac0, 0x0ab60, 0x096d5, 0x092e0,
        0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5,
        0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930,
        0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530,
        0x05aa0, 0x076a3, 0x096d0, 0x04afb, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45,
        0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0,
        0x14b63, 0x09370, 0x049f8, 0x04970, 0x064b0, 0x168a6, 0x0ea50, 0x06aa0, 0x1a6c4, 0x0aae0,
        0x092e0, 0x0d2e3, 0x0c960, 0x0d557, 0x0d4a0, 0x0da50, 0x05d55, 0x056a0, 0x0a6d0, 0x055d4,
        0x052d0, 0x0a9b8, 0x0a950, 0x0b4a0, 0x0b6a6, 0x0ad50, 0x055a0, 0x0aba4, 0x0a5b0, 0x052b0,
        0x0b273, 0x06930, 0x07337, 0x06aa0, 0x0ad50, 0x14b55, 0x04b60, 0x0a570, 0x054e4, 0x0d160,
        0x0e968, 0x0d520, 0x0daa0, 0x16aa6, 0x056d0, 0x04ae0, 0x0a9d4, 0x0a2d0, 0x0d150, 0x0f252,
        0x0d520,
    };

    private static readonly string[] TianGan = ["甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"];
    private static readonly string[] DiZhi = ["子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"];
    private static readonly string[] ShengXiao = ["鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪"];
    private static readonly string[] MonthNames = ["正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊"];
    private static readonly string[] DayNames =
    [
        "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十",
    ];

    private static readonly int[] YearDaysCache = new int[YearCount];
    private static readonly long[] CumulativeDays = new long[YearCount + 1];
    private static readonly int[][] MonthOffsets = new int[YearCount][];
    private static readonly int[][] MonthMeta = new int[YearCount][];

    static LunarCalendar()
    {
        CumulativeDays[0] = 0;
        for (var yearIndex = 0; yearIndex < YearCount; yearIndex++)
        {
            var info = LunarData[yearIndex];
            YearDaysCache[yearIndex] = ComputeYearDays(info);
            CumulativeDays[yearIndex + 1] = CumulativeDays[yearIndex] + YearDaysCache[yearIndex];

            var leapMonth = info & 0xF;
            var offsets = new int[14];
            var meta = new int[14];
            var slotCount = 0;
            var cumulative = 0;

            for (var month = 1; month <= 12; month++)
            {
                offsets[slotCount] = cumulative;
                meta[slotCount] = month;
                slotCount++;
                cumulative += (info & (LeapMonthBigMask >> month)) != 0 ? 30 : 29;

                if (month == leapMonth)
                {
                    offsets[slotCount] = cumulative;
                    meta[slotCount] = month | 0x10;
                    slotCount++;
                    cumulative += (info & LeapMonthBigMask) != 0 ? 30 : 29;
                }
            }

            offsets[slotCount] = cumulative;
            meta[slotCount] = 0;
            slotCount++;

            MonthOffsets[yearIndex] = new int[slotCount];
            MonthMeta[yearIndex] = new int[slotCount];
            Array.Copy(offsets, MonthOffsets[yearIndex], slotCount);
            Array.Copy(meta, MonthMeta[yearIndex], slotCount);
        }

        MaxSupportedSolarDate = LunarToSolar(EndYear, 12, MonthDays(EndYear, 12));
    }

    public static bool IsSolarDateSupported(DateOnly solarDate)
    {
        return solarDate >= MinSupportedSolarDate && solarDate <= MaxSupportedSolarDate;
    }

    public static int LeapMonth(int lunarYear)
    {
        ValidateYear(lunarYear);
        return LunarData[lunarYear - StartYear] & 0xF;
    }

    public static int LeapMonthDays(int lunarYear)
    {
        return LeapMonth(lunarYear) == 0
            ? 0
            : (LunarData[lunarYear - StartYear] & LeapMonthBigMask) != 0 ? 30 : 29;
    }

    public static int MonthDays(int lunarYear, int month)
    {
        ValidateYear(lunarYear);
        if (month < 1 || month > 12)
        {
            throw new ArgumentOutOfRangeException(nameof(month), $"月份超出范围: {month}，应为 1-12");
        }

        return (LunarData[lunarYear - StartYear] & (LeapMonthBigMask >> month)) != 0 ? 30 : 29;
    }

    public static int YearDays(int lunarYear)
    {
        ValidateYear(lunarYear);
        return YearDaysCache[lunarYear - StartYear];
    }

    public static LunarInfo SolarToLunar(DateOnly solarDate)
    {
        var offset = solarDate.DayNumber - MinSupportedSolarDate.DayNumber;
        if (offset < 0)
        {
            throw new ArgumentOutOfRangeException(nameof(solarDate), $"日期早于 {MinSupportedSolarDate:yyyy-MM-dd}，超出农历转换范围");
        }

        var lo = 0;
        var hi = YearCount;
        while (lo < hi)
        {
            var mid = (lo + hi + 1) >> 1;
            if (CumulativeDays[mid] <= offset)
            {
                lo = mid;
            }
            else
            {
                hi = mid - 1;
            }
        }

        var lunarYear = StartYear + lo;
        if (lunarYear > EndYear)
        {
            throw new ArgumentOutOfRangeException(nameof(solarDate), $"日期超出农历转换范围（{StartYear}-{EndYear}）");
        }

        var dayOffset = offset - CumulativeDays[lo];
        var offsets = MonthOffsets[lo];
        var meta = MonthMeta[lo];
        var slotCount = offsets.Length - 1;

        var slot = 0;
        for (var slotIndex = slotCount - 1; slotIndex >= 0; slotIndex--)
        {
            if (offsets[slotIndex] <= dayOffset)
            {
                slot = slotIndex;
                break;
            }
        }

        var monthMeta = meta[slot];
        var lunarMonth = monthMeta & 0xF;
        var isLeapMonth = (monthMeta & 0x10) != 0;
        var lunarDay = (int)(dayOffset - offsets[slot]) + 1;

        return BuildLunarInfo(lunarYear, lunarMonth, lunarDay, isLeapMonth);
    }

    public static DateOnly LunarToSolar(int lunarYear, int lunarMonth, int lunarDay, bool isLeapMonth = false)
    {
        ValidateYear(lunarYear);
        if (lunarMonth < 1 || lunarMonth > 12)
        {
            throw new ArgumentOutOfRangeException(nameof(lunarMonth), $"农历月份超出范围: {lunarMonth}");
        }

        if (lunarDay < 1 || lunarDay > 30)
        {
            throw new ArgumentOutOfRangeException(nameof(lunarDay), $"农历日期超出范围: {lunarDay}");
        }

        var yearIndex = lunarYear - StartYear;
        var meta = MonthMeta[yearIndex];
        var offsets = MonthOffsets[yearIndex];
        var slotCount = offsets.Length - 1;
        var targetMeta = (lunarMonth & 0xF) | (isLeapMonth ? 0x10 : 0);

        var slotIndex = -1;
        for (var index = 0; index < slotCount; index++)
        {
            if (meta[index] == targetMeta)
            {
                slotIndex = index;
                break;
            }
        }

        if (slotIndex < 0)
        {
            throw new ArgumentOutOfRangeException(nameof(isLeapMonth), $"农历 {lunarYear} 年不存在{(isLeapMonth ? "闰" : string.Empty)}{lunarMonth} 月");
        }

        var slotDays = offsets[slotIndex + 1] - offsets[slotIndex];
        if (lunarDay > slotDays)
        {
            throw new ArgumentOutOfRangeException(nameof(lunarDay), $"农历 {lunarYear} 年{(isLeapMonth ? "闰" : string.Empty)}{lunarMonth} 月仅有 {slotDays} 天，日期 {lunarDay} 超出范围");
        }

        var totalOffset = CumulativeDays[yearIndex] + offsets[slotIndex] + lunarDay - 1;
        return MinSupportedSolarDate.AddDays(checked((int)totalOffset));
    }

    public static string GetTianGan(int lunarYear)
    {
        return TianGan[PositiveModulo(lunarYear - 4, 10)];
    }

    public static string GetDiZhi(int lunarYear)
    {
        return DiZhi[PositiveModulo(lunarYear - 4, 12)];
    }

    public static string GetGanZhi(int lunarYear)
    {
        return GetTianGan(lunarYear) + GetDiZhi(lunarYear);
    }

    public static string GetShengXiao(int lunarYear)
    {
        return ShengXiao[PositiveModulo(lunarYear - 4, 12)];
    }

    public static string GetMonthName(int month, bool isLeapMonth)
    {
        if (month < 1 || month > 12)
        {
            throw new ArgumentOutOfRangeException(nameof(month), $"月份超出范围: {month}");
        }

        return (isLeapMonth ? "闰" : string.Empty) + MonthNames[month - 1] + "月";
    }

    public static string GetDayName(int day)
    {
        if (day < 1 || day > 30)
        {
            throw new ArgumentOutOfRangeException(nameof(day), $"日期超出范围: {day}");
        }

        return DayNames[day - 1];
    }

    public static double EstimateNewMoonJde(int k)
    {
        var t = k / 1236.85d;
        var t2 = t * t;
        var t3 = t2 * t;
        var t4 = t3 * t;

        var jde = 2451550.09766d
            + 29.530588861d * k
            + 0.00015437d * t2
            - 0.000000150d * t3
            + 0.00000000073d * t4;

        var m = DegreesToRadians(2.5534d + 29.10535670d * k - 0.0000014d * t2 - 0.00000011d * t3);
        var mp = DegreesToRadians(201.5643d + 385.81693528d * k + 0.0107582d * t2 + 0.00001238d * t3 - 0.000000058d * t4);
        var f = DegreesToRadians(160.7108d + 390.67050284d * k - 0.0016118d * t2 - 0.00000227d * t3 + 0.000000011d * t4);

        jde += -0.40720d * Math.Sin(mp)
             + 0.17241d * Math.Sin(m)
             + 0.01608d * Math.Sin(2d * mp)
             + 0.01039d * Math.Sin(2d * f)
             + 0.00739d * Math.Sin(mp - m);

        return jde;
    }

    public static DateOnly JdeToGregorian(double jde)
    {
        var z = (int)Math.Floor(jde + 0.5d);
        int a;
        if (z < 2299161)
        {
            a = z;
        }
        else
        {
            var alpha = (int)Math.Floor((z - 1867216.25d) / 36524.25d);
            a = z + 1 + alpha - alpha / 4;
        }

        var b = a + 1524;
        var c = (int)Math.Floor((b - 122.1d) / 365.25d);
        var d = (int)Math.Floor(365.25d * c);
        var e = (int)Math.Floor((b - d) / 30.6001d);
        var day = b - d - (int)Math.Floor(30.6001d * e) + (int)Math.Floor(jde + 0.5d - z);
        var month = e < 14 ? e - 1 : e - 13;
        var year = month > 2 ? c - 4716 : c - 4715;

        return new DateOnly(year, month, day);
    }

    public static DateOnly EstimateLunarNewYear(int year)
    {
        var k0 = (int)Math.Round((year - 2000) * 12.3685d);

        for (var delta = -2; delta <= 2; delta++)
        {
            var candidate = JdeToGregorian(EstimateNewMoonJde(k0 + delta));
            if (candidate.Year == year && ((candidate.Month == 1 && candidate.Day >= 21) || (candidate.Month == 2 && candidate.Day <= 20)))
            {
                return candidate;
            }
        }

        for (var delta = -4; delta <= 4; delta++)
        {
            var candidate = JdeToGregorian(EstimateNewMoonJde(k0 + delta));
            if (candidate.Year == year && ((candidate.Month == 1 && candidate.Day >= 20) || (candidate.Month == 2 && candidate.Day <= 21)))
            {
                return candidate;
            }
        }

        return JdeToGregorian(EstimateNewMoonJde(k0));
    }

    private static int ComputeYearDays(int info)
    {
        var total = 0;
        for (var month = 1; month <= 12; month++)
        {
            total += (info & (LeapMonthBigMask >> month)) != 0 ? 30 : 29;
        }

        if ((info & 0xF) != 0)
        {
            total += (info & LeapMonthBigMask) != 0 ? 30 : 29;
        }

        return total;
    }

    private static double DegreesToRadians(double degrees)
    {
        return degrees * Math.PI / 180d;
    }

    private static int PositiveModulo(int value, int divisor)
    {
        var remainder = value % divisor;
        return remainder < 0 ? remainder + divisor : remainder;
    }

    private static void ValidateYear(int year)
    {
        if (year < StartYear || year > EndYear)
        {
            throw new ArgumentOutOfRangeException(nameof(year), $"年份 {year} 超出范围，农历数据覆盖 {StartYear}-{EndYear}");
        }
    }

    private static LunarInfo BuildLunarInfo(int year, int month, int day, bool isLeapMonth)
    {
        var tianGan = GetTianGan(year);
        var diZhi = GetDiZhi(year);
        var ganZhiYear = tianGan + diZhi + "年";
        var shengXiao = GetShengXiao(year);
        var monthName = GetMonthName(month, isLeapMonth);
        var dayName = GetDayName(day);
        var fullName = $"{ganZhiYear} {monthName}{dayName}";
        return new LunarInfo(year, month, day, isLeapMonth, tianGan, diZhi, ganZhiYear, shengXiao, monthName, dayName, fullName);
    }
}
