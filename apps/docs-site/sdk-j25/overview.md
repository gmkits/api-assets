# JDK 25 SDK 总览

`holiday-sdk-j25` 是面向 JDK 21+ 的纯 JDK 客户端：

- **零 Spring 依赖**：仅 `jackson-databind` + JDK 自带 `java.net.http.HttpClient`。
- **HTTP/2 + 虚拟线程**：每个 IO 等待挂在虚拟线程上，零系统线程消耗。
- **同步 / 异步 / 批量**三套 API 并存，业务可按需挑选。
- **Builder 配置**：endpoint / timeout / maxInflight / cache / userAgent。
- **轻量缓存**：`ConcurrentHashMap` + TTL，省去 Caffeine 依赖。

## API 映射

| SDK 方法 | 服务端端点 |
| --- | --- |
| `getDay(date)` | `GET /api/v2/day` |
| `getRange(region, from, to)` | `GET /api/v2/range` |
| `getYear(region, year)` | `GET /api/v2/year` |
| `getMonth(region, year, month)` | `GET /api/v2/month` |
| `countWorkdays(region, from, to)` | `GET /api/v2/workday-count` |
| `nextHoliday(region, from)` | `GET /api/v2/next-holiday` |
| `regions()` | `GET /api/v2/regions` |
| `version()` | `GET /api/v2/version` |
| `batchDays(dates)` | `POST /api/v2/days:batch` 或多次并行 `GET /api/v2/day` |

## 资源管理

`HolidayClient` 实现 `AutoCloseable`，建议用 try-with-resources 包裹：

```java
try (HolidayClient client = HolidayClient.builder()
        .endpoint("https://holiday.example.com")
        .build()) {
    // ...
}
```

`close()` 会关闭内部虚拟线程执行器。
