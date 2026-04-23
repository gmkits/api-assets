package com.github.gmkits.holiday.sdk.j25;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link HolidayClient} 端到端测试，使用 JDK 内置 {@link HttpServer} 启动一个本地桩。
 */
class HolidayClientTest {

    private HttpServer server;
    private final ConcurrentHashMap<String, AtomicInteger> hits = new ConcurrentHashMap<>();
    private final AtomicLong batchActiveMax = new AtomicLong(0);
    private final AtomicLong batchActive = new AtomicLong(0);
    private int port;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/api/v2/day", new DayHandler());
        server.createContext("/api/v2/regions", new SimpleHandler(
                "{\"success\":true,\"data\":[{\"regionCode\":\"CN\"},{\"regionCode\":\"HK\"}]}"));
        server.createContext("/api/v2/version", new SimpleHandler(
                "{\"success\":true,\"data\":{\"apiVersion\":\"2.0.0\",\"specVersion\":\"1.0.0\"}}"));
        server.start();
        port = server.getAddress().getPort();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void getDay_shouldDeserialize() {
        try (HolidayClient client = newClient()) {
            DayInfo info = client.getDay(LocalDate.of(2025, 1, 1));
            assertEquals("2025-01-01", info.date());
            assertEquals("CN", info.regionCode());
            assertTrue(info.holiday());
            assertFalse(info.workday());
        }
    }

    @Test
    void cache_shouldDedupRequests() {
        try (HolidayClient client = HolidayClient.builder()
                .endpoint("http://127.0.0.1:" + port)
                .timeout(Duration.ofSeconds(2))
                .cache(Duration.ofSeconds(60), 16)
                .build()) {
            client.regions();
            client.regions();
            client.regions();
            assertEquals(1, hits.get("/api/v2/regions").get());
        }
    }

    @Test
    void disableCache_shouldHitEveryTime() {
        try (HolidayClient client = HolidayClient.builder()
                .endpoint("http://127.0.0.1:" + port)
                .timeout(Duration.ofSeconds(2))
                .disableCache()
                .build()) {
            client.regions();
            client.regions();
            assertEquals(2, hits.get("/api/v2/regions").get());
        }
    }

    @Test
    void batchDays_shouldFanOutOnVirtualThreads() {
        try (HolidayClient client = newClient()) {
            List<LocalDate> dates = List.of(
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 1, 2),
                    LocalDate.of(2025, 5, 1),
                    LocalDate.of(2025, 10, 1)
            );
            List<HolidayClient.BatchItem> items = client.batchDays("CN", dates);
            assertEquals(4, items.size());
            for (HolidayClient.BatchItem item : items) {
                assertTrue(item.isSuccess(), "failed: " + item.error());
                assertNotNull(item.data());
            }
            // Concurrency observed: at least 2 (virtual threads should overlap).
            assertTrue(batchActiveMax.get() >= 2,
                    "expected batchExecutor to overlap at least 2 tasks; got " + batchActiveMax.get());
        }
    }

    @Test
    void getDayAsync_shouldCompleteOnVirtualThread() throws Exception {
        try (HolidayClient client = newClient()) {
            DayInfo info = client.getDayAsync(LocalDate.of(2025, 1, 1)).get();
            assertEquals("2025-01-01", info.date());
        }
    }

    @Test
    void httpError_shouldRaiseHolidayClientException() {
        try (HolidayClient client = newClient()) {
            HolidayClientException ex = assertThrows(HolidayClientException.class,
                    () -> client.getDay("CN", LocalDate.of(1900, 1, 1)));
            assertEquals(404, ex.statusCode());
            assertNotNull(ex.body());
        }
    }

    @Test
    void version_shouldReturnMap() {
        try (HolidayClient client = newClient()) {
            Map<String, Object> v = client.version();
            assertEquals("2.0.0", v.get("apiVersion"));
        }
    }

    @Test
    void builder_endpointRequired() {
        assertThrows(IllegalStateException.class, () -> HolidayClient.builder().build());
    }

    private HolidayClient newClient() {
        return HolidayClient.builder()
                .endpoint("http://127.0.0.1:" + port)
                .timeout(Duration.ofSeconds(2))
                .disableCache()
                .build();
    }

    // -----------------------------------------------------------------
    // Test handlers
    // -----------------------------------------------------------------

    private final class DayHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            hits.computeIfAbsent("/api/v2/day", k -> new AtomicInteger()).incrementAndGet();
            String query = exchange.getRequestURI().getRawQuery();
            String date = paramFromQuery(query, "date");
            // Track concurrency for the batch fan-out test.
            long active = batchActive.incrementAndGet();
            batchActiveMax.accumulateAndGet(active, Math::max);
            try {
                Thread.sleep(80); // give peers a chance to overlap
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } finally {
                batchActive.decrementAndGet();
            }
            if ("1900-01-01".equals(date)) {
                respond(exchange, 404, "{\"success\":false,\"error\":{\"code\":\"DAY_NOT_FOUND\"}}");
                return;
            }
            String json = "{\"success\":true,\"data\":{"
                    + "\"date\":\"" + date + "\","
                    + "\"regionCode\":\"CN\","
                    + "\"calendarSystem\":\"GREGORIAN\","
                    + "\"holiday\":true,"
                    + "\"workday\":false,"
                    + "\"weekend\":false,"
                    + "\"statutoryHoliday\":true,"
                    + "\"adjustedWorkday\":false,"
                    + "\"holidayNames\":{\"zh-CN\":[\"测试\"]},"
                    + "\"labels\":[\"TEST\"],"
                    + "\"sourceVersion\":\"2025.01.01\","
                    + "\"extensions\":{}}}";
            respond(exchange, 200, json);
        }
    }

    private final class SimpleHandler implements HttpHandler {
        private final String body;

        SimpleHandler(String body) {
            this.body = body;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            hits.computeIfAbsent(exchange.getHttpContext().getPath(), k -> new AtomicInteger())
                    .incrementAndGet();
            respond(exchange, 200, body);
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String paramFromQuery(String query, String name) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0 && pair.substring(0, idx).equals(name)) {
                return java.net.URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
