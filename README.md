# cn-holiday-kit

`cn-holiday-kit` 是一个面向中国节假日数据的跨平台工具包与数据平台，不只是一个简单的 `isHoliday(date)` 判断函数。

它覆盖了 **规范定义、数据生产、二进制编译、运行时查询、HTTP API、农历转换、节气计算、前端展示** 的完整链路，适合做：

- 内网节假日查询服务
- Java / TypeScript SDK
- 节假日数据生产与发布流水线
- 农历公历互转与节气计算
- 运维可观测、可预热、可缓存的 REST API 服务

---

## 项目定位

本项目当前分成八层：

1. **规范层**：节假日元数据、二进制格式、API 合同、JSON Schema
2. **数据层**：原始数据 → Canonical → Materialized → `.hday` Bundle
3. **工具层**：校验、展开、编译、检查 CLI
4. **运行时层**：TypeScript / Java 查询 SDK
5. **农历层**：公历↔农历转换（1900-2100，位压缩 ~800B）
6. **节气层**：二十四节气计算（基于 VSOP87 简化太阳黄经算法）
7. **服务层**：Spring Boot API（兼容层 + Java 25 新服务）
8. **前端层**：Vue 3 管理台与可复用日历组件

---

## 数据链路

项目的数据生产链路如下：

```text
原始数据（Raw）
  -> Canonical 规范数据
  -> Materialized 年度展开数据
  -> .hday 二进制 Bundle
  -> Java / TS SDK 查询
  -> HTTP API 暴露给内网系统
```

其中：

- **Raw**：原始来源数据，默认视为不可信
- **Canonical**：唯一事实来源，方便审计与版本管理
- **Materialized**：按日展开后的年数据，便于编译与检查
- **.hday**：运行时高性能二进制格式

---

## 当前模块说明

### TypeScript

| 包名 | 作用 |
| --- | --- |
| `@holiday/spec` | 共享类型、常量与枚举（含农历扩展类型） |
| `@holiday/core` | `.hday` 运行时查询 SDK（含月查询、工作日统计、下个假期） |
| `@holiday/lunar` | 农历转换模块 + 二十四节气计算（1900-2100，位压缩 ~800B） |
| `@holiday/compiler` | Canonical 校验、物化、编译、CLI |
| `@holiday/web-client` | HTTP API 客户端 |
| `@holiday/vue` | Vue 3 组合式 API 与日历组件 |

### Java

| 模块 | 作用 |
| --- | --- |
| `holiday-spec-java` | Java 共享 DTO / 枚举定义（含 LunarDateInfo） |
| `holiday-core-java` | `.hday` 读取与高性能查询核心 |
| `holiday-lunar-java` | 农历转换模块 + 二十四节气计算（纯算法，无依赖，Java 8+） |
| `holiday-spring-starter` | Spring Boot 自动配置 Starter |
| `holiday-api-j8` | Java 8 / Spring Boot 2.7 兼容 API |
| `holiday-api-j25` | Java 25 / Spring Boot 4 内网 API 服务 |

### 应用

| 应用 | 作用 |
| --- | --- |
| `apps/admin-web` | Vue 3 管理后台 |
| `apps/demo-web` | 浏览器演示应用 |

---

## 农历模块

### 设计概要

- **数据来源**：香港天文台（HKO）/ 紫金山天文台天文年历
- **覆盖范围**：1900-2100（201 年）
- **存储方案**：每年一个 20-bit 整数，201 年仅需 ~800 字节
- **编码格式**：
  - bit 0-3: 闰月月份（0=无闰月，1-12=闰几月）
  - bit 4: 闰月天数（0=29天，1=30天）
  - bit 5-16: 1-12月天数（0=29天，1=30天）
- **基准日**：1900-01-31（庚子年正月初一）
- **线程安全**：纯函数设计，可安全并发

### TypeScript 用法

```ts
import { solarToLunar, lunarToSolar } from '@holiday/lunar';

// 公历 → 农历
const info = solarToLunar(2025, 1, 29);
// => { year: 2025, month: 1, day: 1, isLeapMonth: false,
//      ganZhiYear: '乙巳年', shengXiao: '蛇', monthName: '正月', dayName: '初一' }

// 农历 → 公历（非闰月）
const [y, m, d] = lunarToSolar(2025, 1, 1);
// => [2025, 1, 29]

// 农历 → 公历（闰月，需指定 isLeapMonth=true）
const [y2, m2, d2] = lunarToSolar(2023, 2, 1, true);
// => [2023, 3, 22]（闰二月初一）
```

