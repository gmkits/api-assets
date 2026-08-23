package com.github.gmkits.apiassets.calendar.core;

import com.github.gmkits.apiassets.calendar.spec.SolarTermInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SolarTermTableTest {

    private static final Map<String, String> TRAD_TO_SIMP;
    static {
        TRAD_TO_SIMP = new HashMap<>();
        TRAD_TO_SIMP.put("驚蟄", "惊蛰");
        TRAD_TO_SIMP.put("穀雨", "谷雨");
        TRAD_TO_SIMP.put("小滿", "小满");
        TRAD_TO_SIMP.put("芒種", "芒种");
        TRAD_TO_SIMP.put("處暑", "处暑");
    }

    @Test
    void lookupOutsideSupportedRange_returnsNull() {
        assertNull(SolarTermTable.lookup(1900, 0));
        assertNull(SolarTermTable.lookup(2101, 0));
    }

    @Test
    void lookupOnNonSolarTermDate_returnsNull() {
        LocalDate date = LocalDate.of(2025, 2, 4);
        assertNull(SolarTermTable.lookup(date.getYear(), date.getDayOfYear() - 1));
    }

    @Test
    void forYearBuildsDirectDayIndex() {
        SolarTermInfo[] terms = SolarTermTable.forYear(2025);
        assertEquals(365, terms.length);
        assertNull(terms[LocalDate.of(2025, 2, 4).getDayOfYear() - 1]);
        SolarTermInfo lichun = terms[LocalDate.of(2025, 2, 3).getDayOfYear() - 1];
        assertNotNull(lichun);
        assertEquals(2, lichun.getIndex());
        assertEquals("立春", lichun.getName());
    }

    @Test
    void forYearOutsideSupportedRange_returnsNull() {
        assertNull(SolarTermTable.forYear(1900));
        assertNull(SolarTermTable.forYear(2101));
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/solar-terms.csv", numLinesToSkip = 1)
    void lookupMatchesCsv(String solarDate, int solarTermIndex, String solarTermName) {
        LocalDate date = LocalDate.parse(solarDate);
        SolarTermInfo info = SolarTermTable.lookup(date.getYear(), date.getDayOfYear() - 1);
        assertNotNull(info, solarDate);
        assertEquals(solarTermIndex, info.getIndex(), solarDate);
        String expected = TRAD_TO_SIMP.getOrDefault(solarTermName, solarTermName);
        assertEquals(expected, info.getName(), solarDate);
    }
}
