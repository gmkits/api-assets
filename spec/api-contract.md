# cn-holiday-kit HTTP API 合同

本文档描述仓库当前已经落地的 HTTP 接口，**以 `holiday-api-j25` 的 `/api/v2` 为主**，并补充 `holiday-api-j8` 的 `/api/v1` 兼容接口差异。

## 1. 通用约定

- 所有日期字段均使用 `YYYY-MM-DD`
- 所有枚举值均使用 `UPPER_SNAKE_CASE`
- `/api/v2` 统一返回包装响应；`/api/v1` 直接返回业务对象
- 当前 HTTP JSON 字段名以 Java 序列化结果为准，例如 `holiday/workday/weekend/leapMonth`

## 2. `/api/v2` 主接口（holiday-api-j25）

### 2.1 成功响应

```json
{
  "success": true,
  "timestamp": "2026-01-01T08:00:00Z",
  "requestId": "1c8a4f3d-0d2f-4e2b-9c0f-d5d9f7a3a6a1",
  "path": "/api/v2/day",
  "data": {}
}
```

### 2.2 错误响应

```json
{
  "success": false,
  "timestamp": "2026-01-01T08:00:00Z",
  "requestId": "1c8a4f3d-0d2f-4e2b-9c0f-d5d9f7a3a6a1",
  "path": "/api/v2/day",
  "error": {
    "code": "BAD_REQUEST",
    "message": "请求参数非法",
    "details": {}
  }
}
```

### 2.3 `DayInfo` JSON 结构

`/api/v2/day` 的 `data` 字段，以及 `/api/v2/range`、`/api/v2/month`、`/api/v2/year` 的数组元素，均使用下面的结构：

```json
{
  "date": "2025-02-03",
  "regionCode": "CN",
  "calendarSystem": "GREGORIAN",
  "holiday": false,
  "workday": true,
  "weekend": false,
  "statutoryHoliday": false,
  "adjustedWorkday": false,
  "holidayNames": {},
  "labels": [],
  "sourceVersion": "2025.01.01",
  "extensions": {
    "lunar": {
      "year": 2025,
      "month": 1,
      "day": 6,
      "leapMonth": false,
      "ganZhiYear": "乙巳年",
      "shengXiao": "蛇",
      "monthName": "正月",
      "dayName": "初六"
    },
    "solarTerm": {
      "index": 2,
      "name": "立春"
    }
  }
}
```

### 2.4 查询接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/v2/day` | 查询单日 |
| `GET` | `/api/v2/range` | 查询闭区间 |
| `GET` | `/api/v2/year` | 查询整年 |
| `GET` | `/api/v2/month` | 查询指定月份 |
| `GET` | `/api/v2/workday-count` | 统计区间工作日 |
| `GET` | `/api/v2/next-holiday` | 查询下一个法定节假日 |
| `GET` | `/api/v2/regions` | 查询支持地区 |
| `GET` | `/api/v2/version` | 查询版本信息 |
| `GET` | `/api/v2/manifest` | 读取 manifest 快照 |
| `GET` | `/api/v2/bundles/{regionCode}/{year}/metadata` | 查询 bundle 元信息 |

### 2.5 运维接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/v2/ops/cache/clear` | 清理缓存 |
| `POST` | `/api/v2/ops/cache/warmup` | 预热缓存 |
| `POST` | `/api/v2/ops/manifest/reload` | 重载 manifest |

### 2.6 常用参数

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `regionCode` | `string` | 地区代码，默认 `CN` |
| `date` | `string` | 单日查询日期 |
| `from` / `to` | `string` | 区间起止日期，闭区间 |
| `year` | `number` | 目标年份 |
| `month` | `number` | 目标月份，`1-12` |

### 2.7 主要业务对象

#### `VersionPayload`

```json
{
  "apiVersion": "2.0.0",
  "specVersion": "1.0.0",
  "bundleFormatVersion": "1.0.0",
  "publishedAt": "2025-01-01T00:00:00+08:00",
  "regions": ["CN"]
}
```

#### `WorkdayCountPayload`

```json
{
  "from": "2025-10-01",
  "to": "2025-10-31",
  "workdays": 18,
  "totalDays": 31,
  "holidays": 13
}
```

#### `BundleMetadataPayload`

```json
{
  "regionCode": "CN",
  "year": 2025,
  "file": "CN/2025.hday",
  "sha256": "...",
  "crc32": "...",
  "sourceVersion": "2025.01.01",
  "size": 12345
}
```

## 3. `/api/v1` 兼容接口（holiday-api-j8）

`holiday-api-j8` 保留基础查询与 bundle 下载能力，响应不包 `success/data` 外层。

### 3.1 接口列表

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/v1/day` | 查询单日 |
| `GET` | `/api/v1/range` | 查询区间 |
| `GET` | `/api/v1/year` | 查询整年 |
| `GET` | `/api/v1/regions` | 查询地区列表 |
| `GET` | `/api/v1/version` | 查询版本信息 |
| `GET` | `/api/v1/manifest` | 读取 manifest |
| `GET` | `/api/v1/bundle/{region}/{year}` | 下载 `.hday` bundle |

### 3.2 与 `/api/v2` 的差异

1. `/api/v1/day`、`/range`、`/year` 直接返回业务对象或数组，不包裹 `success/data`
2. 布尔字段同样使用 `holiday/workday/weekend/...`
3. `extensions.lunar` 同样使用 `leapMonth`
4. `/api/v1` 不提供月份查询、工作日统计、下个假期与运维接口

## 4. 扩展字段约定

| 键名 | 类型 | 说明 |
| --- | --- | --- |
| `extensions.lunar` | `LunarDateInfo` | 当前日期对应的农历信息 |
| `extensions.solarTerm` | `SolarTermInfo` | 当天命中的节气信息；非节气日不返回 |

节气与农历数据均基于香港天文台（HKO）1901-2100 数据生成的基线文件校验。
