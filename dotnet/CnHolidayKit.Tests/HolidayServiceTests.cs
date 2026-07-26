namespace CnHolidayKit.Tests;

[TestClass]
public sealed class HolidayServiceTests
{
    private static readonly IHolidayService Service = new HolidayServiceBuilder()
        .WithDefaultRegion("CN")
        .WithDataDirectory(TestDataPaths.BundleRoot)
        .Build();

    [TestMethod]
    public void NewYearsDayIsHoliday()
    {
        var info = Service.GetDayInfo(new DateOnly(2025, 1, 1));
        Assert.IsNotNull(info);
        Assert.IsTrue(info.IsHoliday);
        Assert.IsTrue(info.IsStatutoryHoliday);
        Assert.IsFalse(info.IsWorkday);
    }

    [TestMethod]
    public void NewYearsDayHasNames()
    {
        var info = Service.GetDayInfo(new DateOnly(2025, 1, 1));
        Assert.IsNotNull(info);
        Assert.IsTrue(info.HolidayNames.Count > 0);
        Assert.IsTrue(info.HolidayNames.ContainsKey("en-US"));
    }

    [TestMethod]
    public void NewYearsDayHasLabels()
    {
        var info = Service.GetDayInfo(new DateOnly(2025, 1, 1));
        Assert.IsNotNull(info);
        CollectionAssert.Contains(info.Labels.ToList(), "NEW_YEAR");
    }

    [TestMethod]
    public void NewYearsDayHasLunarExtension()
    {
        var info = Service.GetDayInfo(new DateOnly(2025, 1, 1));
        Assert.IsNotNull(info);
        Assert.IsNotNull(info.LunarDate);
        Assert.AreEqual(2024, info.LunarDate.Year);
        Assert.AreEqual(12, info.LunarDate.Month);
        Assert.AreEqual(2, info.LunarDate.Day);
        Assert.AreEqual("甲辰年", info.LunarDate.GanZhiYear);
        Assert.AreEqual("龙", info.LunarDate.ShengXiao);
        Assert.AreEqual("腊月", info.LunarDate.MonthName);
        Assert.AreEqual("初二", info.LunarDate.DayName);
    }

    [TestMethod]
    public void LiChunHasSolarTermExtension()
    {
        var info = Service.GetDayInfo(new DateOnly(2025, 2, 3));
        Assert.IsNotNull(info);
        Assert.IsNotNull(info.SolarTerm);
        Assert.AreEqual(2, info.SolarTerm.Index);
        Assert.AreEqual("立春", info.SolarTerm.Name);
    }

    [TestMethod]
    public void NonSolarTermDayOmitsSolarTermExtension()
    {
        var info = Service.GetDayInfo(new DateOnly(2025, 2, 4));
        Assert.IsNotNull(info);
        Assert.IsFalse(info.Extensions.ContainsKey(DayInfo.SolarTermExtensionKey));
    }

    [TestMethod]
    public void NormalWorkday()
    {
        var info = Service.GetDayInfo(new DateOnly(2025, 1, 2));
        Assert.IsNotNull(info);
        Assert.IsTrue(info.IsWorkday);
        Assert.IsFalse(info.IsHoliday);
        Assert.IsFalse(info.IsWeekend);
    }

    [TestMethod]
    public void SaturdayIsWeekend()
    {
        var info = Service.GetDayInfo(new DateOnly(2025, 1, 4));
        Assert.IsNotNull(info);
        Assert.IsTrue(info.IsWeekend);
        Assert.IsTrue(info.IsHoliday);
        Assert.IsFalse(info.IsOfficialHoliday);
        Assert.IsFalse(info.IsWorkday);
    }

