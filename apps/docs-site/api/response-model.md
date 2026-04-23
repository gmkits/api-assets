# 统一响应模型

`/api/v2` 所有端点都返回统一信封 `ApiResponse<T>`：

```json
{
  "success": true,
  "data": { /* 业务数据 */ },
  "error": null,
  "requestId": "trace-abc-123",
  "path": "/api/v2/day",
  "timestamp": "2025-01-01T00:00:00Z"
}
```

| 字段 | 说明 |
| --- | --- |
| `success` | 是否成功 |
| `data` | 业务数据；失败时为 `null` |
| `error` | `ApiErrorResponse`：包含 `code` 与 `message`；成功时为 `null` |
| `requestId` | 请求级 traceId，与请求头 `X-Request-Id` 联动 |
| `path` | 请求路径 |
| `timestamp` | ISO8601 时间戳（UTC） |

## 批量响应

`POST /api/v2/days:batch` 返回 `List<Item>`：

```json
{
  "success": true,
  "data": [
    { "date": "2025-01-01", "data": { "...": "..." }, "error": null },
    { "date": "2025-05-01", "data": { "...": "..." }, "error": null },
    { "date": "9999-12-31", "data": null, "error": "DAY_NOT_FOUND" }
  ]
}
```

> 单个子查询失败不会影响其它子查询，已用 JDK 25 虚拟线程并行执行。
