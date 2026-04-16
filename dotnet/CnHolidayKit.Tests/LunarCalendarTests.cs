namespace CnHolidayKit.Tests;

[TestClass]
public sealed class LunarCalendarTests
{
    [TestMethod]
    public void YearRange()
    {
        Assert.AreEqual(1900, LunarCalendar.StartYear);
        Assert.AreEqual(2100, LunarCalendar.EndYear);
    }

    [TestMethod]
    public void EveryYearAndMonthValid()
    {
        for (var year = LunarCalendar.StartYear; year <= LunarCalendar.EndYear; year++)
        {
            var yearDays = LunarCalendar.YearDays(year);
            Assert.IsTrue(yearDays >= 353 && yearDays <= 385, $"{year}年 {yearDays}天");
            for (var month = 1; month <= 12; month++)
            {
                var monthDays = LunarCalendar.MonthDays(year, month);
                Assert.IsTrue(monthDays is 29 or 30, $"{year}-{month} {monthDays}天");
            }
        }
    }

    [TestMethod]
    public void LeapMonthConsistency()
    {
        for (var year = LunarCalendar.StartYear; year <= LunarCalendar.EndYear; year++)
        {
            var leapMonth = LunarCalendar.LeapMonth(year);
            Assert.IsTrue(leapMonth >= 0 && leapMonth <= 12, $"{year}年闰月={leapMonth}");
            var leapDays = LunarCalendar.LeapMonthDays(year);
            if (leapMonth == 0)
            {
                Assert.AreEqual(0, leapDays, $"{year}年无闰月但天数={leapDays}");
            }
            else
            {
                Assert.IsTrue(leapDays is 29 or 30, $"{year}年闰月天数={leapDays}");
            }
        }
    }

    [TestMethod]
    public void KnownLeapMonths()
    {
        Assert.AreEqual(6, LunarCalendar.LeapMonth(2025));
        Assert.AreEqual(2, LunarCalendar.LeapMonth(2023));
        Assert.AreEqual(0, LunarCalendar.LeapMonth(2024));
    }

    [TestMethod]
    public void LeapMonthConversion()
    {
        var leapMonthSolar = LunarCalendar.LunarToSolar(2025, 6, 1, isLeapMonth: true);
        var normalMonthSolar = LunarCalendar.LunarToSolar(2025, 6, 1, isLeapMonth: false);
        Assert.AreNotEqual(leapMonthSolar, normalMonthSolar);

        var leapRoundTrip = LunarCalendar.SolarToLunar(leapMonthSolar);
        Assert.AreEqual(6, leapRoundTrip.Month);
        Assert.IsTrue(leapRoundTrip.IsLeapMonth);

        var normalRoundTrip = LunarCalendar.SolarToLunar(normalMonthSolar);
        Assert.AreEqual(6, normalRoundTrip.Month);
        Assert.IsFalse(normalRoundTrip.IsLeapMonth);
    }

    [TestMethod]
    public void LeapMonth2023RoundTrip()
    {
        var solarDate = LunarCalendar.LunarToSolar(2023, 2, 15, isLeapMonth: true);
        var roundTrip = LunarCalendar.SolarToLunar(solarDate);
        Assert.AreEqual(2, roundTrip.Month);
        Assert.AreEqual(15, roundTrip.Day);
        Assert.IsTrue(roundTrip.IsLeapMonth);
    }

    [TestMethod]
    public void NoLeapMonthWithLeapFlagThrows()
    {
        Assert.ThrowsException<ArgumentOutOfRangeException>(() => LunarCalendar.LunarToSolar(2024, 6, 1, true));
        Assert.ThrowsException<ArgumentOutOfRangeException>(() => LunarCalendar.LunarToSolar(2024, 1, 1, true));
    }

    [TestMethod]
    public void WrongLeapMonthThrows()
    {
        Assert.ThrowsException<ArgumentOutOfRangeException>(() => LunarCalendar.LunarToSolar(2025, 3, 1, true));
    }

    [TestMethod]
    public void SmallMonthDay30Throws()
    {
        for (var month = 1; month <= 12; month++)
        {
            if (LunarCalendar.MonthDays(2025, month) == 29)
            {
                Assert.ThrowsException<ArgumentOutOfRangeException>(() => LunarCalendar.LunarToSolar(2025, month, 30));
                return;
            }
        }

        Assert.Fail("未找到 29 天月");
    }

