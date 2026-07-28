package com.github.gmkits.holiday.core;

import com.github.gmkits.holiday.spec.CalendarSystem;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.CRC32;

import com.github.gmkits.holiday.spec.DayInfo;
import com.github.gmkits.holiday.spec.SolarTermInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link HdayReader} binary parser.
 */
class HdayReaderTest {

    @Test
    void readBundle_CN2025_shouldParseCorrectly() throws IOException {
        HdayBundle bundle;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("bundles/CN/2025.hday")) {
            assertNotNull(is, "2025.hday should exist on classpath");
            bundle = HdayReader.read(is);
        }
        assertEquals(2025, bundle.getYear());
        assertEquals("CN", bundle.getRegionCode());
        assertEquals(365, bundle.getDayCount());
        assertEquals(2, bundle.getMajorVersion());
    }

    @Test
    void readBundle_CN2026_shouldParseCorrectly() throws IOException {
        HdayBundle bundle;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("bundles/CN/2026.hday")) {
            assertNotNull(is, "2026.hday should exist on classpath");
            bundle = HdayReader.read(is);
        }
        assertEquals(2026, bundle.getYear());
        assertEquals("CN", bundle.getRegionCode());
        assertEquals(365, bundle.getDayCount());
    }

    @Test
    void readBundle_tooSmall_shouldThrow() {
        byte[] data = new byte[16];
        assertThrows(Exception.class, () -> HdayReader.read(new ByteArrayInputStream(data)));
    }

    @Test
    void readBundle_wrongMagic_shouldThrow() {
        byte[] data = new byte[64];
        data[0] = 'B';
        data[1] = 'A';
        data[2] = 'D';
        data[3] = '!';
        assertThrows(Exception.class, () -> HdayReader.read(new ByteArrayInputStream(data)));
    }

    @Test
    void readBundle_corruption_shouldReturnStableCodes() throws IOException {
        byte[] crc = bundleBytes();
        crc[crc.length - 5] ^= 1;
        assertFormatCode(crc, HdayFormatException.Code.BAD_CRC);

        byte[] version = bundleBytes();
        version[4] = 3;
        assertFormatCode(version, HdayFormatException.Code.UNSUPPORTED_VERSION);

        byte[] utf8 = bundleBytes();
        utf8[11] = (byte) 0xc3;
        utf8[12] = 0x28;
        refreshCrc(utf8);
        assertFormatCode(utf8, HdayFormatException.Code.BAD_UTF8);

        byte[] padding = bundleBytes();
        padding[13] = 1;
        refreshCrc(padding);
        assertFormatCode(padding, HdayFormatException.Code.BAD_HEADER);

        byte[] dayCount = bundleBytes();
        putShort(dayCount, 28, 366);
        refreshCrc(dayCount);
        assertFormatCode(dayCount, HdayFormatException.Code.BAD_HEADER);
    }

    @Test
    void readBundle_sectionCorruption_shouldBeRejected() throws IOException {
        byte[] duplicate = bundleBytes();
        putShort(duplicate, 68, 1);
        refreshCrc(duplicate);
        assertFormatCode(duplicate, HdayFormatException.Code.BAD_SECTION_TABLE);

        byte[] overlap = bundleBytes();
        putInt(overlap, 72, getInt(overlap, 36));
        refreshCrc(overlap);
        assertFormatCode(overlap, HdayFormatException.Code.BAD_SECTION_TABLE);

        byte[] critical = bundleBytes();
        putShort(critical, 68, 0x7ffe);
        putShort(critical, 70, 1);
        refreshCrc(critical);
        assertFormatCode(
                critical, HdayFormatException.Code.UNKNOWN_CRITICAL_SECTION);
    }

    @Test
    void readBundle_unknownOptionalSection_shouldBeSkipped() throws IOException {
        byte[] optional = bundleBytes();
        putShort(optional, 68, 0x7ffe);
        putShort(optional, 70, 0);
        refreshCrc(optional);
        HdayBundle bundle = HdayReader.read(new ByteArrayInputStream(optional));
        assertEquals(2025, bundle.getYear());
    }

    @Test
    void readBundle_invalidOverride_shouldBeRejected() throws IOException {
        byte[] badDay = bundleBytes();
        int daySection = getInt(badDay, 36);
        putShort(badDay, daySection + 2, 365);
        refreshCrc(badDay);
        assertFormatCode(badDay, HdayFormatException.Code.BAD_DAY_OVERRIDE);

        byte[] badState = bundleBytes();
        int stateSection = getInt(badState, 36);
        badState[stateSection + 4] = 3;
        refreshCrc(badState);
        assertFormatCode(badState, HdayFormatException.Code.BAD_DAY_OVERRIDE);

        byte[] badIndex = bundleBytes();
        int indexSection = getInt(badIndex, 36);
        putShort(badIndex, indexSection + 6, 0xfffe);
        refreshCrc(badIndex);
        assertFormatCode(badIndex, HdayFormatException.Code.BAD_INDEX);
    }

    @Test
    void readBundle_dayInfosArePrebuilt() throws IOException {
        HdayBundle bundle;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("bundles/CN/2025.hday")) {
            bundle = HdayReader.read(is);
        }
        assertNotNull(bundle.getDayInfos());
        assertEquals(365, bundle.getDayInfos().size());
        assertNotNull(bundle.getDayInfo(0));
    }

    @Test
    void readBundle_dayInfo_dateIsCorrect() throws IOException {
        HdayBundle bundle;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("bundles/CN/2025.hday")) {
            bundle = HdayReader.read(is);
        }
        assertEquals(java.time.LocalDate.of(2025, 1, 1), bundle.getDayInfo(0).getDate());
        assertEquals(java.time.LocalDate.of(2025, 12, 31), bundle.getDayInfo(364).getDate());
    }

    @Test
    void readBundle_rangeQuery() throws IOException {
        HdayBundle bundle;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("bundles/CN/2025.hday")) {
            bundle = HdayReader.read(is);
        }
        List<DayInfo> range = bundle.getRange(0, 6);
        assertEquals(7, range.size());
        assertEquals(java.time.LocalDate.of(2025, 1, 1), range.get(0).getDate());
        assertEquals(java.time.LocalDate.of(2025, 1, 7), range.get(6).getDate());
        assertThrows(UnsupportedOperationException.class, () -> range.add(bundle.getDayInfo(0)));
    }

    @Test
    void readBundle_rangeQuery_reversed() throws IOException {
        HdayBundle bundle;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("bundles/CN/2025.hday")) {
            bundle = HdayReader.read(is);
        }
        assertTrue(bundle.getRange(10, 5).isEmpty());
    }

    @Test
    void readBundle_dayInfo_includesDirectSolarTerm() throws IOException {
        HdayBundle bundle;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("bundles/CN/2025.hday")) {
            bundle = HdayReader.read(is);
        }

        DayInfo info = bundle.getDayInfo(LocalDate.of(2025, 4, 4));
        assertNotNull(info);
        SolarTermInfo solarTermInfo = info.getSolarTerm();
        assertNotNull(solarTermInfo, "Qingming should expose solarTerm");
        assertEquals(6, solarTermInfo.getIndex());
        assertEquals("清明", solarTermInfo.getName());
        assertTrue(info.getFestivals().stream()
                .anyMatch(festival -> "TOMB_SWEEPING".equals(festival.getCode())));
    }

    @Test
    void readBundle_outOfBounds_shouldThrow() throws IOException {
        HdayBundle bundle;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("bundles/CN/2025.hday")) {
            bundle = HdayReader.read(is);
        }
        assertThrows(IndexOutOfBoundsException.class, () -> bundle.getDayInfo(365));
        assertThrows(IndexOutOfBoundsException.class, () -> bundle.getDayInfo(-1));
    }

    @Test
    void buildDayInfo_outsideLunarRange_shouldNotThrow() {
        HdayBundle bundle = new HdayBundle(
                1900,
                "CN",
                CalendarSystem.GREGORIAN,
                1,
                1,
                0,
                new HdayBundle.DayEntry[]{
                        new HdayBundle.DayEntry(
                                HdayBundle.DayEntry.FLAG_IS_WORKDAY,
                                HdayBundle.NO_INDEX,
                                HdayBundle.NO_INDEX),
                },
                new String[0],
                new int[0][][],
                "test");

        DayInfo info = bundle.getDayInfo(LocalDate.of(1900, 1, 1));
        assertNotNull(info);
        assertTrue(info.isWorkday());
        assertNull(info.getLunar(), "Out-of-range dates should have no lunar date");
        assertNull(info.getGanZhi(), "Out-of-range dates should have no gan-zhi attributes");
    }

    private byte[] bundleBytes() throws IOException {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("bundles/CN/2025.hday")) {
            assertNotNull(input);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] chunk = new byte[1024];
            int count;
            while ((count = input.read(chunk)) >= 0) {
                output.write(chunk, 0, count);
            }
            return output.toByteArray();
        }
    }

    private static void assertFormatCode(
            byte[] data,
            HdayFormatException.Code code) {
        HdayFormatException error = assertThrows(
                HdayFormatException.class,
                () -> HdayReader.read(new ByteArrayInputStream(data)));
        assertEquals(code, error.getCode());
    }

    private static void refreshCrc(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data, 0, data.length - 4);
        putInt(data, data.length - 4, (int) crc.getValue());
    }

    private static int getInt(byte[] data, int offset) {
        return ByteBuffer.wrap(data, offset, 4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .getInt();
    }

    private static void putInt(byte[] data, int offset, int value) {
        ByteBuffer.wrap(data, offset, 4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(value);
    }

    private static void putShort(byte[] data, int offset, int value) {
        ByteBuffer.wrap(data, offset, 2)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort((short) value);
    }
}
