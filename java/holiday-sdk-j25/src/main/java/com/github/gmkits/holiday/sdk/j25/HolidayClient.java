package com.github.gmkits.holiday.sdk.j25;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

/**
 * cn-holiday-kit 的 Java 25 SDK 主门面。
 *
 * <p>对外提供同步 + 异步两套查询 API；内部 HTTP/2 客户端使用虚拟线程
 * 执行器，IO 阻塞不会消耗系统线程。可选轻量内存缓存（基于
 * {@link ConcurrentHashMap}）+ {@link Semaphore} 限并发，避免突发流量
 * 击穿后端。</p>
 *
 * <h2>典型使用</h2>
 * <pre>{@code
 * HolidayClient client = HolidayClient.builder()
 *         .endpoint("https://holiday.example.com")
 *         .timeout(Duration.ofSeconds(3))
 *         .maxInflight(64)
 *         .build();
 * DayInfo info = client.getDay(LocalDate.of(2025, 1, 1));        // 同步
 * CompletableFuture<DayInfo> future = client.getDayAsync(d);     // 异步
 * List<BatchItem> items = client.batchDays(List.of(d1, d2, d3)); // 虚拟线程并行
 * }</pre>
 */
public final class HolidayClient implements AutoCloseable {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final URI endpoint;
    private final String defaultRegion;
    private final Duration timeout;
    private final String userAgent;
    private final HttpClient httpClient;
    private final ExecutorService asyncExecutor;
    private final ExecutorService batchExecutor;
    private final Semaphore inflightSemaphore;
    private final ConcurrentHashMap<String, CacheEntry> cache;
    private final long cacheTtlMillis;
    private final int cacheMaxEntries;
    private final boolean ownsExecutors;

    private HolidayClient(Builder b) {
        this.endpoint = Objects.requireNonNull(b.endpoint, "endpoint");
        this.defaultRegion = b.defaultRegion;
        this.timeout = b.timeout;
        this.userAgent = b.userAgent;
        this.cacheTtlMillis = b.cacheTtl == null ? 0 : b.cacheTtl.toMillis();
        this.cacheMaxEntries = b.cacheMaxEntries;
        this.cache = cacheTtlMillis > 0 && cacheMaxEntries > 0 ? new ConcurrentHashMap<>() : null;
        this.inflightSemaphore = b.maxInflight > 0 ? new Semaphore(b.maxInflight, true) : null;
        this.ownsExecutors = true;
        // Per-task virtual threads: blocking HTTP IO without OS-thread cost.
        this.asyncExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.batchExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(timeout)
                .executor(this.asyncExecutor)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void close() {
        if (!ownsExecutors) return;
        asyncExecutor.shutdown();
        batchExecutor.shutdown();
    }

    // ---------------------------------------------------------------------
    // Sync API
    // ---------------------------------------------------------------------

    public DayInfo getDay(LocalDate date) {
        return getDay(defaultRegion, date);
    }

    public DayInfo getDay(String regionCode, LocalDate date) {
        Objects.requireNonNull(regionCode, "regionCode");
        Objects.requireNonNull(date, "date");
        String path = "/api/v2/day?date=" + enc(date.format(DATE_FMT))
                + "&regionCode=" + enc(regionCode);
        return parseDayInfoFromApiResponse(getJson(path));
    }

    public List<DayInfo> getRange(String regionCode, LocalDate from, LocalDate to) {
        Objects.requireNonNull(regionCode);
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);
        String path = "/api/v2/range?from=" + enc(from.format(DATE_FMT))
                + "&to=" + enc(to.format(DATE_FMT))
                + "&regionCode=" + enc(regionCode);
        return parseDayInfoArrayFromApiResponse(getJson(path));
    }

    public List<DayInfo> getYear(String regionCode, int year) {
        String path = "/api/v2/year?year=" + year + "&regionCode=" + enc(regionCode);
        return parseDayInfoArrayFromApiResponse(getJson(path));
    }

    public List<DayInfo> getMonth(String regionCode, int year, int month) {
        String path = "/api/v2/month?year=" + year + "&month=" + month
                + "&regionCode=" + enc(regionCode);
        return parseDayInfoArrayFromApiResponse(getJson(path));
    }

    public long countWorkdays(String regionCode, LocalDate from, LocalDate to) {
        String path = "/api/v2/workday-count?from=" + enc(from.format(DATE_FMT))
                + "&to=" + enc(to.format(DATE_FMT))
                + "&regionCode=" + enc(regionCode);
        JsonNode payload = getJson(path).path("data");
        return payload.path("workdayCount").asLong();
    }

    public Optional<DayInfo> nextHoliday(String regionCode, LocalDate from) {
        String path = "/api/v2/next-holiday?from=" + enc(from.format(DATE_FMT))
                + "&regionCode=" + enc(regionCode);
        JsonNode root = getJson(path);
        JsonNode data = root.path("data");
        if (data.isMissingNode() || data.isNull()) return Optional.empty();
        return Optional.of(parseDayInfo(data));
    }

