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
  "isHoliday": true,
  "isOfficialHoliday": true,
  "isWorkday": false,
  "isWeekend": false,
  "isStatutoryHoliday": true,
  "isAdjustedWorkday": false,
  "holidayNames": {
    "zh-CN": ["中秋节"],
    "en-US": ["Mid-Autumn Festival"]
  },
  "labels": ["MID_AUTUMN"],
  "lunar": {
    "year": 2025,
    "month": 8,
    "day": 15,
    "leapMonth": false,
    "monthName": "八月",
    "dayName": "十五"
  },
  "solarTerm": null,
  "ganZhi": {
    "yearName": "乙巳",
    "heavenlyStem": "乙",
    "earthlyBranch": "巳",
    "zodiac": "蛇"
  },
  "festivals": [
    {
      "code": "MID_AUTUMN",
      "names": {
        "zh-CN": "中秋节",
        "en-US": "Mid-Autumn Festival"
      }
    }
  ],
  "sourceVersion": "2025.GOV_NOTICE"
}
```

Java、HTTP、TypeScript 与通用 JSON Schema 统一使用
`isHoliday/isOfficialHoliday/isWorkday/...`。农历、节气、干支和节日均是
`DayInfo` 一级字段，不需要读取弱类型扩展映射。

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
