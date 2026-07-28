package com.github.gmkits.holiday.core;

import com.github.gmkits.holiday.spec.CalendarSystem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32;

/**
 * 严格的 {@code .hday} v2 读取器。
 *
 * <p>读取器在分配任何由文件控制大小的数组前验证 CRC、版本、目录边界、
 * section 重叠关系和内部计数，避免损坏文件退化为运行时越界异常。</p>
 */
public final class HdayReader {

    private static final byte[] MAGIC = {'H', 'D', 'A', 'Y'};
    private static final int VERSION_MAJOR = 2;
    private static final int HEADER_SIZE = 32;
    private static final int SECTION_ENTRY_SIZE = 12;
    private static final int DAY_OVERRIDE_SIZE = 8;
    private static final int SECTION_CRITICAL = 1;

    private static final int SECTION_DAY_OVERRIDES = 0x0001;
    private static final int SECTION_STRING_TABLE = 0x0002;
    private static final int SECTION_NAME_LIST_TABLE = 0x0003;
    private static final int SECTION_META_TABLE = 0x0004;

    private static final int STATE_FORCE_HOLIDAY = 1;
    private static final int STATE_FORCE_WORKDAY = 1 << 1;
    private static final int STATE_STATUTORY = 1 << 2;
    private static final int STATE_ADJUSTED = 1 << 3;
    private static final int KNOWN_STATES = 0x0f;

    private static final int META_SPEC_VERSION = 1;
    private static final int META_SOURCE_VERSION = 2;
    private static final int META_GENERATED_AT = 3;

