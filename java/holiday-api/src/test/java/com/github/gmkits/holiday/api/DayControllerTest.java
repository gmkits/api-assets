package com.github.gmkits.holiday.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getDay_newYear2025() throws Exception {
        mockMvc.perform(get("/api/v1/day")
                        .param("date", "2025-01-01")
                        .param("regionCode", "CN"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.date").value("2025-01-01"))
                .andExpect(jsonPath("$.regionCode").value("CN"))
                .andExpect(jsonPath("$.isHoliday").value(true))
                .andExpect(jsonPath("$.isWorkday").value(false))
                .andExpect(jsonPath("$.isWeekend").value(false))
                .andExpect(jsonPath("$.isStatutoryHoliday").value(true))
                .andExpect(jsonPath("$.isAdjustedWorkday").value(false))
                .andExpect(jsonPath("$.holiday").doesNotExist())
                .andExpect(jsonPath("$.extensions").doesNotExist())
                .andExpect(jsonPath("$.holidayNames").isMap())
                .andExpect(jsonPath("$.labels").isArray())
                .andExpect(jsonPath("$.lunar.year").value(2024))
                .andExpect(jsonPath("$.lunar.month").value(12))
                .andExpect(jsonPath("$.lunar.day").value(2))
                .andExpect(jsonPath("$.lunar.isLeapMonth").value(false))
                .andExpect(jsonPath("$.lunar.monthName").value("腊月"))
                .andExpect(jsonPath("$.lunar.dayName").value("初二"))
                .andExpect(jsonPath("$.ganZhi.yearName").value("甲辰"))
                .andExpect(jsonPath("$.ganZhi.heavenlyStem").value("甲"))
                .andExpect(jsonPath("$.ganZhi.earthlyBranch").value("辰"))
                .andExpect(jsonPath("$.ganZhi.zodiac").value("龙"));
    }

    @Test
    void getDay_nationalDay2025() throws Exception {
        mockMvc.perform(get("/api/v1/day")
                        .param("date", "2025-10-01")
                        .param("regionCode", "CN"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.date").value("2025-10-01"))
                .andExpect(jsonPath("$.regionCode").value("CN"))
                .andExpect(jsonPath("$.isHoliday").value(true))
                .andExpect(jsonPath("$.isStatutoryHoliday").value(true));
    }

    @Test
    void getDay_midAutumn2025_combinesAllDateLayers() throws Exception {
        mockMvc.perform(get("/api/v1/day")
                        .param("date", "2025-10-06")
                        .param("regionCode", "CN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isHoliday").value(true))
                .andExpect(jsonPath("$.isOfficialHoliday").value(true))
                .andExpect(jsonPath("$.isStatutoryHoliday").value(true))
                .andExpect(jsonPath("$.sourceVersion").value("2025.GOV_NOTICE"))
                .andExpect(jsonPath("$.lunar.month").value(8))
                .andExpect(jsonPath("$.lunar.day").value(15))
                .andExpect(jsonPath("$.ganZhi.yearName").value("乙巳"))
                .andExpect(jsonPath("$.festivals[0].code").value("MID_AUTUMN"));
    }

    @Test
    void getDay_normalWeekend_isNotOfficialHoliday() throws Exception {
        mockMvc.perform(get("/api/v1/day")
                        .param("date", "2025-01-04")
                        .param("regionCode", "CN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isHoliday").value(true))
                .andExpect(jsonPath("$.isWeekend").value(true))
                .andExpect(jsonPath("$.isOfficialHoliday").value(false));
    }

    @Test
    void getDay_liChun2025_hasSolarTerm() throws Exception {
        mockMvc.perform(get("/api/v1/day")
                        .param("date", "2025-02-03")
                        .param("regionCode", "CN"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.solarTerm.index").value(2))
                .andExpect(jsonPath("$.solarTerm.name").value("立春"));
    }

    @Test
    void getDay_qingMingCombinesSolarTermAndFestival() throws Exception {
        mockMvc.perform(get("/api/v1/day")
                        .param("date", "2025-04-04")
                        .param("regionCode", "CN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solarTerm.name").value("清明"))
                .andExpect(jsonPath("$.festivals[0].code").value("TOMB_SWEEPING"));
    }

    @Test
    void getDay_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/day")
                        .param("date", "2099-01-01")
                        .param("regionCode", "CN"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDay_defaultRegion() throws Exception {
        mockMvc.perform(get("/api/v1/day")
                        .param("date", "2025-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regionCode").value("CN"));
    }

    @Test
    void getRange_missingBoundaryYearDoesNotReturnPartialData() throws Exception {
        mockMvc.perform(get("/api/v1/range")
                        .param("from", "1999-12-31")
                        .param("to", "2000-01-02"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getRange_reversedBoundaryIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/range")
                        .param("from", "2025-01-02")
                        .param("to", "2025-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getYear_usesQueryParameterContract() throws Exception {
        mockMvc.perform(get("/api/v1/year").param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(365));
    }
}