    public List<String> regions() {
        JsonNode root = getJson("/api/v2/regions");
        List<String> regions = new ArrayList<>();
        for (JsonNode entry : root.path("data")) {
            regions.add(entry.path("regionCode").asText());
        }
        return List.copyOf(regions);
    }

    public Map<String, Object> version() {
        JsonNode root = getJson("/api/v2/version");
        try {
            return MAPPER.convertValue(root.path("data"), new TypeReference<>() {});
        } catch (IllegalArgumentException ex) {
            throw new HolidayClientException("解析 version 响应失败: " + ex.getMessage(), ex);
        }
    }

    // ---------------------------------------------------------------------
    // Async API
    // ---------------------------------------------------------------------

    public CompletableFuture<DayInfo> getDayAsync(LocalDate date) {
        return getDayAsync(defaultRegion, date);
    }

    public CompletableFuture<DayInfo> getDayAsync(String regionCode, LocalDate date) {
        return supplyAsync(() -> getDay(regionCode, date));
    }

    public CompletableFuture<List<DayInfo>> getYearAsync(String regionCode, int year) {
        return supplyAsync(() -> getYear(regionCode, year));
    }

    public CompletableFuture<List<DayInfo>> getRangeAsync(String regionCode, LocalDate from, LocalDate to) {
        return supplyAsync(() -> getRange(regionCode, from, to));
    }

    // ---------------------------------------------------------------------
    // Structured-concurrency-style batch
    // ---------------------------------------------------------------------

    /**
     * 单条批量结果。{@code data} 与 {@code error} 互斥：成功时 {@code error == null}。
     */
    public record BatchItem(LocalDate date, DayInfo data, String error) {
        public boolean isSuccess() {
            return error == null;
        }
    }

    /**
     * 在虚拟线程上并行查询多个日期；任一子任务失败不影响其它结果。
     *
     * <p>当 {@code java.util.concurrent.StructuredTaskScope} 在未来 JDK 版本
     * 转正后，可平滑替换为 {@code StructuredTaskScope.open(Joiner.awaitAll())}
     * 形态。当前实现等价（per-task 虚拟线程 + {@code invokeAll}）。</p>
     */
    public List<BatchItem> batchDays(List<LocalDate> dates) {
        return batchDays(defaultRegion, dates);
    }

    public List<BatchItem> batchDays(String regionCode, List<LocalDate> dates) {
        Objects.requireNonNull(regionCode);
        Objects.requireNonNull(dates);
        if (dates.isEmpty()) return List.of();

        List<Callable<DayInfo>> tasks = new ArrayList<>(dates.size());
        for (LocalDate d : dates) {
            tasks.add(() -> getDay(regionCode, d));
        }

        List<Future<DayInfo>> futures;
        try {
            futures = batchExecutor.invokeAll(tasks);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new HolidayClientException("批量查询被中断", ex);
        }

        List<BatchItem> items = new ArrayList<>(dates.size());
        for (int i = 0; i < dates.size(); i++) {
            LocalDate date = dates.get(i);
            Future<DayInfo> f = futures.get(i);
            try {
                items.add(new BatchItem(date, f.get(), null));
            } catch (CancellationException ex) {
                items.add(new BatchItem(date, null, "已取消"));
            } catch (ExecutionException ex) {
                Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                items.add(new BatchItem(date, null, cause.getMessage()));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                items.add(new BatchItem(date, null, "中断"));
            }
        }
        return List.copyOf(items);
    }

    // ---------------------------------------------------------------------
    // HTTP
    // ---------------------------------------------------------------------

    private JsonNode getJson(String path) {
        if (cache != null) {
            CacheEntry hit = cache.get(path);
            long now = System.currentTimeMillis();
            if (hit != null && hit.expiresAt > now) {
                return hit.value;
            }
            if (hit != null) {
                cache.remove(path, hit);
            }
        }
        boolean acquired = false;
        try {
            if (inflightSemaphore != null) {
                inflightSemaphore.acquire();
                acquired = true;
            }
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(endpoint.resolve(path))
                    .timeout(timeout)
                    .header("Accept", "application/json")
                    .header("User-Agent", userAgent)
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, BodyHandlers.ofString(StandardCharsets.UTF_8));
            int code = resp.statusCode();
            if (code / 100 != 2) {
                throw new HolidayClientException("HTTP " + code + " " + truncate(resp.body(), 256),
                        code, truncate(resp.body(), 1024));
            }
            JsonNode tree;
            try {
                tree = MAPPER.readTree(resp.body());
            } catch (IOException ex) {
                throw new HolidayClientException("解析响应失败: " + ex.getMessage(), ex);
            }
            if (cache != null) {
                if (cache.size() >= cacheMaxEntries) {
                    // Cheap eviction: drop a single entry.  Strict LRU not needed at SDK scope.
                    java.util.Iterator<String> it = cache.keySet().iterator();
                    if (it.hasNext()) {
                        cache.remove(it.next());
                    }
                }
                cache.put(path, new CacheEntry(System.currentTimeMillis() + cacheTtlMillis, tree));
            }
            return tree;
        } catch (IOException ex) {
            throw new HolidayClientException("网络错误: " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new HolidayClientException("请求被中断", ex);
        } finally {
            if (acquired) inflightSemaphore.release();
        }
    }

