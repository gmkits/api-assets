# cn-holiday-kit

`cn-holiday-kit` 是一个面向中国节假日数据的跨平台工具包与数据平台，不只是一个简单的 `isHoliday(date)` 判断函数。

它覆盖了 **规范定义、数据生产、二进制编译、运行时查询、HTTP API、农历转换、前端展示** 的完整链路，适合做：

- 内网节假日查询服务
- Java / TypeScript SDK
- 节假日数据生产与发布流水线
- 农历公历互转
- 运维可观测、可预热、可缓存的 REST API 服务

## 项目定位

本项目当前分成七层：

1. **规范层**：节假日元数据、二进制格式、API 合同、JSON Schema
2. **数据层**：原始数据 → Canonical → Materialized → `.hday` Bundle
3. **工具层**：校验、展开、编译、检查 CLI
4. **运行时层**：TypeScript / Java 查询 SDK
5. **农历层**：公历↔农历转换（1900-2100，位压缩 ~800B）
6. **服务层**：Spring Boot API（兼容层 + Java 25 新服务）
7. **前端层**：Vue 3 管理台与可复用日历组件

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

## 当前模块说明

### TypeScript

| 包名 | 作用 |
| --- | --- |
| `@holiday/spec` | 共享类型、常量与枚举（含农历扩展类型） |
| `@holiday/core` | `.hday` 运行时查询 SDK（含月查询、工作日统计、下个假期） |
| `@holiday/lunar` | 农历转换模块（1900-2100，位压缩 ~800B） |
| `@holiday/compiler` | Canonical 校验、物化、编译、CLI |
| `@holiday/web-client` | HTTP API 客户端 |
| `@holiday/vue` | Vue 3 组合式 API 与日历组件 |

### Java

| 模块 | 作用 |
| --- | --- |
| `holiday-spec-java` | Java 共享 DTO / 枚举定义（含 LunarDateInfo） |
| `holiday-core-java` | `.hday` 读取与高性能查询核心 |
| `holiday-lunar-java` | 农历转换模块（纯算法，无依赖，Java 8+） |
| `holiday-spring-starter` | 旧版 Spring Boot Starter |
| `holiday-api-j8` | Java 8 / Spring Boot 2.7 兼容 API |
| `holiday-api-j25` | Java 25 / Spring Boot 4 内网 API 服务 |

### 应用

| 应用 | 作用 |
| --- | --- |
| `apps/admin-web` | Vue 3 管理后台 |
| `apps/demo-web` | 浏览器演示应用 |

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

const info = solarToLunar(2025, 1, 29);
// => { ganZhiYear: '乙巳年', shengXiao: '蛇', monthName: '正月', dayName: '初一' }

const [y, m, d] = lunarToSolar(2025, 1, 1);
// => [2025, 1, 29]
```

### Java 用法

```java
import com.github.gmkits.holiday.lunar.LunarCalendar;

