package com.github.gmkits.holiday.api25;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class HolidayApi25ApplicationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(webApplicationContext.getBeansOfType(jakarta.servlet.Filter.class).values()
                        .toArray(new jakarta.servlet.Filter[0]))
                .build();
    }

    @Test
    void getDay_shouldReturnWrappedDayInfo() throws Exception {
        mockMvc.perform(get("/api/v2/day")
                        .param("date", "2025-01-01")
                        .param("regionCode", "CN"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/v2/day"))
                .andExpect(jsonPath("$.data.date").value("2025-01-01"))
                .andExpect(jsonPath("$.data.regionCode").value("CN"))
                .andExpect(jsonPath("$.data.holiday").value(true));
    }

    @Test
    void getBundleMetadata_shouldReturnManifestInfo() throws Exception {
        mockMvc.perform(get("/api/v2/bundles/CN/2025/metadata"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.path").value("/api/v2/bundles/CN/2025/metadata"))
                .andExpect(jsonPath("$.data.regionCode").value("CN"))
                .andExpect(jsonPath("$.data.year").value(2025))
                .andExpect(jsonPath("$.data.file").value("CN/2025.hday"));
    }

    @Test
    void getYear_shouldReturnFullYearData() throws Exception {
        mockMvc.perform(get("/api/v2/year")
                        .param("regionCode", "CN")
                        .param("year", "2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(365));
    }

    @Test
    void getRange_shouldReturnRangeData() throws Exception {
        mockMvc.perform(get("/api/v2/range")
                        .param("regionCode", "CN")
                        .param("from", "2025-01-01")
                        .param("to", "2025-01-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(7));
    }

    @Test
    void getRegions_shouldReturnSupportedRegions() throws Exception {
        mockMvc.perform(get("/api/v2/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getVersion_shouldReturnVersionInfo() throws Exception {
        mockMvc.perform(get("/api/v2/version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.apiVersion").value("2.0.0"));
    }

    @Test
    void getManifest_shouldReturnManifestJson() throws Exception {
        mockMvc.perform(get("/api/v2/manifest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isNotEmpty());
    }

    @Test
    void getDay_missingDate_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/v2/day")
                        .param("regionCode", "CN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").isNotEmpty());
    }

    @Test
    void clearCaches_shouldSucceed() throws Exception {
        mockMvc.perform(post("/api/v2/ops/cache/clear"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.operation").value("clearCaches"));
    }

    @Test
    void warmUp_shouldSucceed() throws Exception {
        mockMvc.perform(post("/api/v2/ops/cache/warmup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"regions\":[\"CN\"],\"years\":[2025],\"includeCurrentAndNextYear\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.operation").value("warmUp"));
    }

    @Test
    void reloadManifest_shouldSucceed() throws Exception {
        mockMvc.perform(post("/api/v2/ops/manifest/reload"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.operation").value("reloadManifest"));
    }

    @Test
    void requestIdHeader_shouldBePresent() throws Exception {
        mockMvc.perform(get("/api/v2/version")
                        .header("X-Request-Id", "test-trace-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("test-trace-123"));
    }

    @Test
    void getManifest_shouldHonourIfNoneMatch() throws Exception {
        // First request: capture ETag
        String etag = mockMvc.perform(get("/api/v2/manifest"))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andReturn()
                .getResponse()
                .getHeader("ETag");
        // Second request with matching If-None-Match → 304 Not Modified
        mockMvc.perform(get("/api/v2/manifest").header("If-None-Match", etag))
                .andExpect(status().isNotModified())
                .andExpect(header().string("ETag", etag));
    }

    @Test
    void getVersion_shouldHonourIfNoneMatch() throws Exception {
        String etag = mockMvc.perform(get("/api/v2/version"))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andReturn()
                .getResponse()
                .getHeader("ETag");
        mockMvc.perform(get("/api/v2/version").header("If-None-Match", etag))
                .andExpect(status().isNotModified());
    }

    @Test
    void batchDays_shouldFanOutOnVirtualThreads() throws Exception {
        String body = "{\"regionCode\":\"CN\",\"dates\":[\"2025-01-01\",\"2025-01-02\",\"2025-05-01\"]}";
        mockMvc.perform(post("/api/v2/days:batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].date").value("2025-01-01"))
                .andExpect(jsonPath("$.data[0].data.holiday").value(true))
                .andExpect(jsonPath("$.data[2].date").value("2025-05-01"));
    }

    @Test
    void batchDays_emptyDates_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v2/days:batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"regionCode\":\"CN\",\"dates\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
