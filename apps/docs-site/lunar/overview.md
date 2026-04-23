# 农历 & 节气

`@holiday/lunar`（TS）与 `holiday-lunar-java`（Java）实现完全一致，可互相替换。

## 主要 API

| 函数 | 说明 |
| --- | --- |
| `solarToLunar(date)` | 公历 → 农历 |
| `lunarToSolar(year, month, day, isLeap)` | 农历 → 公历 |
| `getSolarTerms(year)` | 当年 24 节气 |
| `getSolarTerm(date)` | 指定日期所属节气（含 ±1 日） |
| `getZodiac(year)` | 年生肖 |
| `getGanZhi(year)` | 年干支 |

## 数据精度

- **农历**：1900–2100 共 200 年，覆盖闰月、月大小，单元测试以 4824 行 CSV 黄金集对齐 HKO 数据。
- **节气**：1900–2100 共 201 年（HKO 数据始于 1901，1900 用 VSOP87 估算补齐）。
- **存储**：节气使用 2-bit 偏移 + base-day 表，整张表约 1.2 KB；农历位压缩约 800 B。

## 示例

::: code-group

```ts [TypeScript]
import { solarToLunar, getSolarTerm } from '@holiday/lunar';

solarToLunar(new Date('2025-02-12'));   // → { year: 2025, month: 1, day: 15, isLeap: false }
getSolarTerm(new Date('2025-02-04'));   // → '立春'
```

```java [Java]
import com.github.gmkits.holiday.lunar.LunarCalendar;

var d = LunarCalendar.solarToLunar(LocalDate.of(2025, 2, 12));
// d.year() = 2025, d.month() = 1, d.day() = 15, d.isLeap() = false

LunarCalendar.getSolarTerm(LocalDate.of(2025, 2, 4));   // → "立春"
```

:::
