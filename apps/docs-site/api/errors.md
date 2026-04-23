# 错误码

| HTTP 状态 | 错误码 | 说明 |
| --- | --- | --- |
| 400 | `INVALID_PARAMETER` | 参数校验失败（含 `@RegionCode` 不匹配） |
| 400 | `EMPTY_BATCH` | 批量端点 `dates` 为空 |
| 404 | `DAY_NOT_FOUND` | 指定日期无数据 |
| 404 | `YEAR_NOT_FOUND` | 指定年份无数据 |
| 404 | `BUNDLE_NOT_FOUND` | 指定 bundle 不存在 |
| 429 | `RATE_LIMITED` | 触发限流 |
| 500 | `INTERNAL_ERROR` | 服务端未捕获异常 |
| 503 | `BATCH_INTERRUPTED` | 批量查询被中断（虚拟线程被取消） |

## 客户端处理

```java
try {
    client.getDay(LocalDate.of(2099, 1, 1));
} catch (HolidayClientException e) {
    if (e.statusCode() == 404) { /* fallback */ }
    else if (e.statusCode() == 429) { /* backoff */ }
}
```

```ts
import { HolidayApiError } from '@holiday/web-client';

catch (err) {
  if (err instanceof HolidayApiError && err.status === 404) {
    return null;
  }
  throw err;
}
```