    [TestMethod]
    public void AdjustedWorkdaySpringFestivalMakeup()
    {
        var info = Service.GetDayInfo(new DateOnly(2025, 1, 26));
        Assert.IsNotNull(info);
        Assert.IsTrue(info.IsAdjustedWorkday);
        Assert.IsTrue(info.IsWorkday);
        Assert.IsTrue(info.IsWeekend);
        Assert.IsFalse(info.IsHoliday);
    }

    [TestMethod]
    public void SpringFestivalIsHoliday()
    {
        var info = Service.GetDayInfo(new DateOnly(2025, 1, 28));
        Assert.IsNotNull(info);
        Assert.IsTrue(info.IsHoliday);
        CollectionAssert.Contains(info.Labels.ToList(), "SPRING_FESTIVAL");
    }

    [TestMethod]
    public void IsHolidayConvenience()
    {
        Assert.IsTrue(Service.IsHoliday(new DateOnly(2025, 1, 1)));
        Assert.IsFalse(Service.IsHoliday(new DateOnly(2025, 1, 2)));
    }

    [TestMethod]
    public void IsWorkdayConvenience()
    {
        Assert.IsTrue(Service.IsWorkday(new DateOnly(2025, 1, 2)));
        Assert.IsFalse(Service.IsWorkday(new DateOnly(2025, 1, 1)));
    }

    [TestMethod]
    public void GetRangeReturnsCorrectCount()
    {
        var range = Service.GetRange(new DateOnly(2025, 1, 1), new DateOnly(2025, 1, 7));
        Assert.AreEqual(7, range.Count);
        Assert.AreEqual(new DateOnly(2025, 1, 1), range[0].Date);
        Assert.AreEqual(new DateOnly(2025, 1, 7), range[6].Date);
    }

    [TestMethod]
    public void GetYearReturns365Days()
    {
        var year = Service.GetYear(2025);
        Assert.AreEqual(365, year.Count);
        Assert.ThrowsException<NotSupportedException>(() => ((IList<DayInfo>)year).RemoveAt(0));
    }

    [TestMethod]
    public void RegionOverride()
    {
        var info = Service.GetDayInfo("CN", new DateOnly(2025, 1, 1));
        Assert.IsNotNull(info);
        Assert.AreEqual("CN", info.RegionCode);
    }

    [TestMethod]
    public void GetMonthReturnsJanuarySlice()
    {
        var month = Service.GetMonth(2025, 1);
        Assert.AreEqual(31, month.Count);
        Assert.AreEqual(new DateOnly(2025, 1, 1), month[0].Date);
        Assert.AreEqual(new DateOnly(2025, 1, 31), month[month.Count - 1].Date);
    }

    [TestMethod]
    public void CountWorkdaysReturnsExpectedValue()
    {
        Assert.AreEqual(4, Service.CountWorkdays(new DateOnly(2025, 1, 1), new DateOnly(2025, 1, 7)));
    }

    [TestMethod]
    public void GetNextHolidayWithinSameYear()
    {
        var nextHoliday = Service.GetNextHoliday(new DateOnly(2025, 1, 2));
        Assert.IsNotNull(nextHoliday);
        Assert.AreEqual(new DateOnly(2025, 1, 28), nextHoliday.Date);
        Assert.IsTrue(nextHoliday.IsStatutoryHoliday);
    }

    [TestMethod]
    public void GetNextHolidayAcrossYear()
    {
        var nextHoliday = Service.GetNextHoliday(new DateOnly(2025, 10, 8));
        Assert.IsNotNull(nextHoliday);
        Assert.AreEqual(new DateOnly(2026, 1, 1), nextHoliday.Date);
    }

    [TestMethod]
    public void GetCrossYearRange()
    {
        var range = Service.GetRange(new DateOnly(2025, 12, 30), new DateOnly(2026, 1, 2));
        Assert.AreEqual(4, range.Count);
        Assert.AreEqual(new DateOnly(2025, 12, 30), range[0].Date);
        Assert.AreEqual(new DateOnly(2026, 1, 2), range[range.Count - 1].Date);
    }
}