### Java 用法

```java
import com.github.gmkits.holiday.lunar.LunarCalendar;

// 公历 → 农历
LunarInfo info = LunarCalendar.solarToLunar(LocalDate.of(2025, 1, 29));

// 农历 → 公历（非闰月）
LocalDate date = LunarCalendar.lunarToSolar(2025, 1, 1);

// 农历 → 公历（闰月，需指定 isLeapMonth=true）
LocalDate leapDate = LunarCalendar.lunarToSolar(2023, 2, 1, true);
```

### 闰月转换注意事项

> **核心规则**：一个公历日期一定对应唯一一个农历日期；但一个农历日期在闰月年份可能对应两个不同的公历日期。

**公历 → 农历**（`solarToLunar`）：

- 永远是**一对一映射**，无歧义。
- 返回值中 `isLeapMonth` 字段会明确标识该日期是否落在闰月中。
- 例如：`solarToLunar(2023, 3, 22)` → 农历闰二月初一（`isLeapMonth=true`）
- 例如：`solarToLunar(2023, 2, 20)` → 农历二月初一（`isLeapMonth=false`）

**农历 → 公历**（`lunarToSolar`）：

- 需要调用方明确指定 `isLeapMonth` 参数来消除歧义。
- 如果某年有闰四月，那么"四月初一"存在两个不同的公历日期：
  - `lunarToSolar(year, 4, 1, false)` → 正常四月初一的公历日期
  - `lunarToSolar(year, 4, 1, true)` → 闰四月初一的公历日期
- 如果传入的 `isLeapMonth=true` 但该年实际没有该闰月，会抛出异常。

**最佳实践**：

```ts
// 查询某年是否有闰月、闰几月
const leap = leapMonth(2023); // => 2（闰二月）
const leapDays = leapMonthDays(2023); // => 29（闰二月 29 天）

// 安全转换：先检查再转
if (leapMonth(year) === targetMonth) {
  // 该年有闰 targetMonth 月，需要分别处理正月和闰月
  const normalDate = lunarToSolar(year, targetMonth, 1, false);
  const leapDate = lunarToSolar(year, targetMonth, 1, true);
}
```

### 扩展点

- 农历数据通过 `LunarDateInfo` 类型可嵌入 `DayInfo.extensions["lunar"]`
- 如天文台修正数据，只需替换 `LUNAR_INFO` 数组中对应年份的压缩整数

---

## 二十四节气

### 概述

二十四节气是中国传统历法中根据太阳在黄道上的位置划分的 24 个时间节点，每个节气对应太阳黄经的特定度数（每 15° 一个节气）。

本项目在 TypeScript 和 Java 的农历模块中均实现了节气计算，基于 Jean Meeus《Astronomical Algorithms》中的简化 VSOP87 太阳黄经公式，精度约 ±1 天。

### 二十四节气对照表

| 节气 | 太阳黄经 | 大约公历日期 | 含义 |
| --- | --- | --- | --- |
| 小寒 | 285° | 1月5-7日 | 开始进入寒冷季节 |
| 大寒 | 300° | 1月20-21日 | 一年中最冷的时期 |
| 立春 | 315° | 2月3-5日 | 春季开始 |
| 雨水 | 330° | 2月18-20日 | 降水开始增多 |
| 惊蛰 | 345° | 3月5-7日 | 春雷始鸣，蛰虫惊醒 |
| 春分 | 0° | 3月20-22日 | 昼夜等长，春季中点 |
| 清明 | 15° | 4月4-6日 | 天气清爽明朗 |
| 谷雨 | 30° | 4月19-21日 | 雨水增多，有利谷物生长 |
| 立夏 | 45° | 5月5-7日 | 夏季开始 |
| 小满 | 60° | 5月20-22日 | 谷物开始饱满 |
| 芒种 | 75° | 6月5-7日 | 有芒谷物可种植 |
| 夏至 | 90° | 6月21-22日 | 白昼最长，夏季中点 |
| 小暑 | 105° | 7月6-8日 | 开始炎热 |
| 大暑 | 120° | 7月22-24日 | 一年中最热的时期 |
| 立秋 | 135° | 8月7-9日 | 秋季开始 |
| 处暑 | 150° | 8月22-24日 | 暑热结束 |
| 白露 | 165° | 9月7-9日 | 天气转凉，露水凝白 |
| 秋分 | 180° | 9月22-24日 | 昼夜等长，秋季中点 |
| 寒露 | 195° | 10月7-9日 | 露水寒冷 |
| 霜降 | 210° | 10月23-24日 | 开始有霜 |
| 立冬 | 225° | 11月7-8日 | 冬季开始 |
| 小雪 | 240° | 11月22-23日 | 开始降雪 |
| 大雪 | 255° | 12月6-8日 | 降雪增多 |
| 冬至 | 270° | 12月21-23日 | 白昼最短，冬季中点 |

