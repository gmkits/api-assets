package com.github.gmkits.holiday.lunar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32;

/**
 * 可替换的紧凑日历资产加载器。
 *
 * <p>{@code calendar/calendar.cdat} 同时保存 1900–2100 农历年度描述符和
 * 1901–2100 节气 48-bit 表，替代运行时解析约 100 KB CSV/HEX 文本。</p>
 */
final class CalendarAssetLoader {

    private static final String ROOT_PROPERTY = "cn.holiday.assets.path";
    private static final String ROOT_ENV = "CN_HOLIDAY_ASSETS";
    private static final String CLASSPATH_ROOT = "cn-holiday-kit/assets/";
    private static final String ASSET_PATH = "calendar/calendar.cdat";
    private static final int HEADER_SIZE = 16;
    private static final int SECTION_ENTRY_SIZE = 12;
    private static final int SECTION_CRITICAL = 1;
    private static final int SECTION_LUNAR = 1;
    private static final int SECTION_SOLAR = 2;

    private static volatile boolean attempted;
    private static volatile CalendarData cached;

    private CalendarAssetLoader() {
    }

    static int[] loadLunarYears() {
        CalendarData data = load();
        if (data.lunarStart != 1900 || data.lunarEnd != 2100
                || data.lunarYears.length != 201) {
            throw new ExceptionInInitializerError("calendar.cdat lunar range mismatch");
        }
        return data.lunarYears.clone();
    }

    static long[] loadSolarTerms(
            int startYear,
            int endYear,
            int[] baseDays) {
        CalendarData data = load();
        int yearCount = endYear - startYear + 1;
        if (data.solarStart != startYear || data.solarEnd != endYear
                || data.solarTerms.length != yearCount
                || data.solarBaseDays.length != baseDays.length) {
            throw new ExceptionInInitializerError("calendar.cdat solar-term range mismatch");
        }
        for (int i = 0; i < baseDays.length; i++) {
            if (data.solarBaseDays[i] != baseDays[i]) {
                throw new ExceptionInInitializerError(
                        "calendar.cdat solar-term base-day mismatch at " + i);
            }
        }
        return data.solarTerms.clone();
    }

    static String sourceDescription() {
        Path root = externalRoot();
        return root == null ? "classpath:" + CLASSPATH_ROOT
                : root.toAbsolutePath().normalize().toString();
    }

    private static CalendarData load() {
        if (attempted) return cached;
        synchronized (CalendarAssetLoader.class) {
            if (attempted) return cached;
            try (InputStream input = open(ASSET_PATH)) {
                if (input == null) {
                    throw new IOException("Missing classpath asset "
                            + CLASSPATH_ROOT + ASSET_PATH);
                }
                cached = parse(readAllBytes(input));
                attempted = true;
                return cached;
            } catch (IOException | RuntimeException exception) {
                attempted = true;
                throw new ExceptionInInitializerError(
                        "Failed to load calendar.cdat: " + exception.getMessage());
            }
        }
    }

    private static CalendarData parse(byte[] data) throws IOException {
        if (data.length < HEADER_SIZE + 2 * SECTION_ENTRY_SIZE + 4) {
            throw new IOException("calendar.cdat is too small");
        }
        int crcOffset = data.length - 4;
        CRC32 crc = new CRC32();
        crc.update(data, 0, crcOffset);
        long stored = Integer.toUnsignedLong(
                ByteBuffer.wrap(data, crcOffset, 4)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .getInt());
        if (crc.getValue() != stored) {
            throw new IOException("calendar.cdat CRC32 mismatch");
        }

        ByteBuffer header = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        if (header.get() != 'C' || header.get() != 'D'
                || header.get() != 'A' || header.get() != 'T') {
            throw new IOException("calendar.cdat magic mismatch");
        }
        int major = header.get() & 0xff;
        header.get();
        if (major != 1) throw new IOException("Unsupported calendar.cdat version " + major);
        int sectionCount = header.getShort() & 0xffff;
        if (sectionCount < 2
                || (long) HEADER_SIZE + (long) sectionCount * SECTION_ENTRY_SIZE
                > crcOffset) {
            throw new IOException("Invalid calendar.cdat section count");
        }
        if (header.getInt() != 0 || header.getInt() != 0) {
            throw new IOException("calendar.cdat reserved header bytes must be zero");
        }

        Map<Integer, Section> sections = new HashMap<>();
        Set<Integer> types = new HashSet<>();
        List<Section> allSections = new ArrayList<>();
        for (int index = 0; index < sectionCount; index++) {
            int type = header.getShort() & 0xffff;
            int flags = header.getShort() & 0xffff;
            long offset = Integer.toUnsignedLong(header.getInt());
            long length = Integer.toUnsignedLong(header.getInt());
            if ((flags & ~SECTION_CRITICAL) != 0
                    || offset < HEADER_SIZE + sectionCount * SECTION_ENTRY_SIZE
                    || length > Integer.MAX_VALUE
                    || offset > crcOffset - length
                    || !types.add(type)) {
                throw new IOException("Invalid calendar.cdat section " + type);
            }
            boolean known = type == SECTION_LUNAR || type == SECTION_SOLAR;
            if (!known && (flags & SECTION_CRITICAL) != 0) {
                throw new IOException("Unknown critical calendar.cdat section " + type);
            }
            Section section = new Section(type, flags, (int) offset, (int) length);
            allSections.add(section);
            if (known) sections.put(type, section);
        }
        Section lunar = sections.get(SECTION_LUNAR);
        Section solar = sections.get(SECTION_SOLAR);
        if (lunar == null || solar == null) {
            throw new IOException("calendar.cdat missing required section");
        }
        if ((lunar.flags & SECTION_CRITICAL) == 0
                || (solar.flags & SECTION_CRITICAL) == 0) {
            throw new IOException("Required calendar.cdat section is not critical");
        }
        allSections.sort(Comparator.comparingInt(section -> section.offset));
        for (int index = 1; index < allSections.size(); index++) {
            if (overlaps(allSections.get(index - 1), allSections.get(index))) {
                throw new IOException("calendar.cdat sections overlap");
            }
        }
        return new CalendarData(parseLunar(data, lunar), parseSolar(data, solar));
    }