    private static final Set<Integer> KNOWN_SECTIONS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    SECTION_DAY_OVERRIDES,
                    SECTION_STRING_TABLE,
                    SECTION_NAME_LIST_TABLE,
                    SECTION_META_TABLE)));

    private HdayReader() {
    }

    /**
     * 从文件读取年度 bundle。
     *
     * @param path bundle 路径
     * @return 已解析 bundle
     * @throws IOException 文件读取失败或格式非法
     */
    public static HdayBundle read(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            return read(input);
        }
    }

    /**
     * 从输入流读取年度 bundle。
     *
     * @param input 输入流；方法不会关闭该流
     * @return 已解析 bundle
     * @throws IOException 流读取失败或格式非法
     */
    public static HdayBundle read(InputStream input) throws IOException {
        return parse(readAllBytes(input));
    }

    static HdayBundle read(byte[] data) throws IOException {
        return parse(data);
    }

    private static HdayBundle parse(byte[] data) throws IOException {
        if (data.length < HEADER_SIZE + 4) {
            throw format(HdayFormatException.Code.TOO_SMALL,
                    "File too small to be a valid .hday bundle");
        }
        int crcOffset = data.length - 4;

        ByteBuffer header = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        byte[] magic = new byte[4];
        header.get(magic);
        if (!Arrays.equals(magic, MAGIC)) {
            throw format(HdayFormatException.Code.BAD_MAGIC, "Invalid .hday magic");
        }
        int majorVersion = unsigned(header.get());
        int minorVersion = unsigned(header.get());
        if (majorVersion != VERSION_MAJOR) {
            throw format(HdayFormatException.Code.UNSUPPORTED_VERSION,
                    "Unsupported .hday major version " + majorVersion);
        }
        header.getShort(); // 可跳过的全局 flags。
        int year = unsigned(header.getShort());
        int regionLength = unsigned(header.get());
        if (regionLength == 0 || regionLength > 16) {
            throw format(HdayFormatException.Code.BAD_HEADER,
                    "Invalid region code length " + regionLength);
        }
        byte[] regionField = new byte[16];
        header.get(regionField);
        for (int i = regionLength; i < regionField.length; i++) {
            if (regionField[i] != 0) {
                throw format(HdayFormatException.Code.BAD_HEADER,
                        "Region code padding must be zero");
            }
        }
        String regionCode = decodeUtf8(regionField, 0, regionLength);
        int calendarCode = unsigned(header.get());
        if (calendarCode < 0 || calendarCode >= CalendarSystem.values().length) {
            throw format(HdayFormatException.Code.BAD_HEADER,
                    "Invalid calendar system code " + calendarCode);
        }
        CalendarSystem calendarSystem = CalendarSystem.values()[calendarCode];
        int dayCount = unsigned(header.getShort());
        int expectedDayCount = LocalDate.of(year, 1, 1).lengthOfYear();
        if (dayCount != expectedDayCount) {
            throw format(HdayFormatException.Code.BAD_HEADER,
                    year + " requires " + expectedDayCount + " days, got " + dayCount);
        }
        int sectionCount = unsigned(header.getShort());
        if (sectionCount == 0) {
            throw format(HdayFormatException.Code.BAD_SECTION_TABLE,
                    "sectionCount must be positive");
        }
        verifyCrc(data, crcOffset);

        Map<Integer, Section> sections = parseSections(
                data, sectionCount, crcOffset);
        Section daySection = requireSection(sections, SECTION_DAY_OVERRIDES);
        Section stringSection = requireSection(sections, SECTION_STRING_TABLE);
        Section nameSection = requireSection(sections, SECTION_NAME_LIST_TABLE);
        requireCritical(daySection);
        requireCritical(stringSection);
        requireCritical(nameSection);

        String[] strings = parseStrings(data, stringSection);
        int[][][] nameLists = parseNameLists(data, nameSection, strings.length);
        HdayBundle.DayEntry[] days = parseDayOverrides(
                data, daySection, year, dayCount, nameLists.length);
        Map<Integer, String> metadata = parseMetadata(
                data, sections.get(SECTION_META_TABLE), strings);

        return new HdayBundle(
                year,
                regionCode,
                calendarSystem,
                dayCount,
                majorVersion,
                minorVersion,
                days,
                strings,
                nameLists,
                metadata.getOrDefault(META_SOURCE_VERSION, ""));
    }

    private static void verifyCrc(byte[] data, int crcOffset) throws HdayFormatException {
        CRC32 crc = new CRC32();
        crc.update(data, 0, crcOffset);
        long computed = crc.getValue();
        long stored = Integer.toUnsignedLong(
                ByteBuffer.wrap(data, crcOffset, 4)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .getInt());
        if (computed != stored) {
            throw format(HdayFormatException.Code.BAD_CRC,
                    "CRC32 mismatch: stored=" + Long.toHexString(stored)
                            + ", computed=" + Long.toHexString(computed));
        }
    }

    private static Map<Integer, Section> parseSections(
            byte[] data,
            int sectionCount,
            int crcOffset) throws HdayFormatException {
        long tableLength = (long) sectionCount * SECTION_ENTRY_SIZE;
        long dataStart = HEADER_SIZE + tableLength;
        if (dataStart > crcOffset) {
            throw format(HdayFormatException.Code.BAD_SECTION_TABLE,
                    "Section directory exceeds file");
        }

        ByteBuffer directory = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        directory.position(HEADER_SIZE);
        Map<Integer, Section> sections = new HashMap<>();
        List<Section> ordered = new ArrayList<>();
        for (int i = 0; i < sectionCount; i++) {
            int type = unsigned(directory.getShort());
            int flags = unsigned(directory.getShort());
            long offsetLong = Integer.toUnsignedLong(directory.getInt());
            long lengthLong = Integer.toUnsignedLong(directory.getInt());
            if ((flags & ~SECTION_CRITICAL) != 0) {
                throw format(HdayFormatException.Code.BAD_SECTION_TABLE,
                        "Unknown section flags for type " + type);
            }
            if (sections.containsKey(type)) {
                throw format(HdayFormatException.Code.BAD_SECTION_TABLE,
                        "Duplicate section type " + type);
            }
            if (offsetLong < dataStart || lengthLong > Integer.MAX_VALUE
                    || offsetLong > crcOffset - lengthLong) {
                throw format(HdayFormatException.Code.BAD_SECTION_TABLE,
                        "Section " + type + " is out of bounds");
            }
            if (!KNOWN_SECTIONS.contains(type) && (flags & SECTION_CRITICAL) != 0) {
                throw format(HdayFormatException.Code.UNKNOWN_CRITICAL_SECTION,
                        "Unknown critical section " + type);
            }
            Section section = new Section(
                    type, flags, (int) offsetLong, (int) lengthLong);
            sections.put(type, section);
            ordered.add(section);
        }
        ordered.sort(Comparator.comparingInt(section -> section.offset));
        for (int i = 1; i < ordered.size(); i++) {
            Section previous = ordered.get(i - 1);
            Section current = ordered.get(i);
            if ((long) previous.offset + previous.length > current.offset) {
                throw format(HdayFormatException.Code.BAD_SECTION_TABLE,
                        "Overlapping sections " + previous.type + " and " + current.type);
            }
        }
        return sections;
    }

    private static Section requireSection(
            Map<Integer, Section> sections,
            int type) throws HdayFormatException {
        Section section = sections.get(type);
        if (section == null) {
            throw format(HdayFormatException.Code.MISSING_SECTION,
                    "Missing required section " + type);
        }
        return section;
    }

    private static void requireCritical(Section section) throws HdayFormatException {
        if ((section.flags & SECTION_CRITICAL) == 0) {
            throw format(HdayFormatException.Code.BAD_SECTION_TABLE,
                    "Required section " + section.type + " must be critical");
        }
    }

    private static String[] parseStrings(
            byte[] data,
            Section section) throws HdayFormatException {
        ByteBuffer buffer = sectionBuffer(data, section);
        requireRemaining(buffer, 2, "STRING_TABLE count");
        int count = unsigned(buffer.getShort());
        String[] strings = new String[count];
        for (int i = 0; i < count; i++) {
            requireRemaining(buffer, 2, "STRING_TABLE length");
            int length = unsigned(buffer.getShort());
            requireRemaining(buffer, length, "STRING_TABLE value");
            byte[] encoded = new byte[length];
            buffer.get(encoded);
            strings[i] = decodeUtf8(encoded, 0, encoded.length);
        }
        requireConsumed(buffer, "STRING_TABLE");
        return strings;
    }

    private static int[][][] parseNameLists(
            byte[] data,
            Section section,
            int stringCount) throws HdayFormatException {
        ByteBuffer buffer = sectionBuffer(data, section);
        requireRemaining(buffer, 2, "NAME_LIST_TABLE count");
        int count = unsigned(buffer.getShort());
        int[][][] lists = new int[count][][];
        for (int i = 0; i < count; i++) {
            requireRemaining(buffer, 2, "NAME_LIST_TABLE pair count");
            int pairCount = unsigned(buffer.getShort());
            if (pairCount > buffer.remaining() / 4) {
                throw format(HdayFormatException.Code.BAD_SECTION,
                        "NAME_LIST_TABLE pair count exceeds section");
            }
            lists[i] = new int[pairCount][2];
            for (int pair = 0; pair < pairCount; pair++) {
                int key = unsigned(buffer.getShort());
                int value = unsigned(buffer.getShort());
                if (key != HdayBundle.NO_INDEX && key >= stringCount) {
                    throw format(HdayFormatException.Code.BAD_INDEX,
                            "Name key string index out of bounds: " + key);
                }
                if (value >= stringCount) {
                    throw format(HdayFormatException.Code.BAD_INDEX,
                            "Name value string index out of bounds: " + value);
                }
                lists[i][pair][0] = key;
                lists[i][pair][1] = value;
            }
        }
        requireConsumed(buffer, "NAME_LIST_TABLE");
        return lists;
    }

    private static HdayBundle.DayEntry[] parseDayOverrides(
            byte[] data,
            Section section,
            int year,
            int dayCount,
            int nameListCount) throws HdayFormatException {
        ByteBuffer buffer = sectionBuffer(data, section);
        requireRemaining(buffer, 2, "DAY_OVERRIDES count");
        int count = unsigned(buffer.getShort());
        if (section.length != 2 + count * DAY_OVERRIDE_SIZE) {
            throw format(HdayFormatException.Code.BAD_SECTION,
                    "DAY_OVERRIDES length does not match record count");
        }

        HdayBundle.DayEntry[] days = new HdayBundle.DayEntry[dayCount];
        LocalDate first = LocalDate.of(year, 1, 1);
        int firstDayOfWeek = first.getDayOfWeek().getValue();
        for (int dayIndex = 0; dayIndex < dayCount; dayIndex++) {
            int dayOfWeek = ((firstDayOfWeek - 1 + dayIndex) % 7) + 1;
            boolean weekend = dayOfWeek >= 6;
            int flags = weekend
                    ? HdayBundle.DayEntry.FLAG_IS_HOLIDAY
                            | HdayBundle.DayEntry.FLAG_IS_WEEKEND
                    : HdayBundle.DayEntry.FLAG_IS_WORKDAY;
            days[dayIndex] = new HdayBundle.DayEntry(
                    flags, HdayBundle.NO_INDEX, HdayBundle.NO_INDEX);
        }

        int previousDay = -1;
        for (int i = 0; i < count; i++) {
            int dayIndex = unsigned(buffer.getShort());
            int state = unsigned(buffer.get());
            int reserved = unsigned(buffer.get());
            int nameIndex = unsigned(buffer.getShort());
            int labelIndex = unsigned(buffer.getShort());
            if (dayIndex >= dayCount || dayIndex <= previousDay) {
                throw format(HdayFormatException.Code.BAD_DAY_OVERRIDE,
                        "dayIndex must be unique and sorted: " + dayIndex);
            }
            previousDay = dayIndex;
            if (reserved != 0 || (state & ~KNOWN_STATES) != 0) {
                throw format(HdayFormatException.Code.BAD_DAY_OVERRIDE,
                        "Unknown state/reserved value at dayIndex " + dayIndex);
            }
            boolean forceHoliday = (state & STATE_FORCE_HOLIDAY) != 0;
            boolean forceWorkday = (state & STATE_FORCE_WORKDAY) != 0;
            boolean statutory = (state & STATE_STATUTORY) != 0;
            boolean adjusted = (state & STATE_ADJUSTED) != 0;
            if (forceHoliday == forceWorkday || statutory && !forceHoliday
                    || adjusted && !forceWorkday) {
                throw format(HdayFormatException.Code.BAD_DAY_OVERRIDE,
                        "Invalid state combination at dayIndex " + dayIndex);
            }
            validateListIndex(dayIndex, nameIndex, nameListCount, "name");
            validateListIndex(dayIndex, labelIndex, nameListCount, "label");

            int flags = days[dayIndex].flags
                    & ~(HdayBundle.DayEntry.FLAG_IS_HOLIDAY
                    | HdayBundle.DayEntry.FLAG_IS_WORKDAY);
            flags |= forceHoliday
                    ? HdayBundle.DayEntry.FLAG_IS_HOLIDAY
                    : HdayBundle.DayEntry.FLAG_IS_WORKDAY;
            if (statutory) flags |= HdayBundle.DayEntry.FLAG_IS_STATUTORY_HOLIDAY;
            if (adjusted) flags |= HdayBundle.DayEntry.FLAG_IS_ADJUSTED_WORKDAY;
            if (nameIndex != HdayBundle.NO_INDEX) flags |= HdayBundle.DayEntry.FLAG_HAS_NAME;
            if (labelIndex != HdayBundle.NO_INDEX) flags |= HdayBundle.DayEntry.FLAG_HAS_LABEL;
            days[dayIndex] = new HdayBundle.DayEntry(
                    flags, nameIndex, labelIndex);
        }
        return days;
    }

    private static void validateListIndex(
            int dayIndex,
            int index,
            int count,
            String label) throws HdayFormatException {
        if (index != HdayBundle.NO_INDEX && index >= count) {
            throw format(HdayFormatException.Code.BAD_INDEX,
                    label + " list index out of bounds at dayIndex " + dayIndex);
        }
    }

    private static Map<Integer, String> parseMetadata(
            byte[] data,
            Section section,
            String[] strings) throws HdayFormatException {
        Map<Integer, String> metadata = new HashMap<>();
        if (section == null) return metadata;
        ByteBuffer buffer = sectionBuffer(data, section);
        requireRemaining(buffer, 2, "META_TABLE count");
        int count = unsigned(buffer.getShort());
        if (section.length != 2 + count * 4) {
            throw format(HdayFormatException.Code.BAD_SECTION,
                    "META_TABLE length does not match record count");
        }
        for (int i = 0; i < count; i++) {
            int key = unsigned(buffer.getShort());
            int valueIndex = unsigned(buffer.getShort());
            if (valueIndex >= strings.length) {
                throw format(HdayFormatException.Code.BAD_INDEX,
                        "META_TABLE string index out of bounds: " + valueIndex);
            }
            if (key == META_SPEC_VERSION || key == META_SOURCE_VERSION
                    || key == META_GENERATED_AT) {
                metadata.put(key, strings[valueIndex]);
            }
        }
        return metadata;
    }

    private static ByteBuffer sectionBuffer(byte[] data, Section section) {
        return ByteBuffer.wrap(data, section.offset, section.length)
                .slice()
                .order(ByteOrder.LITTLE_ENDIAN);
    }

    private static void requireRemaining(
            ByteBuffer buffer,
            int bytes,
            String label) throws HdayFormatException {
        if (bytes < 0 || buffer.remaining() < bytes) {
            throw format(HdayFormatException.Code.BAD_SECTION,
                    label + " exceeds section");
        }
    }

    private static void requireConsumed(
            ByteBuffer buffer,
            String label) throws HdayFormatException {
        if (buffer.hasRemaining()) {
            throw format(HdayFormatException.Code.BAD_SECTION,
                    label + " contains trailing bytes");
        }
    }

    private static String decodeUtf8(
            byte[] bytes,
            int offset,
            int length) throws HdayFormatException {
        try {
            CharBuffer decoded = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes, offset, length));
            return decoded.toString();
        } catch (CharacterCodingException exception) {
            throw format(HdayFormatException.Code.BAD_UTF8, "Invalid UTF-8 data");
        }
    }

    private static int unsigned(byte value) {
        return value & 0xff;
    }

    private static int unsigned(short value) {
        return value & 0xffff;
    }

    private static HdayFormatException format(
            HdayFormatException.Code code,
            String message) {
        return new HdayFormatException(code, message);
    }

    private static byte[] readAllBytes(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(4096);
        byte[] buffer = new byte[4096];
        int count;
        while ((count = input.read(buffer)) >= 0) {
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }

    private static final class Section {
        private final int type;
        private final int flags;
        private final int offset;
        private final int length;

        private Section(int type, int flags, int offset, int length) {
            this.type = type;
            this.flags = flags;
            this.offset = offset;
            this.length = length;
        }
    }
}