### TypeScript 用法

```ts
import { getSolarTerms, getSolarTerm } from '@holiday/lunar';

// 获取某年全部 24 个节气
const terms = getSolarTerms(2025);
for (const term of terms) {
  console.log(`${term.name}: ${term.date[0]}-${term.date[1]}-${term.date[2]} (黄经 ${term.longitude}°)`);
}
// 小寒: 2025-1-5 (黄经 285°)
// 大寒: 2025-1-20 (黄经 300°)
// ...

// 查询某天是否是节气
const term = getSolarTerm(2025, 3, 20);
// => '春分'（如果当天是节气）
// => null（如果不是节气）
```

### Java 用法

```java
import com.github.gmkits.holiday.lunar.LunarCalendar;
import com.github.gmkits.holiday.lunar.LunarCalendar.SolarTermInfo;

// 获取某年全部 24 个节气
SolarTermInfo[] terms = LunarCalendar.getSolarTerms(2025);
for (SolarTermInfo term : terms) {
    System.out.println(term.getName() + ": " + term.getDate() + " (黄经 " + term.getLongitude() + "°)");
}

// 查询某天是否是节气
String name = LunarCalendar.getSolarTerm(LocalDate.of(2025, 3, 20));
// => "春分" 或 null
```

### 节气算法说明

节气计算使用简化 VSOP87 太阳黄经公式（Jean Meeus《Astronomical Algorithms》第 25 章）：

1. 计算太阳几何平黄经 L0、太阳平近点角 M
2. 计算太阳中心方程 C（基于 M 的三角展开）
3. 叠加章动修正
4. 从目标黄经度数出发，用迭代法（类似牛顿迭代）逼近精确日期

精度约 ±0.01°，对应日期精度约 ±1 天，满足日历显示需求。

---

## 已知问题与常识说明

### 2033 年问题

**背景**：2033 年是农历中一个著名的特殊年份，被称为"2033 年问题"。

**问题本质**：2033 年农历中出现了一种极为罕见的情况——冬至落在农历十一月的最后一天（晦日），而且该年存在闰十一月。由于传统的"无中气置闰"规则在这种边界情况下会产生歧义，历史上不同的历法推算方案给出了不同的闰月位置：

- **方案 A**：闰七月（部分传统万年历采用）
- **方案 B**：闰十一月（紫金山天文台《天文年历》采用，也是当前国家标准）

**本项目的处理方式**：

- 本项目的 `LUNAR_INFO` 数据表采用紫金山天文台/HKO 发布的权威数据，**2033 年为闰十一月**。
- 数据已通过 73,000+ 行 HKO 参照 CSV 进行了全量交叉验证。
- 如果未来天文台修正了数据，只需替换 `LUNAR_INFO` 数组中 2033 年对应的压缩整数即可，无需修改算法代码。

**验证方式**：

```ts
import { leapMonth } from '@holiday/lunar';
console.log(leapMonth(2033)); // => 11（闰十一月）
```

### 数据覆盖范围（1900-2100）

- 农历数据基于天文台发布的实际观测/推算数据，覆盖 1900-01-31 至 2100 年末。
- 超出此范围的日期会抛出 `RangeError`（TypeScript）/ `IllegalArgumentException`（Java）。
- 2100 年之后的农历数据需等天文台发布后才能扩展。

### 朔日估算与数据表的关系

本项目包含两套独立的农历能力：

1. **精确数据表**（`LUNAR_INFO`）：基于天文台权威发布数据的位压缩查表，用于所有实际转换操作。精度为**精确到天**。
2. **天文估算函数**（`estimateNewMoonJDE` / `estimateLunarNewYear`）：基于 Jean Meeus 朔日算法的近似计算，精度约 ±2 天。仅用于**交叉验证**数据表的正确性，不参与日常转换。

