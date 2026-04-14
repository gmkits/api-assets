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
                .andExpect(jsonPath("$.labels").isArray());
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
