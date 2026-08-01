# cn-holiday-kit

面向 Java 8+ 的中国日期工具库：一个 JAR、纯离线运行，同时返回工作日状态、
官方放假、法定节假日、农历、二十四节气和常用传统节日。

项目可以直接并入 `gmkit` 作为扩展模块。对外只维护两个稳定边界：

- Java：`com.github.gmkits:cn-holiday-kit` 单一依赖、单一 JAR。
- 通用数据：`data/source/CN/holidays.csv` 为维护源，`.hday` v2 与
  `calendar.cdat` 为语言无关运行时格式。

TypeScript 编译器、Spring API 和内部 Java 子模块只用于构建、测试或部署，
全部不作为独立发布物，避免多仓库和多版本联动。

## 能力与范围

| 能力 | 范围 |
| --- | --- |
| 中国大陆放假与调休 | 2000–2026，离线 bundle |
| 农历双向转换 | 1900–2100 |
| 二十四节气 | 1901–2100，权威表驱动 |
| 节日与纪念日 | 公历、农历、节气联合判定 |
| Java 运行时 | JDK 8 / 17 / 21 / 25 同一 JAR |
| 模块系统 | JDK 8 classpath；JDK 9+ 自动模块 `com.github.gmkits.holiday` |

`isHoliday` 表示“当天休息”，包含普通周末；`isOfficialHoliday` 表示国务院安排的
放假区间；`isStatutoryHoliday` 只表示法定节假日本日；
`isAdjustedWorkday` 表示周末调休补班。节日列表与是否放假相互独立。

## Java 使用

```xml
<dependency>
  <groupId>com.github.gmkits</groupId>
  <artifactId>cn-holiday-kit</artifactId>
  <version>1.0.0-rc1</version>
</dependency>
```

```java
import com.github.gmkits.holiday.CnHolidayKit;
import com.github.gmkits.holiday.core.HolidayService;
import com.github.gmkits.holiday.spec.DayInfo;

import java.time.LocalDate;

HolidayService service = CnHolidayKit.create();
DayInfo day = service.getDayInfo(LocalDate.of(2025, 10, 6));

day.isHoliday();          // true，当天休息
day.isOfficialHoliday();  // true，属于官方放假安排
day.isStatutoryHoliday(); // true，中秋法定日
day.getLunar();           // 农历八月十五
day.getSolarTerm();       // 非节气日为 null
day.getGanZhi();          // 乙、巳、乙巳、蛇
day.getFestivals();       // MID_AUTUMN / 中秋节
```

高频接口包括：

```java
service.getDayInfo(date);
service.getRange(from, to);
service.getYear(2026);
service.countWorkdays(from, to);
service.getNextHoliday(date);
service.isHoliday(date);
service.isWorkday(date);
```

服务和返回值均可安全并发使用。单日查询复用预构建不可变对象；年内工作日计数使用
前缀和，下一法定日使用反向索引，不在热点路径做 JSON 解析、网络请求或逐日扫描。

## 外部资产与热更新

JAR 内置全部离线资产，也可以让业务系统独立替换日期数据：

```java
HolidayService service =
    CnHolidayKit.fromAssets(java.nio.file.Paths.get("./data/date-assets"));
```

替换 `data/date-assets/holidays` 后调用 `service.clearCache()` 即可重载节假日 bundle。
农历和节气资产在 JVM 内初始化一次，替换后应重启进程。

目录结构：

```text
data/
├── source/CN/
│   ├── holidays.csv       # 唯一人工审阅的数据表
│   └── sources.json       # 年度公文、来源版本与校验摘要
└── date-assets/           # 唯一生成物，可整体复制、挂载或替换
```

通用 CSV 规范见 [`spec/cn-holidays-csv.md`](spec/cn-holidays-csv.md)，资产替换规则见
[`data/date-assets/README.md`](data/date-assets/README.md)。

## 简单 API 服务

如需跨语言访问，可在 JDK 17+ 启动内部 Spring Boot 服务（核心库仍兼容 JDK 8）：

```bash
mvn -f java/pom.xml -pl holiday-api -am package
java -Xms512m -Xmx4g -jar \
  java/holiday-api/target/holiday-api-1.0.0-rc1.jar
```

