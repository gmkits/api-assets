# cn-holiday-kit 主规范

## 1. 发布边界

对外只发布一个 Java 8+ JAR：`com.github.gmkits:cn-holiday-kit`。Java 源码内部保留
`spec`、`lunar`、`core` 包以保持依赖方向清晰，但不要求调用方分别安装或发版。

其他语言使用通用 CSV、`.hday` 或唯一 HTTP API。TypeScript 编译器、Web 组件、
Spring 服务和 JMH 都属于仓库内部实现或示例。

## 2. 数据链

```text
holidays.csv + sources.json
        ↓ 离线生成
target/canonical → target/materialized
        ↓ 编译
.hday → Java / TypeScript / HTTP
```

`data/source/CN/holidays.csv` 是节假日的唯一审阅源。Canonical 和 Materialized
只是 `target/` 中的构建中间物，不纳入版本控制，避免同一事实维护三份。

农历、节气和节假日是三类可分别替换的日期资产，运行时在 `DayInfo` 合并。

## 3. DayInfo 语义

| 字段 | 说明 |
| --- | --- |
| `date` | 公历日期 |
| `regionCode` | 区域，当前为 `CN` |
| `isHoliday` | 当天休息，包含普通周末 |
| `isOfficialHoliday` | 属于官方公布放假区间 |
| `isWorkday` | 当天需要工作 |
| `isWeekend` | 公历星期为周六或周日，不受调休覆盖影响 |
| `isStatutoryHoliday` | 法定节假日本日 |
| `isAdjustedWorkday` | 周末调休补班 |
| `holidayNames` | 官方安排的多语言名称 |
| `labels` | 官方安排的稳定代码 |
| `lunar` | 农历日期；超出支持范围时为 `null` |
| `solarTerm` | 当天命中的节气；非节气日为 `null` |
| `ganZhi` | 农历年对应的天干、地支、干支纪年和生肖 |
| `festivals` | 公历、农历或节气命中的节日/纪念日 |
| `sourceVersion` | 生成该 bundle 的数据版本 |

约束：

1. `isHoliday` 与 `isWorkday` 互斥且必有其一。
2. 普通周末：`isHoliday=true`、`isWeekend=true`、`isOfficialHoliday=false`。
3. 调休补班：`isHoliday=false`、`isWorkday=true`、`isWeekend=true`、`isAdjustedWorkday=true`。
4. `isStatutoryHoliday=true` 必须同时满足 `isHoliday=true` 和 `isOfficialHoliday=true`。
5. `festivals` 不改变工作状态，例如元宵节通常仍是工作日。
6. 干支采用农历年边界；不输出存在流派和换日边界差异的干支月、干支日。

Java、HTTP、TypeScript 与 JSON Schema 统一使用
`isHoliday/isOfficialHoliday/...` 字段名和语义。

## 4. 数据范围与准确度

- 节假日与调休：2000–2026。
- 农历转换：1900–2100。
- 二十四节气：1900–2100。
- 2007–2026 为国务院通知逐日交叉核验。
- 2001–2006 为存档通知级。
- 2000 为法规和历史日历重建级。

来源版本、SHA-256、置信等级和逐年公文链接在
`data/source/CN/sources.json` 中，不允许把重建数据标记为公文同等级。

## 5. 查询性能

Java bundle 加载后预构建不可变 `DayInfo[]`；缓存使用 JDK
`ConcurrentHashMap`，不引入 Caffeine。年内工作日计数使用前缀和 O(1)，下一法定日
使用反向索引 O(1)，跨年查询只按涉及年份循环。

TypeScript 建立等价的 `Uint16Array` 前缀和和 `Int16Array` 下一法定日索引。
农历使用压缩年度表，节气使用离线日期表；运行时不联网。

## 6. 兼容性

- Java 编译目标：`--release 8`。
- JDK 8：classpath。
- JDK 9+：同一 JAR 可作为自动模块 `com.github.gmkits.holiday`。
- `.hday`：v1，允许增加未知读取器可跳过的可选 section。

本项目当前处于整合阶段，不承诺兼容已经删除的多套 API 或手写 SDK。
