# Java 8 兼容层

`/api/v1` 端点使用 Java 8 兼容层 (`holiday-api-j8`)，适配老旧 JVM 环境（Spring Boot 2.7、JDK 8/11）。

## 端点

| 路径 | 说明 |
| --- | --- |
| `GET /api/v1/day` | 单日查询 |
| `GET /api/v1/range` | 区间查询 |
| `GET /api/v1/year` | 整年查询 |
| `GET /api/v1/regions` | 支持地区列表 |
| `GET /api/v1/manifest` | manifest 全量 JSON |
| `GET /api/v1/bundle/{region}/{year}` | 下载 `.hday` 二进制 |

## 与 v2 的区别

`/api/v1` 字段直接来自 spec（`holiday`、`workday`、`weekend` 等布尔），
v2 在外层包裹了统一 `ApiResponse<T>` 信封并增加 `requestId`、`path`、`error` 字段，
便于灰度、审计、与多语言客户端统一。