### 公历闰年规则

公历（格里历）的闰年判断规则：
- 能被 4 整除但不能被 100 整除的年份是闰年
- 能被 400 整除的年份也是闰年
- 因此 2000 年是闰年，1900 年不是闰年，2100 年不是闰年

本项目中 `isLeapYear()` 函数（位于 `@holiday/spec`）严格遵循此规则。

### 农历闰月规则

农历的闰月由天文观测决定，不是简单的数学公式：

- 农历以朔日（新月）为每月初一
- 每年正常有 12 个月（354 或 355 天），比公历少约 11 天
- 为了与公历（太阳年）对齐，每隔 2-3 年加一个闰月（该年 13 个月，383-385 天）
- 闰月的位置由"无中气置闰"规则决定：两个冬至之间如果有 13 个月，第一个没有中气（太阳黄经为 30° 整数倍的节气）的月份为闰月
- 每个月 29 天（小月）或 30 天（大月），由实际朔日间隔决定

### 天干地支与生肖

- 天干：甲、乙、丙、丁、戊、己、庚、辛、壬、癸（10 个，循环）
- 地支：子、丑、寅、卯、辰、巳、午、未、申、酉、戌、亥（12 个，循环）
- 干支纪年：天干 + 地支，60 年一个循环（如"甲子年"、"乙巳年"）
- 生肖：与地支一一对应（子鼠、丑牛、寅虎、卯兔、辰龙、巳蛇、午马、未羊、申猴、酉鸡、戌狗、亥猪）
- 本项目以 `(year - 4) % 10` 和 `(year - 4) % 12` 计算，其中"年"为农历年。

### 其他已知特殊年份

| 年份 | 特殊性 | 说明 |
| --- | --- | --- |
| 1900 | 数据起始年 | 庚子年，基准日 1900-01-31 为正月初一 |
| 2033 | 闰十一月争议 | 见上文"2033 年问题" |
| 2034 | 无春年（盲年） | 整个农历年内没有立春节气 |
| 2100 | 数据终止年 | 超出范围需等天文台发布新数据 |

---

## 查询能力与性能优化

### 农历模块算法优化

- **预计算年天数表 + 前缀和**：模块初始化时构建 `YEAR_DAYS_CACHE[]` 和 `CUMULATIVE_DAYS[]`
- `yearDays()` 从 O(12) 循环优化为 O(1) 查表
- `solarToLunar()` 年份定位从 O(n) 逐年扫描优化为 O(log n) 二分查找
- `lunarToSolar()` 年份累计从 O(n) 逐年累加优化为 O(1) 前缀和查表
- 位压缩存储：每年 20-bit，201 年仅 ~800 字节

### 节气计算

- 使用简化 VSOP87 太阳黄经公式 + 迭代逼近
- 精度约 ±0.01°（对应日期精度 ±1 天）
- 每个节气计算通常 3-5 次迭代收敛

### TypeScript 查询内核

- 预计算 `dayIndex -> month/day` 映射，避免重复月份换算
- 对 bundle 建立惰性查询视图，整年查询不再重复组装对象
- 对名称列表与标签列表增加轻量级缓存，减少字符串重复解析
- 区间查询按"按年分段"处理，减少跨年范围内重复 bundle 加载
- 加入 bundle 并发加载去重，避免同一 `(region, year)` 被重复读取
- 月查询 `getMonth()`、工作日统计 `countWorkdays()`、下个假期 `getNextHoliday()`

### Java 查询内核

- `HdayBundle` 初始化阶段预构建 `DayInfo[]` 查询视图
- 单日、区间、整年查询直接复用预构建结果
- API 范围/整年查询改为 bundle 级批量路径，不再逐天调用单日接口
- 名称与标签解析只在 bundle 构建时完成一次
- 月查询 `getMonth()`、工作日统计 `countWorkdays()`、下个假期 `getNextHoliday()`

### 农历模块性能优化详解

农历转换模块在 TS 和 Java 中均做了以下数学与算法优化：

1. **年天数预计算表**：模块加载 / 类初始化时一次性构建所有 201 年的天数缓存，`yearDays()` 从 O(12) 循环降至 O(1) 查表。