    [TestMethod]
    public void OutOfRangeThrows()
    {
        Assert.ThrowsException<ArgumentOutOfRangeException>(() => LunarCalendar.YearDays(1899));
        Assert.ThrowsException<ArgumentOutOfRangeException>(() => LunarCalendar.YearDays(2101));
        Assert.ThrowsException<ArgumentOutOfRangeException>(() => LunarCalendar.SolarToLunar(new DateOnly(1899, 1, 1)));
        Assert.ThrowsException<ArgumentOutOfRangeException>(() => LunarCalendar.LunarToSolar(1899, 1, 1));
    }

    [TestMethod]
    public void InvalidMonthOrDayThrows()
    {
        Assert.ThrowsException<ArgumentOutOfRangeException>(() => LunarCalendar.MonthDays(2025, 0));
        Assert.ThrowsException<ArgumentOutOfRangeException>(() => LunarCalendar.MonthDays(2025, 13));
        Assert.ThrowsException<ArgumentOutOfRangeException>(() => LunarCalendar.LunarToSolar(2025, 0, 1));
        Assert.ThrowsException<ArgumentOutOfRangeException>(() => LunarCalendar.LunarToSolar(2025, 1, 0));
        Assert.ThrowsException<ArgumentOutOfRangeException>(() => LunarCalendar.LunarToSolar(2025, 1, 31));
    }

    [TestMethod]
    public void KnownDates()
    {
        var cases = new[]
        {
            new { Solar = new DateOnly(2025, 1, 29), LunarYear = 2025, LunarMonth = 1, LunarDay = 1, IsLeap = false, GanZhi = "乙巳年", ShengXiao = "蛇" },
            new { Solar = new DateOnly(2024, 2, 10), LunarYear = 2024, LunarMonth = 1, LunarDay = 1, IsLeap = false, GanZhi = "甲辰年", ShengXiao = "龙" },
            new { Solar = new DateOnly(2023, 1, 22), LunarYear = 2023, LunarMonth = 1, LunarDay = 1, IsLeap = false, GanZhi = "癸卯年", ShengXiao = "兔" },
            new { Solar = new DateOnly(1900, 1, 31), LunarYear = 1900, LunarMonth = 1, LunarDay = 1, IsLeap = false, GanZhi = "庚子年", ShengXiao = "鼠" },
            new { Solar = new DateOnly(2025, 10, 6), LunarYear = 2025, LunarMonth = 8, LunarDay = 15, IsLeap = false, GanZhi = "乙巳年", ShengXiao = "蛇" },
        };

        foreach (var item in cases)
        {
            var info = LunarCalendar.SolarToLunar(item.Solar);
            Assert.AreEqual(item.LunarYear, info.Year, item.Solar.ToString("yyyy-MM-dd"));
            Assert.AreEqual(item.LunarMonth, info.Month, item.Solar.ToString("yyyy-MM-dd"));
            Assert.AreEqual(item.LunarDay, info.Day, item.Solar.ToString("yyyy-MM-dd"));
            Assert.AreEqual(item.IsLeap, info.IsLeapMonth, item.Solar.ToString("yyyy-MM-dd"));
            Assert.AreEqual(item.GanZhi, info.GanZhiYear, item.Solar.ToString("yyyy-MM-dd"));
            Assert.AreEqual(item.ShengXiao, info.ShengXiao, item.Solar.ToString("yyyy-MM-dd"));
        }
    }

    [TestMethod]
    public void BaseDate()
    {
        var info = LunarCalendar.SolarToLunar(new DateOnly(1900, 1, 31));
        Assert.AreEqual(1900, info.Year);
        Assert.AreEqual(1, info.Month);
        Assert.AreEqual(1, info.Day);
    }

    [TestMethod]
    public void NearEndOf2100()
    {
        var info = LunarCalendar.SolarToLunar(new DateOnly(2101, 1, 28));
        Assert.AreEqual(2100, info.Year);
    }

