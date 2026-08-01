package com.github.gmkits.apiassets.calendar.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.security.MessageDigest;
import java.util.HexFormat;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability
class CalendarApiTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void exposesCombinedCalendarDay() throws Exception {
        mvc.perform(get("/v1/calendar/dates/2025-10-06").param("region", "CN"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.date").value("2025-10-06"))
                .andExpect(jsonPath("$.regionCode").value("CN"))
                .andExpect(jsonPath("$.isHoliday").value(true))
                .andExpect(jsonPath("$.isStatutoryHoliday").value(true))
                .andExpect(jsonPath("$.lunar.month").value(8))
                .andExpect(jsonPath("$.lunar.day").value(15))
                .andExpect(jsonPath("$.ganZhi.yearName").value("乙巳"))
                .andExpect(jsonPath("$.festivals[0].code").value("MID_AUTUMN"))
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public,max-age=3600"));
    }

    @Test
    void exposesAllBusinessOperations() throws Exception {
        mvc.perform(get("/v1/calendar/dates")
                        .param("from", "2025-10-01").param("to", "2025-10-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8));

        mvc.perform(get("/v1/calendar/dates")
                        .param("from", "2024-01-01").param("to", "2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(366));

        mvc.perform(get("/v1/calendar/years/2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(365));

        mvc.perform(get("/v1/calendar/workdays/count")
                        .param("from", "2026-01-01").param("to", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workdays", greaterThan(200)));

        mvc.perform(get("/v1/calendar/holidays/next").param("from", "2026-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-01-01"));

        mvc.perform(get("/v1/calendar/lunar/from-solar").param("date", "2025-01-29"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date.year").value(2025))
                .andExpect(jsonPath("$.date.month").value(1))
                .andExpect(jsonPath("$.date.day").value(1));

        mvc.perform(get("/v1/calendar/solar/from-lunar")
                        .param("year", "2025").param("month", "1").param("day", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2025-01-29"));

        mvc.perform(get("/v1/calendar/solar-terms/2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.terms.length()").value(24));

        mvc.perform(get("/v1/calendar/metadata"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public,max-age=60"))
                .andExpect(jsonPath("$.releaseVersion").value("1.0.0-rc.1"))
                .andExpect(jsonPath("$.holidays[0].startYear").value(2000))
                .andExpect(jsonPath("$.holidays[0].endYear").value(2026));

        mvc.perform(get("/internal/metrics"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("jvm_")));
    }

    @Test
    void rejectsInvalidAndPartialRangesWithProblemDetails() throws Exception {
        mvc.perform(get("/v1/calendar/dates")
                        .param("from", "1999-12-31").param("to", "2000-01-02"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("CALENDAR_DATA_NOT_AVAILABLE"));

        mvc.perform(get("/v1/calendar/dates")
                        .param("from", "2026-01-02").param("to", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));

        mvc.perform(get("/v1/calendar/dates/2026-02-30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));

        mvc.perform(get("/v1/calendar/dates")
                        .param("from", "2025-01-01").param("to", "2026-01-02"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/v1/calendar/solar-terms/1900"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CALENDAR_DATA_NOT_AVAILABLE"));

        mvc.perform(get("/v1/calendar/solar/from-lunar")
                        .param("year", "2024").param("month", "6").param("day", "1")
                        .param("leapMonth", "true"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/v1/calendar/holidays/next").param("from", "2026-12-31"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NO_FUTURE_HOLIDAY"));

        mvc.perform(get("/v1/calendar/years/1999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CALENDAR_DATA_NOT_AVAILABLE"));

        mvc.perform(get("/v1/calendar/dates/2026-01-01").param("region", "ZZ"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CALENDAR_DATA_NOT_AVAILABLE"));

        mvc.perform(get("/v1/calendar/workdays/count")
                        .param("from", "2026-01-02").param("to", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));

        mvc.perform(get("/v1/calendar/lunar/from-solar").param("date", "1900-01-30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));

        mvc.perform(get("/v1/calendar/lunar/from-solar"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }

    @Test
    void servesVerifiedAssetsWithConditionalRequests() throws Exception {
        mvc.perform(get("/v1/calendar/assets/manifest"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.formatVersion").value(2))
                .andExpect(jsonPath("$.releaseVersion").value("1.0.0-rc.1"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public,max-age=60"));

        MvcResult result = mvc.perform(get("/v1/calendar/assets/calendar.cdat"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andExpect(header().exists("X-Checksum-SHA256"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public,max-age=86400"))
                .andReturn();
        byte[] expectedCalendar = new ClassPathResource(
                "api-assets/calendar/calendar/calendar.cdat").getContentAsByteArray();
        String calendarSha = sha256(expectedCalendar);
        org.junit.jupiter.api.Assertions.assertArrayEquals(
                expectedCalendar, result.getResponse().getContentAsByteArray());
        org.junit.jupiter.api.Assertions.assertEquals(
                calendarSha, result.getResponse().getHeader("X-Checksum-SHA256"));
        org.junit.jupiter.api.Assertions.assertEquals(
                '"' + calendarSha + '"', result.getResponse().getHeader(HttpHeaders.ETAG));

        mvc.perform(get("/v1/calendar/assets/calendar.cdat")
                        .header(HttpHeaders.IF_NONE_MATCH,
                                result.getResponse().getHeader(HttpHeaders.ETAG)))
                .andExpect(status().isNotModified())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public,max-age=86400"));

        mvc.perform(get("/v1/calendar/assets/holidays/CN/2026.hday"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"CN-2026.hday\""));

        mvc.perform(get("/v1/calendar/assets/holidays/CN/1999.hday"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ASSET_NOT_FOUND"));

        mvc.perform(get("/v1/calendar/assets/holidays/cn/2026.hday"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }

    @Test
    void healthAndRequestIdDoNotDependOnAuthentication() throws Exception {
        mvc.perform(get("/internal/health/live").header("X-Request-Id", "test-request-1"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "test-request-1"))
                .andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/internal/health/ready"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataVersion").value("2026.GOV_NOTICE"))
                .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    void oldApiIsGone() throws Exception {
        mvc.perform(get("/api/v1/day").param("date", "2025-01-01"))
                .andExpect(status().isNotFound());
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
