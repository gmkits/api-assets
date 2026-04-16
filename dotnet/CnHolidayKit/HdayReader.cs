using System.Buffers.Binary;
using System.Text;

namespace CnHolidayKit;

public static class HdayReader
{
    private const string Magic = "HDAY";
    private const int HeaderSize = 32;
    private const int SectionEntrySize = 8;
    private const int DayEntrySize = 8;
    private const ushort DayTableSection = 0x0001;
    private const ushort StringTableSection = 0x0002;
    private const ushort NameListTableSection = 0x0003;

    public static HdayBundle Read(string filePath)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(filePath);
        return Parse(File.ReadAllBytes(filePath));
    }

    public static HdayBundle Read(Stream stream)
    {
        ArgumentNullException.ThrowIfNull(stream);
        using var memory = new MemoryStream();
        stream.CopyTo(memory);
        return Parse(memory.ToArray());
    }

    public static HdayBundle Parse(ReadOnlySpan<byte> data)
    {
        if (data.Length < HeaderSize + sizeof(uint))
        {
            throw new InvalidDataException(".hday 文件过小，至少应包含 32 字节头和 4 字节 CRC32");
        }

        var payload = data[..^sizeof(uint)];
        var storedCrc = BinaryPrimitives.ReadUInt32LittleEndian(data[^sizeof(uint)..]);
        var computedCrc = Crc32.Compute(payload);
        if (storedCrc != computedCrc)
        {
            throw new InvalidDataException($"CRC32 mismatch: expected 0x{storedCrc:x8}, actual 0x{computedCrc:x8}");
        }

        var header = ParseHeader(payload);
        if (header.MajorVersion != 1)
        {
            throw new InvalidDataException($"不支持的 .hday 主版本号: {header.MajorVersion}（期望 1）");
        }

        EnsureAvailable(payload, HeaderSize, header.SectionCount * SectionEntrySize, "section table");
        var sections = ParseSectionTable(payload, header.SectionCount);
        var daySection = FindSection(sections, DayTableSection) ?? throw new InvalidDataException(".hday 文件缺少必需的 DAY_TABLE 段");
        var stringSection = FindSection(sections, StringTableSection) ?? throw new InvalidDataException(".hday 文件缺少必需的 STRING_TABLE 段");
        var nameListSection = FindSection(sections, NameListTableSection) ?? throw new InvalidDataException(".hday 文件缺少必需的 NAME_LIST_TABLE 段");

        var dayEntries = ParseDayTable(payload, daySection, header.DayCount);
        var strings = ParseStringTable(payload, stringSection);
        var nameLists = ParseNameListTable(payload, nameListSection);

        return new HdayBundle(header, dayEntries, strings, nameLists);
    }

    private static HdayBundle.BundleHeader ParseHeader(ReadOnlySpan<byte> payload)
    {
        EnsureAvailable(payload, 0, HeaderSize, "header");
        var magic = Encoding.UTF8.GetString(payload[..4]);
        if (!string.Equals(magic, Magic, StringComparison.Ordinal))
        {
            throw new InvalidDataException($".hday 魔数无效：期望 \"{Magic}\"，实际 \"{magic}\"");
        }

        var majorVersion = payload[0x04];
        var minorVersion = payload[0x05];
        var flags = BinaryPrimitives.ReadUInt16LittleEndian(payload.Slice(0x06, 2));
        var year = BinaryPrimitives.ReadUInt16LittleEndian(payload.Slice(0x08, 2));
        var regionCodeLength = payload[0x0A];
        if (regionCodeLength > 16)
        {
            throw new InvalidDataException($"regionCodeLen 超出范围: {regionCodeLength}");
        }

        var regionCode = Encoding.UTF8.GetString(payload.Slice(0x0B, regionCodeLength));
        var calendarSystem = ResolveCalendarSystem(payload[0x1B]);
        var dayCount = BinaryPrimitives.ReadUInt16LittleEndian(payload.Slice(0x1C, 2));
        var sectionCount = BinaryPrimitives.ReadUInt16LittleEndian(payload.Slice(0x1E, 2));

        return new HdayBundle.BundleHeader(magic, majorVersion, minorVersion, flags, year, regionCode, calendarSystem, dayCount, sectionCount);
    }

    private static SectionInfo[] ParseSectionTable(ReadOnlySpan<byte> payload, int sectionCount)
    {
        var sections = new SectionInfo[sectionCount];
        var offset = HeaderSize;
        for (var index = 0; index < sectionCount; index++)
        {
            var entry = payload.Slice(offset, SectionEntrySize);
            sections[index] = new SectionInfo(
                BinaryPrimitives.ReadUInt16LittleEndian(entry[..2]),
                BinaryPrimitives.ReadInt32LittleEndian(entry.Slice(2, 4)),
                BinaryPrimitives.ReadUInt16LittleEndian(entry.Slice(6, 2)));
            offset += SectionEntrySize;
        }

        return sections;
    }

    private static HdayBundle.DayEntry[] ParseDayTable(ReadOnlySpan<byte> payload, SectionInfo section, int dayCount)
    {
        var body = ReadSection(payload, section, "DAY_TABLE");
        var requiredBytes = checked(dayCount * DayEntrySize);
        if (body.Length < requiredBytes)
        {
            throw new InvalidDataException($"DAY_TABLE 长度不足: 期望至少 {requiredBytes} 字节，实际 {body.Length} 字节");
        }

        var result = new HdayBundle.DayEntry[dayCount];
        for (var index = 0; index < dayCount; index++)
        {
            var entry = body.Slice(index * DayEntrySize, DayEntrySize);
            result[index] = new HdayBundle.DayEntry(
                BinaryPrimitives.ReadUInt16LittleEndian(entry[..2]),
                BinaryPrimitives.ReadUInt16LittleEndian(entry.Slice(2, 2)),
                BinaryPrimitives.ReadUInt16LittleEndian(entry.Slice(4, 2)),
                BinaryPrimitives.ReadUInt16LittleEndian(entry.Slice(6, 2)));
        }

        return result;
    }

    private static string[] ParseStringTable(ReadOnlySpan<byte> payload, SectionInfo section)
    {
        var body = ReadSection(payload, section, "STRING_TABLE");
        EnsureAvailable(body, 0, 2, "STRING_TABLE.stringCount");
        var stringCount = BinaryPrimitives.ReadUInt16LittleEndian(body[..2]);
        var offset = 2;
        var result = new string[stringCount];
        for (var index = 0; index < stringCount; index++)
        {
            EnsureAvailable(body, offset, 2, $"STRING_TABLE[{index}].length");
            var length = BinaryPrimitives.ReadUInt16LittleEndian(body.Slice(offset, 2));
            offset += 2;
            EnsureAvailable(body, offset, length, $"STRING_TABLE[{index}].data");
            result[index] = Encoding.UTF8.GetString(body.Slice(offset, length));
            offset += length;
        }

        return result;
    }

    private static HdayBundle.NameListEntry[] ParseNameListTable(ReadOnlySpan<byte> payload, SectionInfo section)
    {
        var body = ReadSection(payload, section, "NAME_LIST_TABLE");
        EnsureAvailable(body, 0, 2, "NAME_LIST_TABLE.listCount");
        var listCount = BinaryPrimitives.ReadUInt16LittleEndian(body[..2]);
        var offset = 2;
        var result = new HdayBundle.NameListEntry[listCount];
        for (var listIndex = 0; listIndex < listCount; listIndex++)
        {
            EnsureAvailable(body, offset, 2, $"NAME_LIST_TABLE[{listIndex}].pairCount");
            var pairCount = BinaryPrimitives.ReadUInt16LittleEndian(body.Slice(offset, 2));
            offset += 2;
            var pairs = new HdayBundle.NameListPair[pairCount];
            for (var pairIndex = 0; pairIndex < pairCount; pairIndex++)
            {
                EnsureAvailable(body, offset, 4, $"NAME_LIST_TABLE[{listIndex}][{pairIndex}]");
                pairs[pairIndex] = new HdayBundle.NameListPair(
                    BinaryPrimitives.ReadUInt16LittleEndian(body.Slice(offset, 2)),
                    BinaryPrimitives.ReadUInt16LittleEndian(body.Slice(offset + 2, 2)));
                offset += 4;
            }

            result[listIndex] = new HdayBundle.NameListEntry(Array.AsReadOnly(pairs));
        }

        return result;
    }

    private static SectionInfo? FindSection(IEnumerable<SectionInfo> sections, ushort type)
    {
        foreach (var section in sections)
        {
            if (section.Type == type)
            {
                return section;
            }
        }

        return null;
    }

    private static CalendarSystem ResolveCalendarSystem(byte code)
    {
        return code == (byte)CalendarSystem.ChineseLunar ? CalendarSystem.ChineseLunar : CalendarSystem.Gregorian;
    }

    private static ReadOnlySpan<byte> ReadSection(ReadOnlySpan<byte> payload, SectionInfo section, string sectionName)
    {
        EnsureAvailable(payload, section.Offset, section.Length, sectionName);
        return payload.Slice(section.Offset, section.Length);
    }

    private static void EnsureAvailable(ReadOnlySpan<byte> data, int offset, int length, string description)
    {
        if (offset < 0 || length < 0 || offset > data.Length || data.Length - offset < length)
        {
            throw new InvalidDataException($"{description} 超出 .hday 边界");
        }
    }

    private readonly record struct SectionInfo(ushort Type, int Offset, int Length);
}
