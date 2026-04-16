namespace CnHolidayKit.Tests;

[TestClass]
public sealed class HdayReaderTests
{
    [TestMethod]
    public void ReadBundle2025ShouldParseCorrectly()
    {
        var bundle = HdayReader.Read(TestDataPaths.Bundle2025);
        Assert.AreEqual(2025, bundle.Year);
        Assert.AreEqual("CN", bundle.RegionCode);
        Assert.AreEqual(365, bundle.DayCount);
        Assert.AreEqual(1, bundle.Header.MajorVersion);
    }

    [TestMethod]
    public void ReadBundle2026ShouldParseCorrectly()
    {
        var bundle = HdayReader.Read(TestDataPaths.Bundle2026);
        Assert.AreEqual(2026, bundle.Year);
        Assert.AreEqual("CN", bundle.RegionCode);
        Assert.AreEqual(365, bundle.DayCount);
    }

    [TestMethod]
    public void ReadBundleTooSmallShouldThrow()
    {
        Assert.ThrowsException<InvalidDataException>(() => HdayReader.Parse(new byte[16]));
    }

    [TestMethod]
    public void ReadBundleWrongMagicShouldThrow()
    {
        var data = new byte[64];
        data[0] = (byte)'B';
        data[1] = (byte)'A';
        data[2] = (byte)'D';
        data[3] = (byte)'!';
        Assert.ThrowsException<InvalidDataException>(() => HdayReader.Parse(data));
    }

    [TestMethod]
    public void ReadBundleDayInfosArePrebuilt()
    {
        var bundle = HdayReader.Read(TestDataPaths.Bundle2025);
        Assert.AreEqual(365, bundle.GetDayInfos().Count);
        Assert.IsNotNull(bundle.GetDayInfo(0));
    }

    [TestMethod]
    public void ReadBundleDayInfoDateIsCorrect()
    {
        var bundle = HdayReader.Read(TestDataPaths.Bundle2025);
        Assert.AreEqual(new DateOnly(2025, 1, 1), bundle.GetDayInfo(0).Date);
        Assert.AreEqual(new DateOnly(2025, 12, 31), bundle.GetDayInfo(364).Date);
    }

    [TestMethod]
    public void ReadBundleRangeQuery()
    {
        var bundle = HdayReader.Read(TestDataPaths.Bundle2025);
        var range = bundle.GetRange(0, 6);
        Assert.AreEqual(7, range.Count);
        Assert.AreEqual(new DateOnly(2025, 1, 1), range[0].Date);
        Assert.AreEqual(new DateOnly(2025, 1, 7), range[6].Date);
        Assert.ThrowsException<NotSupportedException>(() => ((IList<DayInfo>)range).Add(bundle.GetDayInfo(0)));
    }

    [TestMethod]
    public void ReadBundleRangeQueryReversed()
    {
        var bundle = HdayReader.Read(TestDataPaths.Bundle2025);
        Assert.AreEqual(0, bundle.GetRange(10, 5).Count);
    }

    [TestMethod]
    public void ReadBundleDayInfoIncludesSolarTermExtension()
    {
        var bundle = HdayReader.Read(TestDataPaths.Bundle2025);
        var info = bundle.GetDayInfo(new DateOnly(2025, 4, 4));
        Assert.IsNotNull(info);
        Assert.IsNotNull(info.SolarTerm);
        Assert.AreEqual(6, info.SolarTerm.Index);
        Assert.AreEqual("清明", info.SolarTerm.Name);
    }

    [TestMethod]
    public void ReadBundleOutOfBoundsShouldThrow()
    {
        var bundle = HdayReader.Read(TestDataPaths.Bundle2025);
        Assert.ThrowsException<ArgumentOutOfRangeException>(() => bundle.GetDayInfo(365));
        Assert.ThrowsException<ArgumentOutOfRangeException>(() => bundle.GetDayInfo(-1));
    }

    [TestMethod]
    public void BuildDayInfoOutsideLunarRangeShouldOmitLunarExtension()
    {
        var bundle = new HdayBundle(
            new HdayBundle.BundleHeader("HDAY", 1, 0, 0, 1900, "CN", CalendarSystem.Gregorian, 1, 0),
            new[]
            {
                new HdayBundle.DayEntry(
                    HdayBundle.DayEntry.FlagIsWorkday,
                    HdayBundle.NoIndex,
                    HdayBundle.NoIndex,
                    HdayBundle.NoIndex),
            },
            Array.Empty<string>(),
            Array.Empty<HdayBundle.NameListEntry>());

        var info = bundle.GetDayInfo(new DateOnly(1900, 1, 1));
        Assert.IsNotNull(info);
        Assert.IsTrue(info.IsWorkday);
        Assert.AreEqual(0, info.Extensions.Count);
    }
}
