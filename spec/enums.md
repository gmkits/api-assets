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
| `0x0001` | `DAY_TABLE` | 每日主表 |
| `0x0002` | `STRING_TABLE` | 字符串池 |
| `0x0003` | `NAME_LIST_TABLE` | 名称/标签列表 |
| `0x0004` | `EXT_JSON` | 年级扩展 JSON |
| `0x0005` - `0xFFFF` | 预留 | 未来扩展 |

### 7.3 DAY_TABLE 标志位

| 位 | 掩码 | 名称 | 说明 |
| --- | --- | --- | --- |
| 0 | `0x0001` | `IS_HOLIDAY` | 休息日 |
| 1 | `0x0002` | `IS_WORKDAY` | 工作日 |
| 2 | `0x0004` | `IS_WEEKEND` | 默认周末 |
| 3 | `0x0008` | `IS_STATUTORY_HOLIDAY` | 法定节假日 |
| 4 | `0x0010` | `IS_ADJUSTED_WORKDAY` | 调休补班 |
| 5 | `0x0020` | `HAS_NAME` | 存在名称列表 |
| 6 | `0x0040` | `HAS_LABEL` | 存在标签列表 |
| 7 - 15 | `0xFF80` | 预留 | 当前必须为 0 |

## 8. 扩展字段

| 键名 | 结构 | 说明 |
| --- | --- | --- |
| `extensions.lunar` | `LunarDateInfo` | 农历日期、干支年、生肖、月名、日名 |
| `extensions.solarTerm` | `SolarTermInfo` | 节气索引与中文名 |
