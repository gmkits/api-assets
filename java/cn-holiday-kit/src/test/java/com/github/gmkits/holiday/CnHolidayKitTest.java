package com.github.gmkits.holiday;

import com.github.gmkits.holiday.core.HolidayService;
import com.github.gmkits.holiday.lunar.LunarInfo;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CnHolidayKitTest {

    @Test
    void singleEntryLoadsBundledHolidayAndCalendarAssets() {
        HolidayService service = CnHolidayKit.create();
        assertTrue(service.isHoliday(LocalDate.of(2026, 10, 1)));

        LunarInfo lunar = CnHolidayKit.solarToLunar(LocalDate.of(2026, 2, 17));
        assertNotNull(lunar);
        assertEquals(1, lunar.getDate().getMonth());
        assertEquals(1, lunar.getDate().getDay());
        assertEquals("立春", CnHolidayKit.getSolarTerm(LocalDate.of(2025, 2, 3)));
        assertEquals(24, CnHolidayKit.getSolarTerms(2026).length);
    }
}
