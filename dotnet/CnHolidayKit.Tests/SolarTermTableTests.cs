namespace CnHolidayKit.Tests;

[TestClass]
public sealed class SolarTermTableTests
{
    [TestMethod]
    public void LookupOutsideSupportedRangeReturnsNull()
    {
        Assert.IsNull(SolarTermTable.Lookup(1900, 0));
        Assert.IsNull(SolarTermTable.Lookup(2101, 0));
    }

    [TestMethod]
    public void LookupOnNonSolarTermDateReturnsNull()
    {
        Assert.IsNull(SolarTermTable.Lookup(new DateOnly(2025, 2, 4)));
    }

    [TestMethod]
    public void LookupMatchesCsv()
    {
        foreach (var row in TestDataPaths.SolarTermRows)
        {
            var date = row.SolarDate;
            var dayIndex = date.DayNumber - new DateOnly(date.Year, 1, 1).DayNumber;
            var info = SolarTermTable.Lookup(date.Year, dayIndex);
            Assert.IsNotNull(info, row.SolarDate.ToString("yyyy-MM-dd"));
            Assert.AreEqual(row.SolarTermIndex, info.Index, row.SolarDate.ToString("yyyy-MM-dd"));
            Assert.AreEqual(row.SolarTermName, info.Name, row.SolarDate.ToString("yyyy-MM-dd"));
        }
    }
}
