# `api-assets/calendar` 1.0.0-rc.1 的 4 GiB 验收

这是领域纵向 Monorepo 改造后的同机结果。数据只能用于观察当前提交相对历史实现的
变化，不能当作带 TLS、网关、鉴权和访问日志的生产容量承诺。

## 环境

- Apple M2，8 个逻辑 CPU，16 GiB 主机内存
- BellSoft Liberica JDK 21.0.6，G1，`-Xms4g -Xmx4g`
- JMH 1.37，3 × 2 s 预热、5 × 3 s 测量、1 fork，启用 GC profiler
- Spring Boot 3.5.16、内嵌 Tomcat、本机回环 HTTP
- 运行时资产：`apis/calendar/assets/runtime`

## JMH

| Benchmark | 结果 | 分配量 | 相对迁移前 JDK 21 |
| --- | ---: | ---: | ---: |
| `singleDay` | 352,450,598 ops/s | 约 `10^-5 B/op` | +0.02% |
| `isHoliday` | 393,668,606 ops/s | 约 `10^-5 B/op` | -0.44% |
| `month` | 112,797,178 ops/s | 56 B/op | +9.94% |
| `countWorkdaysForYear` | 196,197,195 ops/s | 约 `10^-5 B/op` | +1.23% |
| `concurrentMixedQueries` | 233,790,667 ops/s | 56 B/op | +8.24% |
| `lunarConversion` | 20,703,167 ops/s | 232 B/op | -12.11% |
| `coldBundleParse` | 44.741 µs/op | 242,624 B/op | 慢 1.92% |
| `firstServiceQuery` | 155.830 µs/op | 282,427 B/op | 快 35.60% |

除农历转换外，热查询和冷加载均在 5% 验收线内。农历结构从二维对象表收敛为扁平
primitive 数组后，最初的最多 13 项扫描测得约 19.45M ops/s。按照基准结果保留了
按需初始化的紧凑日槽表，最终提升到 20.70M ops/s，约快 6.4%；它只在首次农历
转换时分配约 73 KiB，不增加只使用节假日查询的服务实例常驻内存。与历史
23.55M ops/s 相比仍慢约 12.1%，这里选择保留更简单的扁平年度描述符和按需空间，
不预构建大量 `LunarInfo` 对象来换取单项分数。

## HTTP

服务先用 20,000 请求预热，再对
`GET /v1/calendar/dates/2025-10-06?region=CN` 连续执行两轮 100,000 请求、并发 100：

| 轮次 | QPS | 失败 |
| --- | ---: | ---: |
| 1 | 32,333.53 | 0 |
| 2 | 64,204.96 | 0 |

两轮都高于同机发布候选基线 29,952.29–30,201.95 QPS，没有触发 5% 回退线。
第二轮受本机连接复用、CPU 调度和 JIT 稳态影响明显，因此不把 64K 外推为稳定容量；
验收只确认 100,000 请求、并发 100、4 GiB 堆下零错误且未回退。

## 资产体积

- 27 个 `.hday`：24,289 B，平均 899.59 B/年
- `calendar.cdat`：2,088 B
- 年度 bundle 与通用日历二进制合计：26,377 B

## 复现

```bash
mvn -B -f apis/calendar/runtime/java/pom.xml \
  -pl calendar-benchmarks -am package -DskipTests

java -Xms4g -Xmx4g -XX:+UseG1GC \
  -Dcalendar.assets.path="$PWD/apis/calendar/assets/runtime" \
  -jar apis/calendar/runtime/java/calendar-benchmarks/target/benchmarks-jar-with-dependencies.jar \
  -wi 3 -i 5 -w 2s -r 3s -f 1 -prof gc

ab -k -n 100000 -c 100 \
  'http://127.0.0.1:18081/v1/calendar/dates/2025-10-06?region=CN'
```
