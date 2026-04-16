# cn-holiday-kit 主规范

本文档说明仓库当前的核心设计目标、数据分层、查询模型与扩展约定。它不追求覆盖每一个历史设想，而是以**当前已实现能力**为主。

## 1. 项目定位

`cn-holiday-kit` 不是单一的“判断某天是不是假日”的工具函数，而是一套围绕中国节假日数据构建的完整工具链：

1. 规范与 Schema
2. 原始数据清洗与规范化
3. `.hday` 二进制编译
4. Java / TypeScript 运行时查询
5. HTTP API 与运维接口
6. 农历与节气扩展能力

## 2. 数据分层

```text
原始数据（Raw）
  -> Canonical 规范数据
  -> Materialized 年度展开数据
  -> .hday Runtime Bundle
  -> SDK / HTTP API 查询
```

### 2.1 Raw

- 保存原始来源内容
- 只用于审计、追溯、重放导入
- 运行时绝不直接消费

### 2.2 Canonical

- 单地区单年份的唯一事实来源
- 导入器必须先产出 Canonical
- 适合做审查、版本管理与差异比较

### 2.3 Materialized

- 把规则完全展开到公历日期
- 适合做 golden 对比与编译输入

### 2.4 `.hday`

- 面向运行时的高性能二进制格式
- 单年单地区一个文件
- 只负责主数据，不直接内嵌农历/节气日级扩展

## 3. 查询模型

查询层统一围绕 `DayInfo` 语义展开。一个日历日至少包含以下语义字段：

| 语义 | 说明 |
| --- | --- |
| `date` | 日期 |
| `regionCode` | 地区代码 |
| `calendarSystem` | 历法体系 |
| `holiday` / `isHoliday` | 是否休息日 |
| `workday` / `isWorkday` | 是否工作日 |
| `weekend` / `isWeekend` | 是否默认周末 |
| `statutoryHoliday` / `isStatutoryHoliday` | 是否法定节假日 |
| `adjustedWorkday` / `isAdjustedWorkday` | 是否调休补班 |
| `holidayNames` | 多语言名称 |
| `labels` | 标签列表 |
| `sourceVersion` | 数据版本 |
| `extensions` | 兼容扩展区 |

### 3.1 语义约束

1. 休息日与工作日互斥，且二者必有其一
2. 默认周末不因调休而消失
3. 法定节假日是休息日的子集
4. 调休补班必须是工作日

### 3.2 命名约定

当前仓库已经存在语言差异：

- TypeScript SDK 使用 `isHoliday/isWorkday/...`
- Java 对象与 HTTP JSON 当前使用 `holiday/workday/...`
- `@holiday/web-client` 会把 HTTP 返回归一化为 TypeScript 侧命名

因此本规范强调**语义对齐**，不再宣称所有语言层的字段名完全一致。

## 4. 扩展约定

`extensions` 是当前最稳定的兼容扩展位。仓库已落地两类标准扩展：

### 4.1 `extensions.lunar`

提供当天对应的农历信息：

- 年、月、日
- 是否闰月
- 干支年
- 生肖
- 月中文名
- 日中文名

### 4.2 `extensions.solarTerm`

仅在当天命中节气时返回，结构为：

- `index`：稳定节气序号（`0-23`）
- `name`：节气中文名

## 5. 权威数据基线

当前农历与节气的跨语言校验均基于香港天文台（HKO）1901-2100 数据：

| 文件 | 说明 |
| --- | --- |
| `tests/lunar-golden.csv` | 公历 ↔ 农历权威对照 |
| `tests/solar-terms.csv` | 节气日期权威对照 |

这些基线同时同步到 Java 测试资源中，用于跨语言一致性验证。

## 6. 运行时实现策略

### 6.1 节假日主查询

- `.hday` 负责承载节假日、名称、标签等主数据
- Java `HdayBundle` 在初始化阶段预构建 `DayInfo[]`
- TypeScript 查询层对 bundle 建立惰性查询视图

### 6.2 农历扩展

- 使用 1900-2100 压缩算法表
- TS / Java 都已实现边界校验、闰月校验与 round-trip 测试

### 6.3 节气扩展

- 先从 HKO 文本提取 `tests/solar-terms.csv`
- 再离线生成年度 `dayIndex` 表
- 运行时以 O(1) 查表方式补到 `extensions.solarTerm`

## 7. 性能策略

### 7.1 Java

- `holiday-core-java` 使用 **Caffeine 后端** 承载 bundle 缓存
- 区间查询预估容量并直接追加，避免中间列表复制
- `getYear()` 直接返回预构建年视图
- `getNextHoliday()` 直接扫描预构建数组

### 7.2 TypeScript

- 预计算 `dayIndex -> month/day`
- 查询链路缓存名称与标签解析结果
- 节气运行时不读取 CSV，只查离线生成表
- 农历范围先做快判，避免把异常当流程控制

## 8. 兼容性边界

| 层 | 当前边界 |
| --- | --- |
| Java 8 模块 | 保持 Java 8 兼容 |
| `holiday-api-j25` | 可使用 Java 25 / Spring Boot 4 / Caffeine |
| `.hday` 格式 | 当前仍为 v1.0.0 |
| 农历范围 | 1900-2100 |
| HKO 节气范围 | 1901-2100 |

## 9. 文档关系

- `README.md`：中文主入口与使用说明
- `spec/api-contract.md`：HTTP 接口
- `spec/enums.md`：枚举与位标记
- `spec/bundle-format.md`：`.hday` 二进制格式

如果实现与历史文档不一致，应以**当前代码与测试**为准，并同步回写这些文档。