LunarInfo info = LunarCalendar.solarToLunar(LocalDate.of(2025, 1, 29));
LocalDate date = LunarCalendar.lunarToSolar(2025, 1, 1);
```

### 扩展点

- 农历数据通过 `LunarDateInfo` 类型可嵌入 `DayInfo.extensions["lunar"]`
- 如天文台修正数据，只需替换 `LUNAR_INFO` 数组中对应年份的压缩整数

## 查询能力与性能优化

本次实现已经对核心查询链路做了针对性优化：

### TypeScript 查询内核

- 预计算 `dayIndex -> month/day` 映射，避免重复月份换算
- 对 bundle 建立惰性查询视图，整年查询不再重复组装对象
- 对名称列表与标签列表增加轻量级缓存，减少字符串重复解析
- 区间查询按“按年分段”处理，减少跨年范围内重复 bundle 加载
- 加入 bundle 并发加载去重，避免同一 `(region, year)` 被重复读取
- **新增**：月查询 `getMonth()`、工作日统计 `countWorkdays()`、下个假期 `getNextHoliday()`

### Java 查询内核

- `HdayBundle` 初始化阶段预构建 `DayInfo[]` 查询视图
- 单日、区间、整年查询直接复用预构建结果
- API 范围/整年查询改为 bundle 级批量路径，不再逐天调用单日接口
- 名称与标签解析只在 bundle 构建时完成一次
- **新增**：月查询 `getMonth()`、工作日统计 `countWorkdays()`、下个假期 `getNextHoliday()`

## 快速开始

## 1. 安装与构建

### TypeScript 工作区

```bash
corepack enable
pnpm install
pnpm run build
pnpm run lint
```

### Java — Maven（推荐）

```bash
cd java
mvn test
```

### Java — Gradle

```bash
cd java
./gradlew build
```

> 说明：仓库同时保留 Gradle 和 Maven 构建文件（`pom.xml` + `build.gradle`），两者共存。Maven 为推荐构建方式。

## 2. TypeScript 查询示例

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

## 3. Java 查询示例

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

## API 服务部署

### 兼容服务：holiday-api-j8

适合需要继续运行 Java 8 / Spring Boot 2.7 的场景。

```bash
cd java
./gradlew :holiday-api-j8:bootRun
```

### 新服务：holiday-api-j25

适合内网部署、缓存预热、统一返回、OpenAPI、Actuator 等规范化需求。

```bash
cd java
./gradlew :holiday-api-j25:bootRun
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

## 编译器 CLI

```bash
holiday-compiler validate --input data/canonical/CN/2025.canon.json
holiday-compiler materialize --input data/canonical/CN/2025.canon.json --output data/materialized/CN/2025.year.json
holiday-compiler compile --input data/materialized/CN/2025.year.json --output data/bundles/CN/2025.hday
holiday-compiler build-manifest --bundles-dir data/bundles --output data/manifest.json
holiday-compiler inspect --bundle data/bundles/CN/2025.hday
```

## 目录结构

```text
cn-holiday-kit/
├── spec/                      # 规范、格式、Schema、接口约定
├── data/                      # 原始数据、规范数据、物化数据、bundle 与 manifest
├── packages/                  # TypeScript 包
│   ├── ts-spec/               # 共享类型与常量
│   ├── ts-core/               # 查询 SDK
│   ├── ts-lunar/              # 农历转换模块
│   ├── ts-compiler/           # 编译器
│   ├── ts-web-client/         # HTTP 客户端
│   └── ts-vue/                # Vue 组件
├── java/                      # Java 多模块工程（Gradle + Maven 双轨）
│   ├── holiday-spec-java/     # 共享类型
│   ├── holiday-core-java/     # 查询核心
│   ├── holiday-lunar-java/    # 农历转换
│   ├── holiday-spring-starter/
│   ├── holiday-api-j8/
│   └── holiday-api-j25/
├── apps/                      # Web 应用
├── examples/                  # 示例代码
├── tests/                     # Golden 数据与跨语言对比脚本
└── .github/workflows/ci.yml   # CI 工作流
```

## 规范文档

- `spec/holiday-spec.md`：主规范
- `spec/bundle-format.md`：`.hday` 二进制格式
- `spec/api-contract.md`：HTTP API 合同
- `spec/enums.md`：枚举字典
- `spec/holiday-json-schema/`：JSON Schema 定义

## 当前已知构建说明

- TypeScript：`pnpm run build`、`pnpm run lint` 可通过
- Java Gradle：`cd java && ./gradlew build` 可作为构建入口
- Java Maven：`cd java && mvn test` 可通过（spec + core + lunar）

## 后续规划

1. 完善 `holiday-api-j25` 的容器化与部署模板
2. 增加 manifest / bundle 元数据缓存失效策略
3. 增加更细粒度的基准测试和回归测试
4. 继续推进注释、接口文档与管理后台的中文化
5. 补充更多地区、更多来源的数据导入器
6. 农历数据精度验证与天文台数据定期同步

## 许可证

Apache-2.0
