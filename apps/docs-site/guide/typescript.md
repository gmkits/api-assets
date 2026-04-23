# TypeScript 客户端

`@holiday/web-client` 是面向浏览器与 Node 的对外 HTTP 客户端，依赖 `@holiday/spec` 类型定义。

## 安装

```bash
pnpm add @holiday/web-client
```

## 选项

| 字段 | 说明 | 默认值 |
| --- | --- | --- |
| `baseUrl` | 服务端 URL | 必填 |
| `defaultRegion` | 默认地区代码 | `'CN'` |
| `fetchFn` | 自定义 fetch 实现（用于测试 / Node 18-） | `globalThis.fetch` |
| `timeoutMs` | 单次请求超时；`<=0` 关闭 | `5000` |
| `retry.maxRetries` | 5xx 重试次数 | `2` |
| `retry.baseDelayMs` | 指数退避基准 | `100` |
| `retry.retryStatuses` | 触发重试的状态码集合 | `[502, 503, 504]` |
| `cache.maxEntries` | 内存 LRU 容量 | `64` |
| `cache.ttlMs` | 内存条目 TTL | `60_000` |

## 取消请求

每个查询方法都接受 `RequestOptions { signal, bypassCache }`，与原生 `AbortController` 配合即可：

```ts
const ac = new AbortController();
setTimeout(() => ac.abort(), 1_000);
await client.getRange('2025-01-01', '2025-12-31', 'CN', { signal: ac.signal });
```

## 错误模型

非 2xx 响应抛出 `HolidayApiError`，保留状态码与响应体片段：

```ts
import { HolidayApiError } from '@holiday/web-client';

try {
  await client.getDayInfo('2099-01-01');
} catch (e) {
  if (e instanceof HolidayApiError) {
    console.error(e.status, e.body);
  }
}
```
