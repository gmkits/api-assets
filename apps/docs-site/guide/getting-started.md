# 快速开始

cn-holiday-kit 提供三条并行路径：**TypeScript**、**Java 8 兼容层**、**Java 25 高性能 SDK**。
所有路径背后都是同一份数据规范（`.hday` bundle）和同一份 OpenAPI。

## 5 分钟试一下 JDK 25 SDK

```xml
<dependency>
  <groupId>com.github.gmkits</groupId>
  <artifactId>holiday-sdk-j25</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

```java
import com.github.gmkits.holiday.sdk.j25.HolidayClient;
import java.time.LocalDate;

try (HolidayClient client = HolidayClient.builder()
        .endpoint("https://holiday.example.com")
        .build()) {
    // 同步
    var info = client.getDay(LocalDate.of(2025, 1, 1));

    // 异步
    var future = client.getDayAsync(LocalDate.of(2025, 1, 2));

    // 批量（虚拟线程并行 fan-out）
    var batch = client.batchDays(List.of(
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 5, 1),
            LocalDate.of(2025, 10, 1)));
}
```

> 详细字段语义参见 [JDK 25 SDK 总览](/sdk-j25/overview)。

## 5 分钟试一下 TypeScript Web Client

```bash
pnpm add @holiday/web-client
```

```ts
import { HolidayApiClient } from '@holiday/web-client';

const client = new HolidayApiClient({
  baseUrl: 'https://holiday.example.com',
  timeoutMs: 3_000,
  retry: { maxRetries: 2 },
  cache: { ttlMs: 60_000, maxEntries: 64 },
});

// 取消悬挂中的请求
const ac = new AbortController();
const day = await client.getDayInfo('2025-01-01', 'CN', { signal: ac.signal });
```

## 5 分钟试一下 Spring Boot 4 启动器

```yaml
holiday:
  api:
    default-region: CN
    preload-current-and-next-year: true
```

启动应用后访问 `http://localhost:8080/swagger-ui.html`，所有 `/api/v2/*` 端点开箱即用。
