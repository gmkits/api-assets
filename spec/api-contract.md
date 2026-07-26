# cn-holiday-kit HTTP API

仓库只保留一套无状态查询 API：`holiday-api-j8` 的 `/api/v1`。它编译为 Java 8
字节码，同一个可执行 JAR 可运行在 JDK 8、17、21、25。

所有日期使用 ISO `YYYY-MM-DD`，区域默认 `CN`。查询不到已安装年份时返回 404，
参数格式错误返回 400。

## 接口

| 方法 | 路径 | 参数 | 响应 |
| --- | --- | --- | --- |
| GET | `/api/v1/day` | `date`, `regionCode?` | `DayInfo` |
| GET | `/api/v1/range` | `from`, `to`, `regionCode?` | `DayInfo[]` |
| GET | `/api/v1/year` | `year`, `regionCode?` | `DayInfo[]` |
| GET | `/api/v1/regions` | 无 | `string[]` |
| GET | `/api/v1/version` | 无 | 版本与区域 |
| GET | `/api/v1/manifest` | 无 | bundle manifest |
| GET | `/api/v1/bundle/{region}/{year}` | 路径参数 | `.hday` 二进制 |

示例：

```http
GET /api/v1/day?date=2025-10-06&regionCode=CN
```

```json
{
  "date": "2025-10-06",
  "regionCode": "CN",
  "calendarSystem": "GREGORIAN",
  "holiday": true,
  "officialHoliday": true,
  "workday": false,
  "weekend": false,
  "statutoryHoliday": true,
  "adjustedWorkday": false,
  "holidayNames": {
    "zh-CN": ["中秋节"],
    "en-US": ["Mid-Autumn Festival"]
  },
  "labels": ["MID_AUTUMN"],
  "festivals": [
    {
      "code": "MID_AUTUMN",
      "names": {
        "zh-CN": "中秋节",
        "en-US": "Mid-Autumn Festival"
      }
    }
  ],
  "sourceVersion": "2025.GOV_NOTICE",
  "extensions": {
    "lunar": {
      "year": 2025,
      "month": 8,
      "day": 15,
      "leapMonth": false,
      "ganZhiYear": "乙巳年",
      "shengXiao": "蛇",
      "monthName": "八月",
      "dayName": "十五"
    }
  }
}
```

Java Bean 序列化字段使用 `holiday/officialHoliday/workday/...`；通用 JSON Schema
使用 `isHoliday/isOfficialHoliday/isWorkday/...`。TypeScript Web 适配器负责把前者
归一化为后者。

错误响应：

```json
{
  "status": 400,
  "message": "参数值无效",
  "timestamp": "2026-07-26T12:00:00"
}
```

API 没有数据库和远程依赖。替换外部 `data/date-assets` 可以更新数据而不修改接口；
节假日 bundle 缓存可通过库内 `HolidayService.clearCache()` 重载。
