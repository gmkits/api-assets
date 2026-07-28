# cn-holiday-kit 枚举与常量字典

本文档整理仓库中已经使用的核心枚举、`.hday` 二进制常量与位标记。

## 1. DayKind

| 值 | 含义 |
| --- | --- |
| `STATUTORY_HOLIDAY` | 法定节假日 |
| `OFFICIAL_HOLIDAY` | 当年放假安排中的休假日 |
| `ADJUSTED_WORKDAY` | 调休补班日 |
| `NORMAL_WORKDAY` | 普通工作日 |
| `NORMAL_WEEKEND` | 普通周末 |

## 2. RuleType

| 值 | 含义 |
| --- | --- |
| `FIXED_DATE` | 固定日期 |
| `DATE_RANGE` | 连续日期区间 |
| `WEEKDAY_OVERRIDE` | 指定星期覆盖规则 |
| `LUNAR_DATE` | 农历日期规则 |
| `RECURRENCE` | 周期性规则 |
| `PATCH` | 组织内部补丁规则 |

## 3. SourceType

| 值 | 含义 |
| --- | --- |
| `GOV_NOTICE` | 政府公告 |
| `ICS_FEED` | iCalendar/ICS 数据源 |
| `THIRD_PARTY_JSON` | 第三方 JSON 数据 |
| `CSV_IMPORT` | CSV/Excel 导入 |
| `MANUAL_ENTRY` | 人工录入 |
| `ENTERPRISE_PATCH` | 企业自定义补丁 |

## 4. CalendarSystem

| 值 | 含义 |
| --- | --- |
| `GREGORIAN` | 公历 |
| `CHINESE_LUNAR` | 农历 |

## 5. HolidayLabel

| 值 | 含义 |
| --- | --- |
| `NEW_YEAR` | 元旦 |
| `SPRING_FESTIVAL` | 春节 |
| `TOMB_SWEEPING` | 清明节 |
| `LABOUR_DAY` | 劳动节 |
| `DRAGON_BOAT` | 端午节 |
| `MID_AUTUMN` | 中秋节 |
| `NATIONAL_DAY` | 国庆节 |
| `STATUTORY` | 法定节假日标记 |
| `ADJUSTED_WORKDAY` | 调休补班标记 |
| `BRIDGE_DAY` | 桥接假日 |

## 6. WeekDay

| 值 | 含义 |
| --- | --- |
| `MON` | 星期一 |
| `TUE` | 星期二 |
| `WED` | 星期三 |
| `THU` | 星期四 |
| `FRI` | 星期五 |
| `SAT` | 星期六 |
| `SUN` | 星期日 |

## 7. `.hday` 二进制常量

### 7.1 CalendarSystem 数值编码

| 编码 | 枚举值 |
| --- | --- |
| `0x00` | `GREGORIAN` |
| `0x01` | `CHINESE_LUNAR` |
| `0x02` - `0xFF` | 预留 |

### 7.2 Section 类型

| 编码 | 名称 | 说明 |
| --- | --- | --- |
| `0x0001` | `DAY_OVERRIDES` | 稀疏日期覆盖表 |
| `0x0002` | `STRING_TABLE` | 字符串池 |
| `0x0003` | `NAME_LIST_TABLE` | 名称/标签列表 |
| `0x0004` | `META_TABLE` | 可跳过未知键的元数据表 |
| `0x0005` - `0xFFFF` | 预留 | 未来扩展 |

### 7.3 Section flags

| 位 | 掩码 | 名称 | 说明 |
| --- | --- | --- | --- |
| 0 | `0x0001` | `CRITICAL` | 未识别此 section 时必须拒绝文件 |
| 1 - 15 | `0xFFFE` | 预留 | 当前必须为 0 |

### 7.4 DAY_OVERRIDES 状态位

| 位 | 掩码 | 名称 | 说明 |
| --- | --- | --- | --- |
| 0 | `0x01` | `FORCE_HOLIDAY` | 强制休息 |
| 1 | `0x02` | `FORCE_WORKDAY` | 强制工作 |
| 2 | `0x04` | `STATUTORY_HOLIDAY` | 法定节假日 |
| 3 | `0x08` | `ADJUSTED_WORKDAY` | 调休补班 |
| 4 - 7 | `0xF0` | 预留 | 当前必须为 0 |

## 8. 扩展字段

| 键名 | 结构 | 说明 |
| --- | --- | --- |
| `lunar` | `LunarDateInfo \| null` | 农历日期、月名和日名 |
| `solarTerm` | `SolarTermInfo \| null` | 节气索引与中文名 |
| `ganZhi` | `GanZhiInfo \| null` | 天干、地支、干支纪年和生肖 |
