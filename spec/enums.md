# Holiday Data Platform — Enumeration Dictionary v1.0.0

## DayKind

Values used in rules and materialized data to classify each day:

| Value | Description (EN) | Description (ZH) |
|---|---|---|
| `STATUTORY_HOLIDAY` | A day that is a statutory holiday per law | 法定节假日 |
| `OFFICIAL_HOLIDAY` | A day designated as holiday in the annual arrangement | 放假日（含调休形成的假期） |
| `ADJUSTED_WORKDAY` | A weekend day reclassified as workday due to holiday swap | 调休补班日 |
| `NORMAL_WORKDAY` | A regular workday (Mon-Fri, not overridden) | 正常工作日 |
| `NORMAL_WEEKEND` | A regular weekend day (Sat/Sun, not overridden) | 正常周末 |

## RuleType

Values for rules in canonical spec:

| Value | Description |
|---|---|
| `FIXED_DATE` | A single specific date |
| `DATE_RANGE` | A contiguous range of dates (from/to) |
| `WEEKDAY_OVERRIDE` | Override a specific weekday pattern |
| `LUNAR_DATE` | A date in a non-Gregorian calendar (e.g., Chinese lunar) |
| `RECURRENCE` | A recurring pattern (e.g., every year on same date) |
| `PATCH` | An enterprise/organizational patch override |

## SourceType

Values for data source provenance:

| Value | Description |
|---|---|
| `GOV_NOTICE` | Official government holiday notice |
| `ICS_FEED` | iCalendar (RFC 5545) feed |
| `THIRD_PARTY_JSON` | Third-party JSON data source |
| `CSV_IMPORT` | CSV/Excel import |
| `MANUAL_ENTRY` | Manual human entry |
| `ENTERPRISE_PATCH` | Enterprise-specific patch |

## CalendarSystem

| Value | Description |
|---|---|
| `GREGORIAN` | Standard Gregorian calendar |
| `CHINESE_LUNAR` | Chinese traditional lunar calendar (农历) |

## HolidayLabel

Standard labels that can be applied to days. These are not exhaustive — custom labels are allowed.

| Value | Description (EN) | Description (ZH) |
|---|---|---|
| `NEW_YEAR` | New Year's Day | 元旦 |
| `SPRING_FESTIVAL` | Spring Festival (Chinese New Year) | 春节 |
| `TOMB_SWEEPING` | Tomb-Sweeping Day (Qingming) | 清明节 |
| `LABOR_DAY` | International Labor Day | 劳动节 |
| `DRAGON_BOAT` | Dragon Boat Festival (Duanwu) | 端午节 |
| `MID_AUTUMN` | Mid-Autumn Festival | 中秋节 |
| `NATIONAL_DAY` | National Day | 国庆节 |
| `STATUTORY` | Marks a statutory holiday proper | 法定节假日标记 |
| `ADJUSTED_WORKDAY` | Marks an adjusted workday | 调休补班标记 |
| `BRIDGE_DAY` | A bridge day connecting holidays | 桥接假日 |

## WeekDay

Values used in `weekendMask`:

| Value | Meaning |
|---|---|
| `MON` | Monday |
| `TUE` | Tuesday |
| `WED` | Wednesday |
| `THU` | Thursday |
| `FRI` | Friday |
| `SAT` | Saturday |
| `SUN` | Sunday |

## CalendarSystem Numeric Codes (for .hday binary)

Used in the binary bundle header:

| Code | CalendarSystem |
|---|---|
| `0x00` | `GREGORIAN` |
| `0x01` | `CHINESE_LUNAR` |
| `0x02`–`0xFF` | Reserved |

## Section Types (for .hday binary)

| Code | Section |
|---|---|
| `0x0001` | `DAY_TABLE` |
| `0x0002` | `STRING_TABLE` |
| `0x0003` | `NAME_LIST_TABLE` |
| `0x0004` | `EXT_JSON` |
| `0x0005`–`0xFFFF` | Reserved for future use |

## Flag Bits (DAY_TABLE entry)

| Bit | Name | Meaning |
|---|---|---|
| 0 | `IS_HOLIDAY` | Day off |
| 1 | `IS_WORKDAY` | Working day |
| 2 | `IS_WEEKEND` | Default weekend |
| 3 | `IS_STATUTORY_HOLIDAY` | Statutory holiday |
| 4 | `IS_ADJUSTED_WORKDAY` | Adjusted workday |
| 5 | `HAS_NAME` | Has associated name(s) |
| 6 | `HAS_LABEL` | Has associated label(s) |
| 7–15 | Reserved | Reserved for future use |
