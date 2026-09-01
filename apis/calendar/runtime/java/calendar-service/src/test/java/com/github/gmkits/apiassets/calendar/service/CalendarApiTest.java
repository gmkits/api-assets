package com.github.gmkits.apiassets.calendar.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    void exposesProjectedCombinedCalendarDay() throws Exception {
        mvc.perform(get("/v1/calendar/dates/2025-10-06").param("region", "CN"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.date").value("2025-10-06"))
                .andExpect(jsonPath("$.regionCode").value("CN"))
                .andExpect(jsonPath("$.locale").value("zh-CN"))
                .andExpect(jsonPath("$.localeFallback").value(false))
                .andExpect(jsonPath("$.isHoliday").value(true))
                .andExpect(jsonPath("$.isStatutoryHoliday").value(true))
                .andExpect(jsonPath("$.lunar.month").value(8))
                .andExpect(jsonPath("$.lunar.day").value(15))
                .andExpect(jsonPath("$.ganZhi.yearName").value("乙巳"))
                .andExpect(jsonPath("$.festivals[0].code").value("MID_AUTUMN"))
                .andExpect(jsonPath("$.festivals[0].name").value("中秋节"))
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public,max-age=3600"));
    }

    @Test
    void supportsCollectionsMonthYearAndStatistics() throws Exception {
        mvc.perform(get("/v1/calendar/dates")
                        .param("from", "2025-10-01").param("to", "2025-10-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2025-10-01"))
                .andExpect(jsonPath("$.to").value("2025-10-08"))
                .andExpect(jsonPath("$.count").value(8))
                .andExpect(jsonPath("$.items.length()").value(8));

        mvc.perform(get("/v1/calendar/months/2024/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2024-02-01"))
                .andExpect(jsonPath("$.to").value("2024-02-29"))
                .andExpect(jsonPath("$.count").value(29));

        mvc.perform(get("/v1/calendar/years/2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-01-01"))
                .andExpect(jsonPath("$.count").value(365))
                .andExpect(jsonPath("$.items.length()").value(365));

        mvc.perform(get("/v1/calendar/workdays/count")
                        .param("from", "2026-01-01").param("to", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calendarDays").value(365))
                .andExpect(jsonPath("$.workdays", greaterThan(200)))
                .andExpect(jsonPath("$.nonWorkdays").isNumber())
                .andExpect(jsonPath("$.weekendDays").isNumber())
                .andExpect(jsonPath("$.statutoryHolidayDays").isNumber())
                .andExpect(jsonPath("$.adjustedWorkdays").isNumber());
    }

    @Test
    void mergesBatchRangesAndProjectsFields() throws Exception {
        String body = """
                {"region":"CN","locale":"en-US","fields":["holidayNames","festivals"],
                 "ranges":[{"from":"2025-01-01","to":"2025-01-03"},
                            {"from":"2025-01-03","to":"2025-01-05"},
                            {"from":"2025-01-10","to":"2025-01-10"}]}
                """;
        mvc.perform(post("/v1/calendar/dates:batch")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.region").value("CN"))
                .andExpect(jsonPath("$.locale").value("en-US"))
                .andExpect(jsonPath("$.ranges.length()").value(2))
                .andExpect(jsonPath("$.ranges[0].from").value("2025-01-01"))
                .andExpect(jsonPath("$.ranges[0].to").value("2025-01-05"))
                .andExpect(jsonPath("$.count").value(6))
                .andExpect(jsonPath("$.items.length()").value(6))
                .andExpect(jsonPath("$.items[0].holidayNames[0]").value("New Year's Day"))
                .andExpect(jsonPath("$.items[0].localeFallback").value(false))
                .andExpect(jsonPath("$.items[0].lunar").doesNotExist());
    }

    @Test
    void exposesHolidaySummaryRegionsAndConversions() throws Exception {
        mvc.perform(get("/v1/calendar/holidays").param("year", "2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2025))
                .andExpect(jsonPath("$.region").value("CN"))
                .andExpect(jsonPath("$.locale").value("zh-CN"))
                .andExpect(jsonPath("$.holidays").isArray())
                .andExpect(jsonPath("$.holidays[0].code").isString())
                .andExpect(jsonPath("$.holidays[0].daysOff").isArray())
                .andExpect(jsonPath("$.holidays[0].statutoryDates").isArray());

        mvc.perform(get("/v1/calendar/holidays/next").param("from", "2026-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-01-01"));

        mvc.perform(get("/v1/calendar/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regions[0].code").value("CN"))
                .andExpect(jsonPath("$.regions[0].locales[1]").value("en-US"))
                .andExpect(jsonPath("$.regions[0].holidays.startYear").value(2000));

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
    }

    @Test
    void rejectsInvalidRangesFieldsAndUnavailableData() throws Exception {
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

        mvc.perform(get("/v1/calendar/dates/2026-01-01").param("locale", "fr-FR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));

        mvc.perform(get("/v1/calendar/dates/2026-01-01").param("fields", "lunar,,labels"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));

        mvc.perform(get("/v1/calendar/dates/2026-01-01").param("regoin", "CN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));

        mvc.perform(post("/v1/calendar/dates:batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ranges\":[{\"from\":\"2025-01-01\",\"to\":\"2025-01-01\"}],\"unknown\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));

        mvc.perform(get("/v1/calendar/years/1999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CALENDAR_DATA_NOT_AVAILABLE"));

        mvc.perform(get("/v1/calendar/solar-terms/1900"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CALENDAR_DATA_NOT_AVAILABLE"));

        mvc.perform(get("/v1/calendar/holidays/next").param("from", "2026-12-31"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NO_FUTURE_HOLIDAY"));
    }

    @Test
    void enforcesBatch4096DayLimitAfterRangeNormalization() throws Exception {
        mvc.perform(post("/v1/calendar/dates:batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchBody(128)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(4096));
        mvc.perform(post("/v1/calendar/dates:batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchBody(129)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }

    @Test
    void servesVerifiedAssetsWithConditionalRequestsAndMetadata() throws Exception {
        mvc.perform(get("/v1/calendar/metadata"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public,max-age=60"))
                .andExpect(jsonPath("$.releaseVersion").value("1.0.0-rc.2"))
                .andExpect(jsonPath("$.breakingChange").value(true))
                .andExpect(jsonPath("$.holidays[0].startYear").value(2000));

        mvc.perform(get("/v1/calendar/assets/manifest"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.formatVersion").value(2))
                .andExpect(jsonPath("$.releaseVersion").value("1.0.0-rc.2"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public,max-age=60"));

        MvcResult result = mvc.perform(get("/v1/calendar/assets/calendar.cdat"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andExpect(header().exists("X-Checksum-SHA256"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public,max-age=86400"))
                .andReturn();
        byte[] expected = new ClassPathResource(
                "api-assets/calendar/calendar/calendar.cdat").getContentAsByteArray();
        String sha = sha256(expected);
        org.junit.jupiter.api.Assertions.assertArrayEquals(expected,
                result.getResponse().getContentAsByteArray());
        org.junit.jupiter.api.Assertions.assertEquals(sha,
                result.getResponse().getHeader("X-Checksum-SHA256"));
        mvc.perform(get("/v1/calendar/assets/calendar.cdat")
                        .header(HttpHeaders.IF_NONE_MATCH, result.getResponse().getHeader(HttpHeaders.ETAG)))
                .andExpect(status().isNotModified());

        mvc.perform(get("/v1/calendar/assets/holidays/CN/2026.hday"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"CN-2026.hday\""));
    }

    @Test
    void healthAndRequestIdWorkAndOldApiIsGone() throws Exception {
        mvc.perform(get("/internal/health/live").header("X-Request-Id", "test-request-1"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "test-request-1"))
                .andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/internal/health/ready"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataVersion").value("2026.GOV_NOTICE"));
        mvc.perform(get("/api/v1/day").param("date", "2025-01-01"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/internal/metrics"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("jvm_")));
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static String batchBody(int daysPerRange) {
        StringBuilder json = new StringBuilder("{\"ranges\":[");
        LocalDate start = LocalDate.of(2000, 1, 1);
        for (int i = 0; i < 32; i++) {
            if (i > 0) json.append(',');
            LocalDate end = start.plusDays(daysPerRange - 1L);
            json.append("{\"from\":\"").append(start)
                    .append("\",\"to\":\"").append(end).append("\"}");
            start = end.plusDays(1);
        }
        return json.append("]}").toString();
    }
}