2. **前缀和数组 + 二分查找**：预构建从 1900 到每个年份的累计天数前缀和，`solarToLunar()` 的年份定位从 O(n) 逐年累减优化为 O(log n) 二分查找。对于 2100 年附近的日期，查找步数从 ~200 次降至 ~8 次。

3. **`lunarToSolar()` 年份累计 O(1)**：利用前缀和直接获取目标年之前的总天数，替代原先的逐年循环累加。

4. **位压缩存储**：每年一个 20-bit 整数，201 年仅占 ~800 字节，远小于逐天映射方案的 73,000+ 字节。

5. **纯函数无状态设计**：所有方法线程安全，可无锁并发，适合虚拟线程环境。

### 节假日查询内核优化

1. **`HdayBundle` 预构建视图**：`DayInfo[]` 在 bundle 加载时一次性构建完毕，后续所有查询（单日、区间、月、年）均零分配复用。

2. **LRU 缓存**：bundle 按 `(region, year)` 粒度缓存，避免重复 IO 与解析。

3. **批量路径**：区间/年/月查询直接走 `getRange()` 切片，不逐日调用单日接口。

---

## 快速开始

### 1. 安装与构建

#### TypeScript 工作区

```bash
corepack enable
pnpm install
pnpm run build
pnpm run lint
pnpm run test
```

#### Java（Maven）

```bash
cd java
mvn clean test              # JDK 17 模块（spec + core + lunar + starter + api-j8）
mvn clean test -Pj25        # JDK 25 API 模块需要 JDK 25+
```

### 2. TypeScript 查询示例

```ts
import { createHolidayService } from '@holiday/core';

const service = createHolidayService({
  dataPath: './data/bundles',
  defaultRegion: 'CN',
});

const day = await service.getDayInfo('2025-10-01');
const range = await service.getRange('2025-10-01', '2025-10-08');
const year = await service.getYear(2026);
const month = await service.getMonth(2025, 10);
const workdays = await service.countWorkdays('2025-10-01', '2025-10-31');
const nextHoliday = await service.getNextHoliday('2025-07-01');
```

### 3. Java 查询示例

```java
import com.github.gmkits.holiday.core.HolidayService;
import com.github.gmkits.holiday.core.HolidayServiceBuilder;

import java.nio.file.Paths;
import java.time.LocalDate;

HolidayService service = new HolidayServiceBuilder()
        .defaultRegion("CN")
        .dataPath(Paths.get("./data/bundles"))
        .build();

service.getDayInfo("CN", LocalDate.of(2025, 10, 1));
service.getRange("CN", LocalDate.of(2025, 10, 1), LocalDate.of(2025, 10, 8));
service.getYear("CN", 2026);
service.getMonth("CN", 2025, 10);
service.countWorkdays("CN", LocalDate.of(2025, 10, 1), LocalDate.of(2025, 10, 31));
service.getNextHoliday("CN", LocalDate.of(2025, 7, 1));
```

---

## API 服务部署

### 兼容服务：holiday-api-j8

适合需要继续运行 Java 8 / Spring Boot 2.7 的场景。

```bash
cd java
mvn -pl holiday-api-j8 spring-boot:run
```

### 新服务：holiday-api-j25

适合内网部署、缓存预热、统一返回、OpenAPI、Actuator 等规范化需求。

```bash
cd java
mvn -Pj25 -pl holiday-api-j25 spring-boot:run
```

默认能力包括：

- 单日、区间、整年、月份查询
- 工作日统计
- 下一个法定节假日查找
- 支持地区查询
- 版本信息查询
- manifest 查询
- bundle 元信息查询
- 缓存清理 / 预热 / manifest 重载运维接口
- Actuator 健康检查
- Swagger / OpenAPI 文档

### 主要接口（holiday-api-j25）

| 接口 | 说明 |
| --- | --- |
| `GET /api/v2/day` | 查询单日 |
| `GET /api/v2/range` | 查询区间 |
| `GET /api/v2/year` | 查询整年 |
| `GET /api/v2/month` | 查询指定月份 |
| `GET /api/v2/workday-count` | 统计区间内工作日天数 |
| `GET /api/v2/next-holiday` | 查找下一个法定节假日 |
| `GET /api/v2/regions` | 查询支持地区 |
| `GET /api/v2/version` | 查询版本信息 |
| `GET /api/v2/manifest` | 读取 manifest |
| `GET /api/v2/bundles/{regionCode}/{year}/metadata` | 查询 bundle 元信息 |
| `POST /api/v2/ops/cache/clear` | 清理缓存 |
| `POST /api/v2/ops/cache/warmup` | 预热缓存 |
| `POST /api/v2/ops/manifest/reload` | 重载 manifest |