```bash
curl "http://localhost:8080/api/v1/day?date=2025-10-06"
curl "http://localhost:8080/api/v1/range?from=2025-10-01&to=2025-10-08"
curl "http://localhost:8080/api/v1/year?year=2026"
```

建议其他语言读取 `.hday` v2 / `calendar.cdat` 通用二进制，或调用这套 REST API；
仓库不再为每种语言维护一套手写 SDK。格式见
[`spec/bundle-format.md`](spec/bundle-format.md) 和
[`spec/calendar-format.md`](spec/calendar-format.md)，现有 Java/TypeScript 读取器可作为
其他语言实现的错误语义基线。

## 数据准确性

运行时完全离线，网络仅在维护者执行同步脚本时使用。当前生成器固定上游版本：

- 2004–2026：`chinese-days` 日历；
- 2007–2026：再与 `holiday-cn` 中引用的国务院通知逐日交叉核验；
- 2000–2003：历史日历与存档通知，`sources.json` 明确区分
  `RECONSTRUCTED`、`ARCHIVED_NOTICE`、`GOV_NOTICE` 置信等级。

2000 年属于按当年节假日办法和历史日历重建的数据，不能伪装成与近年公文同等级。
每个年度的原始公文链接、上游 commit/version 和 SHA-256 均记录在
`data/source/CN/sources.json`。第三方数据许可见
[`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md)。

## 更新年度数据

需要网络的维护步骤：

```bash
pnpm install
pnpm --filter @holiday/lunar build
node scripts/sync-cn-holidays.mjs
```

之后所有步骤离线且可重复：

```bash
bash scripts/update-bundles.sh
git diff -- data/source data/date-assets
```

同步脚本会并行下载固定版本数据、核对 2007 年以来两套来源的 OFF/WORK 覆盖，
再生成一份 CSV。编译脚本从 CSV 推导 canonical/materialized 中间文件，
中间文件只放在 `target/`，不再维护重复 JSON 副本。

## 构建与验证

```bash
pnpm -r run build
pnpm -r run lint
pnpm -r run test

mvn -f java/pom.xml clean verify
mvn -f java/pom.xml clean verify         # 在 8/17/21/25 上运行同一命令

bash scripts/verify-bundles.sh
node scripts/verify-cn-holiday-data.mjs
bash scripts/verify-calendar-parity.sh
```

CI 使用 BellSoft Liberica JDK 8、17、21、25 跑同一套 Java 测试。严格 Javadoc、
独立单 JAR 消费和 JDK 9+ module-path 消费都属于 `verify` 门禁。

JMH 基准：

```bash
mvn -f java/pom.xml -pl holiday-benchmarks -am package -DskipTests
java -Xms4g -Xmx4g -jar \
  java/holiday-benchmarks/target/benchmarks.jar \
  -wi 3 -i 5 -f 1
```

JMH 的 `thrpt` 是库内查询吞吐，不等于 HTTP 端到端 QPS；API 最大 QPS 还取决于
序列化、连接、日志、限流和机器核数，应另用 wrk/k6 对部署实例压测。
本机 4 GiB 短测结果见
[`java/holiday-benchmarks/results/2026-07-28-v2-4g.md`](java/holiday-benchmarks/results/2026-07-28-v2-4g.md)。

当前 2000–2026 的 27 个 v2 bundle 总计约 24 KB、平均约 900 B；
通用 `calendar.cdat` 为 2,088 B。bundle 内嵌 CRC32，manifest 另以 SHA-256 校验。

## 仓库结构

```text
java/cn-holiday-kit       唯一 Java 发布物与统一入口
java/holiday-*-java       内部源码分区
java/holiday-api          可选 Java 17+ REST 服务
java/holiday-benchmarks   JMH
packages/ts-*             私有编译器、格式与离线查询实现
spec                      通用格式与 JSON Schema
data                      数据源、二进制和可替换资产
scripts                   同步、编译、校验
```

详细 Java 发布边界见 [`java/ARCHITECTURE.md`](java/ARCHITECTURE.md)。

## License

项目代码使用 Apache-2.0。上游数据集保留各自的 MIT 许可与署名。