    [TestMethod]
    public void CsvSolarToLunar()
    {
        var checkedRows = 0;
        foreach (var row in TestDataPaths.LunarGoldenRows)
        {
            var info = LunarCalendar.SolarToLunar(row.SolarDate);
            Assert.AreEqual(row.LunarYear, info.Year, $"{row.SolarDate:yyyy-MM-dd} year");
            Assert.AreEqual(row.LunarMonth, info.Month, $"{row.SolarDate:yyyy-MM-dd} month");
            Assert.AreEqual(row.LunarDay, info.Day, $"{row.SolarDate:yyyy-MM-dd} day");
            Assert.AreEqual(row.IsLeapMonth, info.IsLeapMonth, $"{row.SolarDate:yyyy-MM-dd} leap");
            checkedRows++;
        }

        Assert.IsTrue(checkedRows > 73000, $"仅验证了 {checkedRows} 行");
    }

    [TestMethod]
    public void CsvLunarToSolar()
    {
        var checkedRows = 0;
        foreach (var row in TestDataPaths.LunarGoldenRows)
        {
            var solarDate = LunarCalendar.LunarToSolar(row.LunarYear, row.LunarMonth, row.LunarDay, row.IsLeapMonth);
            Assert.AreEqual(row.SolarDate, solarDate, $"lunar({row.LunarYear},{row.LunarMonth},{row.LunarDay},{row.IsLeapMonth})");
            checkedRows++;
        }

        Assert.IsTrue(checkedRows > 73000, $"仅验证了 {checkedRows} 行");
    }

    [TestMethod]
    public void GanZhiCycle()
    {
        Assert.AreEqual("甲子", LunarCalendar.GetGanZhi(1984));
        Assert.AreEqual("鼠", LunarCalendar.GetShengXiao(1984));
        Assert.AreEqual("甲子", LunarCalendar.GetGanZhi(2044));
        Assert.AreEqual("甲子", LunarCalendar.GetGanZhi(2104));
    }

    [TestMethod]
    public void GanZhi2025()
    {
        Assert.AreEqual("乙", LunarCalendar.GetTianGan(2025));
        Assert.AreEqual("巳", LunarCalendar.GetDiZhi(2025));
        Assert.AreEqual("乙巳", LunarCalendar.GetGanZhi(2025));
        Assert.AreEqual("蛇", LunarCalendar.GetShengXiao(2025));
    }

    [TestMethod]
    public void Names()
    {
        Assert.AreEqual("正月", LunarCalendar.GetMonthName(1, false));
        Assert.AreEqual("腊月", LunarCalendar.GetMonthName(12, false));
        Assert.AreEqual("闰四月", LunarCalendar.GetMonthName(4, true));
        Assert.AreEqual("初一", LunarCalendar.GetDayName(1));
        Assert.AreEqual("十五", LunarCalendar.GetDayName(15));
        Assert.AreEqual("三十", LunarCalendar.GetDayName(30));
    }

    [TestMethod]
    public void NewMoonJde()
    {
        var jde = LunarCalendar.EstimateNewMoonJde(0);
        Assert.IsTrue(jde > 2451549 && jde < 2451552);
    }

    [TestMethod]
    public void JdeToGregorianKnown()
    {
        Assert.AreEqual(new DateOnly(2000, 1, 1), LunarCalendar.JdeToGregorian(2451545.0));
    }

    [TestMethod]
    public void SpringFestivalEstimate()
    {
        var known = new[]
        {
            new DateOnly(2020, 1, 25), new DateOnly(2021, 2, 12), new DateOnly(2022, 2, 1),
            new DateOnly(2023, 1, 22), new DateOnly(2024, 2, 10), new DateOnly(2025, 1, 29),
            new DateOnly(2026, 2, 17), new DateOnly(2027, 2, 6), new DateOnly(2028, 1, 26),
            new DateOnly(2029, 2, 13), new DateOnly(2030, 2, 3),
        };

        foreach (var solarDate in known)
        {
            var estimate = LunarCalendar.EstimateLunarNewYear(solarDate.Year);
            var diff = Math.Abs(estimate.DayNumber - solarDate.DayNumber);
            Assert.IsTrue(diff <= 2, $"{solarDate.Year}年偏差 {diff} 天");
        }
    }

    [TestMethod]
    public void AdjacentNewMoonInterval()
    {
        for (var k = -100; k < 100; k++)
        {
            var interval = LunarCalendar.EstimateNewMoonJde(k + 1) - LunarCalendar.EstimateNewMoonJde(k);
            Assert.IsTrue(interval >= 29.2 && interval <= 29.9, $"k={k} 间隔 {interval}");
        }
    }
}