    private <T> CompletableFuture<T> supplyAsync(java.util.function.Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, asyncExecutor);
    }

    // ---------------------------------------------------------------------
    // Parsing helpers
    // ---------------------------------------------------------------------

    private static DayInfo parseDayInfoFromApiResponse(JsonNode root) {
        JsonNode data = root.path("data");
        if (data.isMissingNode() || data.isNull()) {
            throw new HolidayClientException("响应缺少 data 字段");
        }
        return parseDayInfo(data);
    }

    private static List<DayInfo> parseDayInfoArrayFromApiResponse(JsonNode root) {
        JsonNode data = root.path("data");
        if (!data.isArray()) {
            throw new HolidayClientException("响应 data 不是数组");
        }
        List<DayInfo> list = new ArrayList<>(data.size());
        for (JsonNode e : data) list.add(parseDayInfo(e));
        return List.copyOf(list);
    }

    private static DayInfo parseDayInfo(JsonNode node) {
        return new DayInfo(
                node.path("date").asText(),
                node.path("regionCode").asText(),
                node.path("calendarSystem").asText(""),
                readBool(node, "isHoliday", "holiday"),
                readBool(node, "isWorkday", "workday"),
                readBool(node, "isWeekend", "weekend"),
                readBool(node, "isStatutoryHoliday", "statutoryHoliday"),
                readBool(node, "isAdjustedWorkday", "adjustedWorkday"),
                readNames(node.path("holidayNames")),
                readStringList(node.path("labels")),
                node.path("sourceVersion").asText(""),
                readMap(node.path("extensions"))
        );
    }

    private static boolean readBool(JsonNode node, String primary, String fallback) {
        JsonNode v = node.path(primary);
        if (v.isBoolean()) return v.asBoolean();
        JsonNode f = node.path(fallback);
        return f.asBoolean(false);
    }

    private static Map<String, List<String>> readNames(JsonNode node) {
        if (node == null || !node.isObject() || node.isEmpty()) return Map.of();
        java.util.LinkedHashMap<String, List<String>> map = new java.util.LinkedHashMap<>();
        node.fields().forEachRemaining(e -> map.put(e.getKey(), readStringList(e.getValue())));
        return Collections.unmodifiableMap(map);
    }

    private static List<String> readStringList(JsonNode node) {
        if (node == null || !node.isArray() || node.isEmpty()) return List.of();
        List<String> list = new ArrayList<>(node.size());
        for (JsonNode e : node) list.add(e.asText());
        return List.copyOf(list);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readMap(JsonNode node) {
        if (node == null || !node.isObject() || node.isEmpty()) return Map.of();
        try {
            return Collections.unmodifiableMap((Map<String, Object>) MAPPER.convertValue(node, Map.class));
        } catch (IllegalArgumentException ex) {
            return Map.of();
        }
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }

    private record CacheEntry(long expiresAt, JsonNode value) {}

    // ---------------------------------------------------------------------
    // Builder
    // ---------------------------------------------------------------------

    public static final class Builder {
        private URI endpoint;
        private String defaultRegion = "CN";
        private Duration timeout = Duration.ofSeconds(5);
        private int maxInflight = 0;
        private Duration cacheTtl = Duration.ofSeconds(60);
        private int cacheMaxEntries = 128;
        private String userAgent = "cn-holiday-kit-sdk-j25/1.0";

        private Builder() {}

        public Builder endpoint(String url) {
            this.endpoint = URI.create(url);
            return this;
        }

        public Builder endpoint(URI uri) {
            this.endpoint = uri;
            return this;
        }

        public Builder defaultRegion(String regionCode) {
            this.defaultRegion = Objects.requireNonNull(regionCode);
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = Objects.requireNonNull(timeout);
            return this;
        }

        /** 0 表示不限制；&gt;0 时使用 {@link Semaphore} 限制并发请求数。 */
        public Builder maxInflight(int maxInflight) {
            this.maxInflight = maxInflight;
            return this;
        }

        public Builder cache(Duration ttl, int maxEntries) {
            this.cacheTtl = ttl;
            this.cacheMaxEntries = maxEntries;
            return this;
        }

        /** 关闭内存缓存。 */
        public Builder disableCache() {
            this.cacheTtl = Duration.ZERO;
            this.cacheMaxEntries = 0;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = Objects.requireNonNull(userAgent);
            return this;
        }

        public HolidayClient build() {
            if (endpoint == null) {
                throw new IllegalStateException("必须设置 endpoint(...)");
            }
            return new HolidayClient(this);
        }
    }
}
