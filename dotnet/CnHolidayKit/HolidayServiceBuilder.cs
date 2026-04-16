namespace CnHolidayKit;

public sealed class HolidayServiceBuilder
{
    private string _defaultRegion = "CN";
    private string? _dataDirectory;
    private int _cacheSize = 32;

    public HolidayServiceBuilder WithDefaultRegion(string regionCode)
    {
        _defaultRegion = string.IsNullOrWhiteSpace(regionCode) ? "CN" : regionCode.Trim();
        return this;
    }

    public HolidayServiceBuilder WithDataDirectory(string dataDirectory)
    {
        _dataDirectory = dataDirectory ?? throw new ArgumentNullException(nameof(dataDirectory));
        return this;
    }

    public HolidayServiceBuilder WithCacheSize(int cacheSize)
    {
        if (cacheSize <= 0)
        {
            throw new ArgumentOutOfRangeException(nameof(cacheSize), $"cacheSize 必须为正数: {cacheSize}");
        }

        _cacheSize = cacheSize;
        return this;
    }

    public IHolidayService Build()
    {
        if (string.IsNullOrWhiteSpace(_dataDirectory))
        {
            throw new InvalidOperationException("未配置 .hday 数据目录，请先调用 WithDataDirectory。");
        }

        return new HolidayServiceImpl(_defaultRegion, _dataDirectory, _cacheSize);
    }
}
