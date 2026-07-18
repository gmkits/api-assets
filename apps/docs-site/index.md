---
layout: home

hero:
  name: cn-holiday-kit
  text: 中国节假日数据平台
  tagline: 跨平台节假日工具集 · TypeScript · Java 8 · Java 25 · 农历 & 节气一应俱全
  actions:
    - theme: brand
      text: 5 分钟快速开始
      link: /guide/getting-started
    - theme: alt
      text: 在 GitHub 上查看
      link: https://github.com/gmkits/cn-holiday-kit

features:
  - icon: 🚀
    title: JDK 25 SDK
    details: HTTP/2 + 虚拟线程 + 结构化并发 fan-out。一个依赖，三行代码即可批量并行查询。
    link: /sdk-j25/overview
    linkText: 了解 SDK
  - icon: 🪶
    title: 极致紧凑
    details: 农历位压缩 ~800B，节气 2-bit 偏移压缩 ~1.2KB（覆盖 1900–2100），整体二进制 bundle <10KB / 区域 / 年。
    link: /lunar/data-format
    linkText: 数据格式
  - icon: 🧩
    title: 统一 API 设计
    details: TS / Java 8 / Java 25 三端共用同一份 OpenAPI 与 .hday 数据规范，保证字段口径一致。
    link: /api/overview
    linkText: API 参考
  - icon: 🛡️
    title: 内置防护
    details: 限流过滤器 · 审计日志 · 全局异常映射 · ETag 条件请求 · 请求级 AbortSignal。
    link: /api/response-model
    linkText: 响应模型
  - icon: ⚡️
    title: 缓存友好
    details: 服务端 Caffeine 本地缓存；TS Web Client 内置 in-flight dedup + LRU + 指数退避重试。
  - icon: 🌐
    title: 真正多端
    details: Browser / Node / JVM / Spring Boot 4 starter，按需挑选你的运行时。
---

## 三端示例

::: code-group

```ts [TypeScript]
import { HolidayApiClient } from '@holiday/web-client';

const client = new HolidayApiClient({ baseUrl: 'https://holiday.example.com' });
const today = await client.getDayInfo('2025-01-01', 'CN');
console.log(today.isStatutoryHoliday);
```

```java [Java 25 SDK]
try (HolidayClient client = HolidayClient.builder()
        .endpoint("https://holiday.example.com")
        .build()) {
    DayInfo today = client.getDay(LocalDate.of(2025, 1, 1));
    System.out.println(today.statutoryHoliday());
}
```

```bash [HTTP]
curl 'https://holiday.example.com/api/v2/day?date=2025-01-01&regionCode=CN'
```

:::
