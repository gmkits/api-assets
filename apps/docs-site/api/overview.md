# API 总览

cn-holiday-kit 暴露两套 HTTP API：

| 路径前缀 | 模块 | 说明 |
| --- | --- | --- |
| `/api/v1` | `holiday-api-j8` | Java 8 兼容层，字段直接来自 `.hday` spec |
| `/api/v2` | `holiday-api-j25` | Java 25 高性能层，统一 `ApiResponse<T>` 信封 + ETag + 批量 |

## v2 端点速查

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v2/day` | 单日查询 |
| GET | `/api/v2/range` | 区间查询 |
| GET | `/api/v2/year` | 整年查询 |
| GET | `/api/v2/month` | 指定月份 |
| GET | `/api/v2/workday-count` | 区间工作日统计 |
| GET | `/api/v2/next-holiday` | 下一个法定节假日 |
| GET | `/api/v2/regions` | 支持地区列表 |
| GET | `/api/v2/version` | 版本信息（支持 ETag） |
| GET | `/api/v2/manifest` | manifest 全量 JSON（支持 ETag） |
| GET | `/api/v2/bundles/{regionCode}/{year}/metadata` | bundle 元信息 |
| **POST** | `/api/v2/days:batch` | **批量按日期查询（最多 100 个）** |
| POST | `/api/v2/ops/cache/clear` | 运维：清空缓存 |
| POST | `/api/v2/ops/cache/warmup` | 运维：预热 |
| POST | `/api/v2/ops/manifest/reload` | 运维：重载 manifest、bundle 和查询缓存 |

## 限流与审计

- `RateLimitFilter`：单机令牌桶限流（基于 IP / 路径），可配置阈值。
- `AuditLogFilter`：在请求/响应日志中输出 `requestId`、状态码、耗时。
- 客户端可通过 `X-Request-Id` 请求头注入自有 traceId，服务端会回写到响应。

## 条件请求

`/api/v2/manifest` 与 `/api/v2/version` 返回 `ETag`，并接受 `If-None-Match`：

```bash
$ curl -i 'https://holiday.example.com/api/v2/manifest' | grep -i etag
ETag: W/"a3f17b2c98d4e511"

$ curl -i -H 'If-None-Match: W/"a3f17b2c98d4e511"' 'https://holiday.example.com/api/v2/manifest'
HTTP/1.1 304 Not Modified
ETag: W/"a3f17b2c98d4e511"
```
