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
                .andExpect(jsonPath("$.holiday").value(true))
                .andExpect(jsonPath("$.workday").value(false))
                .andExpect(jsonPath("$.weekend").value(false))
                .andExpect(jsonPath("$.statutoryHoliday").value(true))
                .andExpect(jsonPath("$.adjustedWorkday").value(false))
                .andExpect(jsonPath("$.holidayNames").isMap())
                .andExpect(jsonPath("$.labels").isArray())
                .andExpect(jsonPath("$.extensions").isMap())
                .andExpect(jsonPath("$.extensions.lunar.year").value(2024))
                .andExpect(jsonPath("$.extensions.lunar.month").value(12))
                .andExpect(jsonPath("$.extensions.lunar.day").value(2))
                .andExpect(jsonPath("$.extensions.lunar.ganZhiYear").value("甲辰年"))
                .andExpect(jsonPath("$.extensions.lunar.shengXiao").value("龙"))
                .andExpect(jsonPath("$.extensions.lunar.monthName").value("腊月"))
                .andExpect(jsonPath("$.extensions.lunar.dayName").value("初二"));
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
                .andExpect(jsonPath("$.holiday").value(true))
                .andExpect(jsonPath("$.statutoryHoliday").value(true));
    }

    @Test
    void getDay_midAutumn2025_combinesAllDateLayers() throws Exception {
        mockMvc.perform(get("/api/v1/day")
                        .param("date", "2025-10-06")
                        .param("regionCode", "CN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holiday").value(true))
                .andExpect(jsonPath("$.officialHoliday").value(true))
                .andExpect(jsonPath("$.statutoryHoliday").value(true))
                .andExpect(jsonPath("$.sourceVersion").value("2025.GOV_NOTICE"))
                .andExpect(jsonPath("$.extensions.lunar.month").value(8))
                .andExpect(jsonPath("$.extensions.lunar.day").value(15))
                .andExpect(jsonPath("$.festivals[0].code").value("MID_AUTUMN"));
    }

    @Test
    void getDay_normalWeekend_isNotOfficialHoliday() throws Exception {
        mockMvc.perform(get("/api/v1/day")
                        .param("date", "2025-01-04")
                        .param("regionCode", "CN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holiday").value(true))
                .andExpect(jsonPath("$.weekend").value(true))
                .andExpect(jsonPath("$.officialHoliday").value(false));
    }

    @Test
    void getDay_liChun2025_hasSolarTerm() throws Exception {
        mockMvc.perform(get("/api/v1/day")
                        .param("date", "2025-02-03")
                        .param("regionCode", "CN"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.extensions.solarTerm.index").value(2))
                .andExpect(jsonPath("$.extensions.solarTerm.name").value("立春"));
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
}