    private static LunarData parseLunar(byte[] data, Section section) throws IOException {
        ByteBuffer input = sectionBuffer(data, section);
        if (input.remaining() < 8) throw new IOException("Invalid lunar section");
        int start = input.getShort() & 0xffff;
        int end = input.getShort() & 0xffff;
        int count = input.getShort() & 0xffff;
        int reserved = input.getShort() & 0xffff;
        if (reserved != 0 || count != end - start + 1 || input.remaining() != count * 4) {
            throw new IOException("Invalid lunar section shape");
        }
        int[] values = new int[count];
        for (int i = 0; i < count; i++) values[i] = input.getInt();
        return new LunarData(start, end, values);
    }

    private static SolarData parseSolar(byte[] data, Section section) throws IOException {
        ByteBuffer input = sectionBuffer(data, section);
        if (input.remaining() < 32) throw new IOException("Invalid solar section");
        int start = input.getShort() & 0xffff;
        int end = input.getShort() & 0xffff;
        int count = input.getShort() & 0xffff;
        int termCount = input.get() & 0xff;
        int reserved = input.get() & 0xff;
        if (reserved != 0 || termCount != 24 || count != end - start + 1
                || input.remaining() != termCount + count * 6) {
            throw new IOException("Invalid solar section shape");
        }
        int[] baseDays = new int[termCount];
        for (int i = 0; i < termCount; i++) baseDays[i] = input.get() & 0xff;
        long[] values = new long[count];
        for (int year = 0; year < count; year++) {
            long packed = 0;
            for (int b = 0; b < 6; b++) {
                packed |= (long) (input.get() & 0xff) << (b * 8);
            }
            values[year] = packed;
        }
        return new SolarData(start, end, baseDays, values);
    }

    private static boolean overlaps(Section left, Section right) {
        return (long) left.offset < (long) right.offset + right.length
                && (long) right.offset < (long) left.offset + left.length;
    }

    private static ByteBuffer sectionBuffer(byte[] data, Section section) {
        return ByteBuffer.wrap(data, section.offset, section.length)
                .slice()
                .order(ByteOrder.LITTLE_ENDIAN);
    }

    private static InputStream open(String relativePath) throws IOException {
        Path root = externalRoot();
        if (root != null) {
            Path file = root.resolve(relativePath);
            if (!Files.isRegularFile(file)) {
                throw new IOException("Missing external asset " + file);
            }
            return Files.newInputStream(file);
        }
        return CalendarAssetLoader.class.getClassLoader()
                .getResourceAsStream(CLASSPATH_ROOT + relativePath);
    }

    private static Path externalRoot() {
        String configured = System.getProperty(ROOT_PROPERTY);
        if (configured == null || configured.trim().isEmpty()) {
            configured = System.getenv(ROOT_ENV);
        }
        return configured == null || configured.trim().isEmpty()
                ? null
                : Paths.get(configured.trim());
    }

    private static byte[] readAllBytes(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(4096);
        byte[] buffer = new byte[4096];
        int count;
        while ((count = input.read(buffer)) >= 0) output.write(buffer, 0, count);
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

    private static final class LunarData {
        private final int start;
        private final int end;
        private final int[] values;

        private LunarData(int start, int end, int[] values) {
            this.start = start;
            this.end = end;
            this.values = values;
        }
    }

    private static final class SolarData {
        private final int start;
        private final int end;
        private final int[] baseDays;
        private final long[] values;

        private SolarData(int start, int end, int[] baseDays, long[] values) {
            this.start = start;
            this.end = end;
            this.baseDays = baseDays;
            this.values = values;
        }
    }

    private static final class CalendarData {
        private final int lunarStart;
        private final int lunarEnd;
        private final int[] lunarYears;
        private final int solarStart;
        private final int solarEnd;
        private final int[] solarBaseDays;
        private final long[] solarTerms;

        private CalendarData(LunarData lunar, SolarData solar) {
            this.lunarStart = lunar.start;
            this.lunarEnd = lunar.end;
            this.lunarYears = lunar.values;
            this.solarStart = solar.start;
            this.solarEnd = solar.end;
            this.solarBaseDays = solar.baseDays;
            this.solarTerms = solar.values;
        }
    }
}
