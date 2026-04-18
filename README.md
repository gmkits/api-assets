# cn-holiday-kit —— 中国节假日数据平台

`cn-holiday-kit` 是一个面向中国节假日数据的跨平台工具包与数据平台。

它覆盖了 **规范定义、数据生产、二进制编译、运行时查询、HTTP 服务、农历转换、节气计算、前端展示** 的完整链路，适合做：

- 内网节假日查询服务
- Java / TypeScript 节假日查询 SDK
- 节假日数据生产与发布流水线
- 农历公历互转与二十四节气计算
- 运维可观测、可预热、可缓存的 REST API 服务

---

## 目录

- [项目定位](#项目定位)
- [数据链路](#数据链路)
- [模块说明](#模块说明)
- [快速开始](#快速开始)
- [农历模块](#农历模块)
- [二十四节气](#二十四节气)
- [已知问题与常识说明](#已知问题与常识说明)
- [API 服务部署](#api-服务部署)
- [编译器命令行](#编译器命令行)
- [目录结构](#目录结构)
- [Java 版本兼容性](#java-版本兼容性)
- [CI 持续集成](#ci-持续集成)
- [构建与测试命令](#构建与测试命令)
- [后续规划](#后续规划)
- [许可证](#许可证)

---

## 项目定位

本项目共分八层：

| 层 | 说明 |
| --- | --- |
| **规范层** | 节假日元数据、`.hday` 二进制格式、API 合同、JSON Schema |
| **数据层** | 原始数据 → Canonical → Materialized → `.hday` 二进制包 |
| **工具层** | 校验、展开、编译、检查等命令行工具 |
| **运行时层** | TypeScript / Java 查询 SDK |
| **农历层** | 公历↔农历转换（1900–2100，位压缩 ~800 字节） |
| **节气层** | 二十四节气计算（基于 VSOP87 简化太阳黄经算法） |
| **服务层** | Spring Boot REST API（Java 8 兼容层 + Java 25 高性能层） |
| **前端层** | Vue 3 管理台与可复用日历组件 |

---

## 数据链路

```text
原始数据（Raw，不可信）
  ↓
Canonical 规范数据（唯一事实来源，方便审计与版本管理）
  ↓
Materialized 年度展开数据（按日展开，便于编译与检查）
  ↓
.hday 二进制包（运行时高性能格式）
  ↓
Java / TypeScript SDK 查询
  ↓
HTTP API 暴露给内网系统
```

---

## 模块说明

### TypeScript 包

| 包名 | 说明 |
| --- | --- |
| `@holiday/spec` | 共享类型、常量与枚举（含农历扩展类型 `LunarDateInfo`） |
| `@holiday/core` | `.hday` 运行时查询 SDK（单日、区间、月查询、工作日统计、下个假期） |
| `@holiday/lunar` | 农历转换 + 二十四节气计算（1900–2100，位压缩 ~800 字节） |
| `@holiday/compiler` | Canonical 校验、物化、编译、命令行工具 |
| `@holiday/web-client` | HTTP API 客户端（对齐 `/api/v1`） |
| `@holiday/vue` | Vue 3 组合式 API 与日历组件 |

### Java 模块

| 模块 | 说明 |
| --- | --- |
| `holiday-spec-java` | 共享 DTO / 枚举（含 `LunarDateInfo`） |
| `holiday-core-java` | `.hday` 读取与高性能查询核心 |
| `holiday-lunar-java` | 农历转换 + 二十四节气计算（纯算法，Java 8+） |
| `holiday-spring-starter` | Spring Boot 自动配置 |
| `holiday-api-j8` | Java 8 / Spring Boot 2.7 兼容 API |
| `holiday-api-j25` | Java 25 / Spring Boot 4 高性能 API（虚拟线程、分层缓存、审计日志、限流） |

### 前端应用

| 应用 | 说明 |
| --- | --- |
| `apps/admin-web` | Vue 3 管理后台 |
| `apps/demo-web` | 浏览器演示应用 |

---

## 快速开始

### 安装与构建

**TypeScript 工作区**

```bash
corepack enable          # 启用 pnpm
pnpm install             # 安装依赖
pnpm run build           # 构建所有 TypeScript 包
pnpm run lint            # 类型检查
pnpm run test            # 运行所有测试
```

**Java（需要 JDK 17+）**

```bash
cd java
mvn -B clean test                    # 构建并测试所有 Java 8 兼容模块
mvn -B clean test -Pj25              # 构建并测试 J25 模块（需要 JDK 25+）
```

### TypeScript 查询示例

```ts
import { createHolidayService } from '@holiday/core';

const service = createHolidayService({
  dataPath: './data/bundles',
  defaultRegion: 'CN',
});

// 查询国庆节当天
const day = await service.getDayInfo('2025-10-01');

// 查询国庆假期区间
const range = await service.getRange('2025-10-01', '2025-10-08');

// 查询整年数据
const year = await service.getYear(2026);

// 查询某月数据
const month = await service.getMonth(2025, 10);

// 统计工作日数
const workdays = await service.countWorkdays('2025-10-01', '2025-10-31');

// 查找下一个法定假期
const nextHoliday = await service.getNextHoliday('2025-07-01');
```

### Java 查询示例

```java
import com.github.gmkits.holiday.core.HolidayService;
import com.github.gmkits.holiday.core.HolidayServiceBuilder;
import java.nio.file.Paths;
import java.time.LocalDate;

HolidayService service = new HolidayServiceBuilder()
        .defaultRegion("CN")
        .dataPath(Paths.get("./data/bundles"))
        .build();

// 查询国庆节当天
service.getDayInfo("CN", LocalDate.of(2025, 10, 1));

// 查询国庆假期区间
service.getRange("CN", LocalDate.of(2025, 10, 1), LocalDate.of(2025, 10, 8));

// 查询整年 / 月份 / 工作日统计 / 下个假期
service.getYear("CN", 2026);
service.getMonth("CN", 2025, 10);
service.countWorkdays("CN", LocalDate.of(2025, 10, 1), LocalDate.of(2025, 10, 31));
service.getNextHoliday("CN", LocalDate.of(2025, 7, 1));
```

---

## 农历模块

### 设计概要

| 项目 | 说明 |
| --- | --- |
| 数据来源 | 香港天文台（HKO）/ 紫金山天文台天文年历 |
| 覆盖范围 | 1900–2100（201 年） |
| 存储方案 | 每年一个 20 位整数，201 年仅 ~800 字节 |
| 基准日 | 1900-01-31（庚子年正月初一） |
| 线程安全 | 纯函数设计，可安全并发 |

**位编码格式**：

| 位 | 含义 |
| --- | --- |
| bit 0–3 | 闰月月份（0 = 无闰月，1–12 = 闰几月） |
| bit 4 | 闰月天数（0 = 29 天，1 = 30 天） |
| bit 5–16 | 1–12 月天数（0 = 29 天，1 = 30 天） |

### TypeScript 用法

```ts
import {
  solarToLunar,   // 公历 → 农历
  lunarToSolar,   // 农历 → 公历
  leapMonth,      // 查询某年闰几月
  leapMonthDays,  // 查询某年闰月天数
} from '@holiday/lunar';

// ── 公历 → 农历 ──
const info = solarToLunar(2025, 1, 29);
// 返回：{ year: 2025, month: 1, day: 1, isLeapMonth: false,
//         ganZhiYear: '乙巳年', shengXiao: '蛇',
//         monthName: '正月', dayName: '初一',
//         fullName: '乙巳年 正月初一' }

// ── 农历 → 公历（普通月份）──
const [y, m, d] = lunarToSolar(2025, 1, 1);
// 返回：[2025, 1, 29]

// ── 农历 → 公历（闰月，需指定 isLeapMonth = true）──
const [y2, m2, d2] = lunarToSolar(2023, 2, 1, true);
// 返回：[2023, 3, 22]（闰二月初一）
```

### Java 用法

```java
import com.github.gmkits.holiday.lunar.LunarCalendar;
import com.github.gmkits.holiday.lunar.LunarInfo;
import java.time.LocalDate;

// 公历 → 农历
LunarInfo info = LunarCalendar.solarToLunar(LocalDate.of(2025, 1, 29));

// 农历 → 公历（普通月份）
LocalDate date = LunarCalendar.lunarToSolar(2025, 1, 1);

// 农历 → 公历（闰月，需指定 isLeapMonth = true）
LocalDate leapDate = LunarCalendar.lunarToSolar(2023, 2, 1, true);
```

### 闰月与公历农历映射关系

> **核心规则**：公历日期 **一定** 唯一对应一个农历日期；但在闰月年份，同一个农历日期可能对应 **两个** 不同的公历日期。

**公历 → 农历**（`solarToLunar`）：

- 永远是**一对一映射**，不存在歧义。
- 返回值中的 `isLeapMonth` 字段明确标识该日期是否落在闰月中。
- 示例：
  - `solarToLunar(2023, 3, 22)` → 农历闰二月初一（`isLeapMonth = true`）
  - `solarToLunar(2023, 2, 20)` → 农历二月初一（`isLeapMonth = false`）

**农历 → 公历**（`lunarToSolar`）：

- 调用方**必须**明确指定 `isLeapMonth` 参数，否则无法消除歧义。
- 示例（假设该年有闰四月）：
  - `lunarToSolar(year, 4, 1, false)` → 正常四月初一的公历日期
  - `lunarToSolar(year, 4, 1, true)` → 闰四月初一的公历日期
- 如果传入 `isLeapMonth = true` 但该年实际没有该闰月，会抛出异常。

**最佳实践**：

```ts
import { leapMonth, leapMonthDays, lunarToSolar } from '@holiday/lunar';

// 查询 2023 年的闰月信息
const leap = leapMonth(2023);        // => 2（闰二月）
const days = leapMonthDays(2023);    // => 29（闰二月 29 天）

// 安全转换：先检查是否有闰月
if (leapMonth(year) === targetMonth) {
  const normalDate = lunarToSolar(year, targetMonth, 1, false);  // 正常月份
  const leapDate   = lunarToSolar(year, targetMonth, 1, true);   // 闰月
}
```

### 扩展点

- 农历数据通过 `LunarDateInfo` 类型可嵌入 `DayInfo.extensions["lunar"]`，不破坏现有接口。
- 如天文台修正数据，只需替换 `LUNAR_INFO` 数组中对应年份的压缩整数，无需改动算法。

---

## 二十四节气

### 概述

二十四节气是中国传统历法中根据太阳在黄道上的位置划分的 24 个时间节点。每个节气对应太阳黄经的特定度数，每 15° 一个节气。

本项目在 TypeScript 和 Java 的农历模块中均实现了节气计算。算法基于 Jean Meeus《天文算法》中的简化 VSOP87 太阳黄经公式，精度约 ±1 天，满足日历显示需求。

### 二十四节气对照表

| 节气 | 太阳黄经 | 公历大约日期 | 含义 |
| --- | --- | --- | --- |
| 小寒 | 285° | 1 月 5–7 日 | 开始进入寒冷季节 |
| 大寒 | 300° | 1 月 20–21 日 | 一年中最冷的时期 |
| 立春 | 315° | 2 月 3–5 日 | 春季开始 |
| 雨水 | 330° | 2 月 18–20 日 | 降水开始增多 |
| 惊蛰 | 345° | 3 月 5–7 日 | 春雷始鸣，蛰虫惊醒 |
| 春分 | 0° | 3 月 20–22 日 | 昼夜等长，春季中点 |
| 清明 | 15° | 4 月 4–6 日 | 天气清爽明朗 |
| 谷雨 | 30° | 4 月 19–21 日 | 雨水增多，利于谷物生长 |
| 立夏 | 45° | 5 月 5–7 日 | 夏季开始 |
| 小满 | 60° | 5 月 20–22 日 | 谷物开始饱满 |
| 芒种 | 75° | 6 月 5–7 日 | 有芒谷物成熟可收割 |
| 夏至 | 90° | 6 月 21–22 日 | 白昼最长，夏季中点 |
| 小暑 | 105° | 7 月 6–8 日 | 开始炎热 |
| 大暑 | 120° | 7 月 22–24 日 | 一年中最热的时期 |
| 立秋 | 135° | 8 月 7–9 日 | 秋季开始 |
| 处暑 | 150° | 8 月 22–24 日 | 暑热结束 |
| 白露 | 165° | 9 月 7–9 日 | 天气转凉，露水凝白 |
| 秋分 | 180° | 9 月 22–24 日 | 昼夜等长，秋季中点 |
| 寒露 | 195° | 10 月 7–9 日 | 露水寒冷 |
| 霜降 | 210° | 10 月 23–24 日 | 开始有霜 |
| 立冬 | 225° | 11 月 7–8 日 | 冬季开始 |
| 小雪 | 240° | 11 月 22–23 日 | 开始降雪 |
| 大雪 | 255° | 12 月 6–8 日 | 降雪增多 |
| 冬至 | 270° | 12 月 21–23 日 | 白昼最短，冬季中点 |

### TypeScript 节气用法

```ts
import { getSolarTerms, getSolarTerm } from '@holiday/lunar';

// 获取 2025 年全部 24 个节气
const terms = getSolarTerms(2025);
for (const term of terms) {
  console.log(`${term.name}: ${term.date[0]}-${term.date[1]}-${term.date[2]} (黄经 ${term.longitude}°)`);
}
// 输出：
// 小寒: 2025-1-5 (黄经 285°)
// 大寒: 2025-1-20 (黄经 300°)
// ...

// 查询某天是否恰好是节气
const name = getSolarTerm(2025, 3, 20);
// => '春分'     ← 当天是春分
// => null       ← 当天不是节气
```

### Java 节气用法

```java
import com.github.gmkits.holiday.lunar.LunarCalendar;
import com.github.gmkits.holiday.lunar.LunarCalendar.SolarTermInfo;
import java.time.LocalDate;

// 获取 2025 年全部 24 个节气
SolarTermInfo[] terms = LunarCalendar.getSolarTerms(2025);
for (SolarTermInfo term : terms) {
    System.out.println(term.getName() + ": " + term.getDate()
        + " (黄经 " + term.getLongitude() + "°)");
}

// 查询某天是否恰好是节气
String name = LunarCalendar.getSolarTerm(LocalDate.of(2025, 3, 20));
// => "春分" 或 null
```

### 节气算法说明

节气计算使用简化 VSOP87 太阳黄经公式（Jean Meeus《天文算法》第 25 章）：

1. 计算太阳几何平黄经 L₀ 和太阳平近点角 M
2. 通过 M 的三角展开计算太阳中心方程 C
3. 叠加章动修正得到太阳视黄经
4. 从目标黄经度数出发，用迭代法逼近太阳恰好经过该度数的日期

精度约 ±0.01°，对应日期精度约 ±1 天。

---

## 已知问题与常识说明

### 2033 年问题

**背景**：2033 年是农历中一个著名的特殊年份。

**问题本质**：2033 年农历出现极为罕见的情况——冬至落在农历十一月的最后一天（晦日），且该年存在闰月。传统"无中气置闰"规则在此边界情况下产生歧义，不同方案给出不同的闰月位置：

| 方案 | 闰月 | 采用者 |
| --- | --- | --- |
| 方案 A | 闰七月 | 部分传统万年历 |
| 方案 B | **闰十一月** | **紫金山天文台《天文年历》（国家标准）** |

**本项目处理**：

- `LUNAR_INFO` 数据表采用紫金山天文台 / 香港天文台（HKO）发布的权威数据，**2033 年为闰十一月**。
- 已通过 73,000+ 行 HKO 参照 CSV 全量交叉验证。
- 若未来天文台修正数据，只需替换 `LUNAR_INFO` 中对应年份的压缩整数即可。

```ts
import { leapMonth } from '@holiday/lunar';
leapMonth(2033); // => 11（闰十一月）
```

### 数据覆盖范围

- 农历数据覆盖 **1900-01-31** 至 **2100 年末**，共 201 年。
- 超出范围会抛出 `RangeError`（TypeScript）或 `IllegalArgumentException`（Java）。
- 2100 年之后的数据需等天文台发布后才能扩展。

### 朔日估算与数据表的关系

本项目包含两套独立的农历能力：

| 能力 | 说明 | 精度 | 用途 |
| --- | --- | --- | --- |
| 精确数据表 `LUNAR_INFO` | 基于天文台权威数据的位压缩查表 | 精确到天 | 所有实际转换操作 |
| 天文估算函数 `estimateNewMoonJDE` | 基于 Jean Meeus 朔日算法 | ±2 天 | 仅用于交叉验证数据表 |

### 公历闰年规则

- 能被 4 整除且不能被 100 整除 → 闰年
- 能被 400 整除 → 闰年
- 因此：2000 年是闰年，1900 年不是闰年，2100 年不是闰年
- 本项目 `isLeapYear()`（位于 `@holiday/spec`）严格遵循此规则

### 农历闰月规则

农历闰月由天文观测决定，非简单数学公式：

- 以朔日（新月）为每月初一
- 正常年 12 个月（354 或 355 天），比公历少约 11 天
- 每隔 2–3 年插入一个闰月（全年 13 个月，383–385 天），使农历年与太阳年对齐
- 闰月位置由**"无中气置闰"**规则确定：两个冬至之间若有 13 个月，第一个没有中气的月份为闰月
- 中气指太阳黄经为 30° 整数倍的节气（雨水、春分、谷雨、小满、夏至、大暑、处暑、秋分、霜降、小雪、冬至、大寒）
- 每月 29 天（小月）或 30 天（大月），取决于实际朔日间隔

### 天干地支与生肖

| 概念 | 说明 |
| --- | --- |
| 天干 | 甲、乙、丙、丁、戊、己、庚、辛、壬、癸（10 个，循环） |
| 地支 | 子、丑、寅、卯、辰、巳、午、未、申、酉、戌、亥（12 个，循环） |
| 干支纪年 | 天干 + 地支，60 年一个周期（如"甲子年"、"乙巳年"） |
| 十二生肖 | 与地支一一对应（子鼠、丑牛、寅虎、卯兔、辰龙、巳蛇、午马、未羊、申猴、酉鸡、戌狗、亥猪） |
| 计算公式 | 天干 = `(year - 4) % 10`，地支 = `(year - 4) % 12`，其中 `year` 为农历年 |

### 其他已知特殊年份

| 年份 | 特殊性 | 说明 |
| --- | --- | --- |
| 1900 | 数据起始年 | 庚子年，基准日 1900-01-31（正月初一） |
| 2033 | 闰十一月争议 | 详见上文"2033 年问题" |
| 2034 | 无春年（盲年） | 整个农历年内没有立春节气 |
| 2097 | 数据修正 | 原六月 30→29 天、七月 29→30 天，已根据 HKO 数据修正（`0x0a4d0` → `0x0a2d0`） |
| 2100 | 数据终止年 | 超出范围需等天文台发布新数据 |

---

## API 服务部署

### 兼容服务（Java 8 / Spring Boot 2.7）

适合需要兼容 Java 8 的旧环境：

```bash
cd java
mvn -pl holiday-api-j8 spring-boot:run
```

### 高性能服务（Java 25 / Spring Boot 4）

适合内网部署，支持虚拟线程、分层缓存、OpenAPI 文档、审计日志、限流等：

```bash
cd java
mvn -Pj25 -pl holiday-api-j25 spring-boot:run
```

**内置能力**：

- 单日 / 区间 / 整年 / 月份查询
- 工作日统计
- 下一个法定节假日查找
- 地区查询、版本信息、manifest 查询
- 缓存清理 / 预热 / manifest 重载等运维接口
- Actuator 健康检查
- Swagger / OpenAPI 文档

### 接口列表（holiday-api-j25）

| 方法 | 接口路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v2/day` | 查询单日信息 |
| GET | `/api/v2/range` | 查询日期区间 |
| GET | `/api/v2/year` | 查询整年数据 |
| GET | `/api/v2/month` | 查询指定月份 |
| GET | `/api/v2/workday-count` | 统计区间内工作日天数 |
| GET | `/api/v2/next-holiday` | 查找下一个法定节假日 |
| GET | `/api/v2/regions` | 查询支持的地区列表 |
| GET | `/api/v2/version` | 查询版本信息 |
| GET | `/api/v2/manifest` | 读取 manifest |
| GET | `/api/v2/bundles/{regionCode}/{year}/metadata` | 查询 bundle 元信息 |
| POST | `/api/v2/ops/cache/clear` | 清理缓存 |
| POST | `/api/v2/ops/cache/warmup` | 预热缓存 |
| POST | `/api/v2/ops/manifest/reload` | 重载 manifest |

---

## 编译器命令行

```bash
# 校验 Canonical 数据格式
holiday-compiler validate --input data/canonical/CN/2025.canon.json

# 物化：Canonical → 年度展开数据
holiday-compiler materialize --input data/canonical/CN/2025.canon.json \
    --output data/materialized/CN/2025.year.json

# 编译：展开数据 → .hday 二进制包
holiday-compiler compile --input data/materialized/CN/2025.year.json \
    --output data/bundles/CN/2025.hday

# 构建 manifest
holiday-compiler build-manifest --bundles-dir data/bundles \
    --output data/manifest.json

# 检查 .hday 二进制包
holiday-compiler inspect --bundle data/bundles/CN/2025.hday
```

---

## 目录结构

```text
cn-holiday-kit/
├── spec/                          # 规范文档、二进制格式、JSON Schema
│   ├── holiday-spec.md            # 主规范
│   ├── bundle-format.md           # .hday 二进制格式
│   ├── api-contract.md            # HTTP API 合同
│   ├── enums.md                   # 枚举字典
│   └── holiday-json-schema/       # JSON Schema 定义
├── data/                          # 数据资产
│   ├── canonical/                 # Canonical 规范数据
│   ├── materialized/              # 年度展开数据
│   ├── bundles/                   # .hday 二进制包
│   └── manifest.json              # 包清单
├── packages/                      # TypeScript 包
│   ├── ts-spec/                   # 共享类型与常量
│   ├── ts-core/                   # 查询 SDK
│   ├── ts-lunar/                  # 农历转换 + 节气计算
│   ├── ts-compiler/               # 编译器
│   ├── ts-web-client/             # HTTP 客户端
│   └── ts-vue/                    # Vue 组件
├── java/                          # Java 多模块工程
│   ├── holiday-spec-java/         # 共享类型
│   ├── holiday-core-java/         # 查询核心
│   ├── holiday-lunar-java/        # 农历转换 + 节气计算
│   ├── holiday-spring-starter/    # Spring Boot 自动配置
│   ├── holiday-api-j8/            # Java 8 兼容 API
│   └── holiday-api-j25/           # Java 25 高性能 API
├── apps/                          # 前端应用
│   ├── admin-web/                 # Vue 3 管理后台
│   └── demo-web/                  # 浏览器演示
├── scripts/                       # 辅助脚本
├── tests/                         # 全量测试数据（HKO 参照 CSV 等）
└── .github/workflows/ci.yml       # CI 持续集成
```

---

## Java 版本兼容性

| 模块 | 最低 Java 版本 | Spring Boot 版本 | 说明 |
| --- | --- | --- | --- |
| `holiday-spec-java` | Java 8 | — | 纯 DTO 与枚举 |
| `holiday-core-java` | Java 8 | — | 查询核心（依赖 Guava） |
| `holiday-lunar-java` | Java 8 | — | 纯算法（仅依赖 Lombok） |
| `holiday-spring-starter` | Java 8 | — | 自动配置 |
| `holiday-api-j8` | Java 8 | 2.7.18 | 兼容旧环境 |
| `holiday-api-j25` | Java 25 | 4.0.5 | 独立 profile `-Pj25` 构建 |

**注意事项**：

- 默认构建（`mvn test`）仅编译 Java 8 兼容模块，但需要 JDK 17+ 运行 Maven。
- J25 模块通过 `-Pj25` profile 单独激活，需要 JDK 25+。
- 所有 Java 8 模块严格使用 Java 8 API（如使用 Guava 的 `ImmutableList.of()` 而非 Java 9+ 的 `List.of()`）。

---

## CI 持续集成

当前 CI 配置（`.github/workflows/ci.yml`）包含以下任务：

| 任务 | 说明 |
| --- | --- |
| **校验 JSON Schema** | 使用 `ajv-cli` + `ajv-formats` 验证所有 Schema 文件格式正确 |
| **TypeScript 构建与测试** | 全量构建 + 类型检查 + 测试 + 编译器流水线验证 |
| **Java 构建与测试** | JDK 17（默认模块）+ JDK 25（J25 模块）矩阵构建 |
| **Bundle 完整性校验** | 检查 `.hday` 文件魔数（`HDAY`）+ SHA256 哈希比对 |

---

## 构建与测试命令

```bash
# ── TypeScript ──
pnpm run build              # 构建所有 TypeScript 包
pnpm run lint               # 类型检查
pnpm run test               # 运行所有测试

# ── Java（需要 JDK 17+）──
cd java
mvn -B clean test                              # 测试所有 Java 8 兼容模块
mvn -B clean test -pl holiday-lunar-java       # 仅测试农历模块
mvn -B clean test -pl holiday-core-java -am    # 测试核心模块及其依赖
mvn -B clean test -Pj25                        # 测试 J25 模块（需要 JDK 25+）
```

---

## 查询性能优化

### 农历模块

| 优化项 | 说明 |
| --- | --- |
| 年天数预计算表 | 模块初始化时构建 201 年天数缓存，`yearDays()` 从 O(12) 降至 O(1) |
| 前缀和 + 二分查找 | `solarToLunar()` 年份定位从 O(n) 降至 O(log n)（~8 次查找） |
| 前缀和直接读取 | `lunarToSolar()` 年份累计从 O(n) 循环降至 O(1) |
| 位压缩存储 | 201 年仅 ~800 字节，远小于逐天映射的 73,000+ 字节 |
| 纯函数无状态 | 线程安全，可无锁并发，适合虚拟线程环境 |

### 节气计算

| 优化项 | 说明 |
| --- | --- |
| VSOP87 简化公式 | 精度 ±0.01°（日期精度 ±1 天），无需完整天文表 |
| 迭代逼近 | 通常 3–5 次迭代即收敛 |

### 节假日查询内核

| 优化项 | 说明 |
| --- | --- |
| 预构建查询视图 | `DayInfo[]` 在 bundle 加载时一次性构建，后续查询零分配复用 |
| LRU 缓存 | bundle 按 `(region, year)` 粒度缓存，避免重复 IO |
| 批量路径 | 区间 / 年 / 月查询直接走 `getRange()` 切片，不逐日调用 |

---

## 后续规划

1. 完善 `holiday-api-j25` 的容器化与部署模板
2. 增加 manifest / bundle 元数据缓存失效策略
3. 增加更细粒度的基准测试和回归测试
4. 补充更多地区、更多来源的数据导入器
5. 农历数据精度验证与天文台数据定期同步
6. 节气计算结果缓存优化

---

## 许可证

Apache-2.0
