using System.Globalization;

namespace CnHolidayKit.Tests;

internal static class TestDataPaths
{
    private static readonly Lazy<IReadOnlyList<LunarGoldenRow>> LunarRowsCache = new(LoadLunarGoldenRows);
    private static readonly Lazy<IReadOnlyList<SolarTermCsvRow>> SolarTermRowsCache = new(LoadSolarTermRows);

    public static string BundleRoot => Path.Combine(AppContext.BaseDirectory, "TestData", "bundles");

    public static string Bundle2025 => Path.Combine(BundleRoot, "CN", "2025.hday");

    public static string Bundle2026 => Path.Combine(BundleRoot, "CN", "2026.hday");

    public static string LunarGoldenCsv => Path.Combine(AppContext.BaseDirectory, "TestData", "lunar-golden.csv");

    public static string SolarTermsCsv => Path.Combine(AppContext.BaseDirectory, "TestData", "solar-terms.csv");

    public static IReadOnlyList<LunarGoldenRow> LunarGoldenRows => LunarRowsCache.Value;

    public static IReadOnlyList<SolarTermCsvRow> SolarTermRows => SolarTermRowsCache.Value;

    public static DateOnly ParseDate(string value)
    {
        return DateOnly.ParseExact(value, "yyyy-MM-dd", CultureInfo.InvariantCulture);
    }

    private static IReadOnlyList<LunarGoldenRow> LoadLunarGoldenRows()
    {
        return File.ReadLines(LunarGoldenCsv)
            .Skip(1)
            .Where(static line => !string.IsNullOrWhiteSpace(line))
            .Select(static line =>
            {
                var parts = line.Split(',');
                return new LunarGoldenRow(
                    ParseDate(parts[0]),
                    int.Parse(parts[1], CultureInfo.InvariantCulture),
                    int.Parse(parts[2], CultureInfo.InvariantCulture),
                    int.Parse(parts[3], CultureInfo.InvariantCulture),
                    parts[4] == "1");
            })
            .ToArray();
    }

    private static IReadOnlyList<SolarTermCsvRow> LoadSolarTermRows()
    {
        return File.ReadLines(SolarTermsCsv)
            .Skip(1)
            .Where(static line => !string.IsNullOrWhiteSpace(line))
            .Select(static line =>
            {
                var parts = line.Split(',');
                return new SolarTermCsvRow(
                    ParseDate(parts[0]),
                    int.Parse(parts[1], CultureInfo.InvariantCulture),
                    parts[2]);
            })
            .ToArray();
    }

    internal readonly record struct LunarGoldenRow(
        DateOnly SolarDate,
        int LunarYear,
        int LunarMonth,
        int LunarDay,
        bool IsLeapMonth);

    internal readonly record struct SolarTermCsvRow(
        DateOnly SolarDate,
        int SolarTermIndex,
        string SolarTermName);
}
