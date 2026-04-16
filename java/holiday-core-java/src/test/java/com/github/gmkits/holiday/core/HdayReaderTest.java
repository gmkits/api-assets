package com.github.gmkits.holiday.core;

import com.github.gmkits.holiday.spec.CalendarSystem;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

import com.github.gmkits.holiday.spec.DayInfo;
import com.github.gmkits.holiday.spec.SolarTermInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        assertEquals(1, bundle.getMajorVersion());
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
    void readBundle_dayInfo_includesSolarTermExtension() throws IOException {
        HdayBundle bundle;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("bundles/CN/2025.hday")) {
            bundle = HdayReader.read(is);
        }

        DayInfo info = bundle.getDayInfo(LocalDate.of(2025, 4, 4));
        assertNotNull(info);
        Object solarTerm = info.getExtensions().get("solarTerm");
        assertTrue(solarTerm instanceof SolarTermInfo, "Qingming should expose solarTerm extension");

        SolarTermInfo solarTermInfo = (SolarTermInfo) solarTerm;
        assertEquals(6, solarTermInfo.getIndex());
        assertEquals("清明", solarTermInfo.getName());
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
                                HdayBundle.NO_INDEX,
                                HdayBundle.NO_INDEX),
                },
                new String[0],
                new int[0][][]);

        DayInfo info = bundle.getDayInfo(LocalDate.of(1900, 1, 1));
        assertNotNull(info);
        assertTrue(info.isWorkday());
        assertTrue(info.getExtensions().isEmpty(), "Out-of-range lunar dates should omit lunar extension");
    }
}
