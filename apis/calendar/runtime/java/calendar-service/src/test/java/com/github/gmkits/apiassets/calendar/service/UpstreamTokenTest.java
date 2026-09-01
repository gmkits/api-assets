package com.github.gmkits.apiassets.calendar.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.HttpHeaders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "calendar.upstream-token=test-secret")
@AutoConfigureMockMvc
@AutoConfigureObservability
class UpstreamTokenTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void protectsBusinessAndMetricsButNotHealth() throws Exception {
        mvc.perform(get("/v1/calendar/dates/2025-01-01"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UPSTREAM_UNAUTHORIZED"));
        mvc.perform(get("/v1/calendar/dates/2025-01-01")
                        .header("Authorization", "Bearer wrong"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/v1/calendar/dates/2025-01-01")
                        .header("Authorization", "Bearer test-secret"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string(HttpHeaders.CACHE_CONTROL, "private,max-age=3600"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string(HttpHeaders.VARY, "Authorization"));
        mvc.perform(get("/internal/metrics"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/v1/calendar/assets/manifest"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UPSTREAM_UNAUTHORIZED"));
        mvc.perform(get("/internal/metrics")
                        .header("Authorization", "Bearer test-secret"))
                .andExpect(status().isOk());
        mvc.perform(get("/internal/health/live"))
                .andExpect(status().isOk());
    }
}