---

## 编译器 CLI

```bash
holiday-compiler validate --input data/canonical/CN/2025.canon.json
holiday-compiler materialize --input data/canonical/CN/2025.canon.json --output data/materialized/CN/2025.year.json
holiday-compiler compile --input data/materialized/CN/2025.year.json --output data/bundles/CN/2025.hday
holiday-compiler build-manifest --bundles-dir data/bundles --output data/manifest.json
holiday-compiler inspect --bundle data/bundles/CN/2025.hday
```

---

## 目录结构

```text
cn-holiday-kit/
├── spec/                      # 规范、格式、Schema、接口约定
├── data/                      # 原始数据、规范数据、物化数据、bundle 与 manifest
├── packages/                  # TypeScript 包
│   ├── ts-spec/               # 共享类型与常量
│   ├── ts-core/               # 查询 SDK
│   ├── ts-lunar/              # 农历转换 + 节气计算模块
│   ├── ts-compiler/           # 编译器
│   ├── ts-web-client/         # HTTP 客户端
│   └── ts-vue/                # Vue 组件
├── java/                      # Java 多模块工程（Maven 构建）
│   ├── holiday-spec-java/     # 共享类型
│   ├── holiday-core-java/     # 查询核心
│   ├── holiday-lunar-java/    # 农历转换 + 节气计算
│   ├── holiday-spring-starter/
│   ├── holiday-api-j8/
│   └── holiday-api-j25/
├── apps/                      # Web 应用
├── examples/                  # 示例代码
├── tests/                     # Golden 数据与跨语言对比脚本
└── .github/workflows/ci.yml   # CI 工作流
```

---

## 规范文档

- `spec/holiday-spec.md`：主规范
- `spec/bundle-format.md`：`.hday` 二进制格式
- `spec/api-contract.md`：HTTP API 合同
- `spec/enums.md`：枚举字典
- `spec/holiday-json-schema/`：JSON Schema 定义

---

## Java 版本兼容性说明

| 模块 | 最低 Java 版本 | Spring Boot 版本 | 说明 |
| --- | --- | --- | --- |
| `holiday-spec-java` | Java 8 | — | 纯 DTO 与枚举 |
| `holiday-core-java` | Java 8 | — | 查询核心，依赖 Guava |
| `holiday-lunar-java` | Java 8 | — | 纯算法，无外部依赖（仅 Lombok） |
| `holiday-spring-starter` | Java 8 | — | 自动配置 |
| `holiday-api-j8` | Java 8 | 2.7.18 | 兼容旧环境 |
| `holiday-api-j25` | Java 25 | 4.0.5 | 需要 JDK 25+，独立 profile 构建 |

- 默认构建（`mvn test`）仅编译 Java 8 兼容模块，需要 JDK 17+ 运行 Maven。
- J25 模块通过 `-Pj25` profile 激活，需要 JDK 25+。
- 所有 Java 8 模块严格使用 Java 8 API（如 `ImmutableList.of()` 来自 Guava 而非 `List.of()`）。

---

## CI 工作流

当前 CI 配置（`.github/workflows/ci.yml`）包含以下任务：

| 任务 | 说明 |
| --- | --- |
| `spec-validate` | 验证 JSON Schema 格式正确性 |
| `compiler-test` | TypeScript 全量构建 + 类型检查 + 测试 + 编译器流水线验证 |
| `java-build` | Java 多版本矩阵构建（JDK 17 + JDK 25） |
| `node-build` | Node.js 多版本矩阵测试（18, 20, 22） |
| `bundle-verify` | Bundle 完整性校验（魔数 + SHA256） |

---

## 构建与测试

```bash
# TypeScript
pnpm run build        # 构建所有 TS 包
pnpm run lint         # TypeScript 类型检查
pnpm run test         # 运行所有 TS 测试

# Java（需要 JDK 17+）
cd java
mvn -B clean test                           # 测试 Java 8 模块
mvn -B clean test -pl holiday-lunar-java    # 仅测试农历模块
mvn -B clean test -pl holiday-core-java -am # 测试核心模块及其依赖
mvn -B clean test -Pj25                     # 测试 J25 模块（需要 JDK 25+）
```

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
