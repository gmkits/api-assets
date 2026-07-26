# 前缀和与下一假日索引优化后 4 GiB JMH

环境：

- Apple M2，8 个逻辑 CPU，16 GiB 主机内存
- BellSoft Liberica JDK 8.0.492 与 JDK 25.0.3 LTS
- JMH 1.37、G1、`-Xms4g -Xmx4g`
- 1 × 1 s 预热、3 × 1 s 测量、1 fork
- 外部统一资产：`data/date-assets`

| Benchmark | 线程 | JDK 8 ops/s | JDK 25 ops/s |
| --- | ---: | ---: | ---: |
| `singleDay` | 1 | 32,503,949 | 46,912,942 |
| `isHoliday` | 1 | 32,070,109 | 46,529,791 |
| `month` | 1 | 27,549,996 | 39,212,302 |
| `countWorkdaysForYear` | 1 | 27,354,120 | 32,738,146 |
| `concurrentMixedQueries` | 8 | 2,766,854 | 51,199,704 |

本轮重点是确认年工作日统计从逐日扫描改为前缀和后已进入常数时间路径。
短测量的误差区间较宽，只适合回归门禁，不适合做容量承诺。

JDK 8 使用 full blackhole，JDK 25 使用 compiler blackhole；尤其是混合并发结果不能
直接当作两套 JVM 的严格横向比较。表中是进程内库调用吞吐，不是 HTTP QPS。
`concurrentMixedQueries` 每个 benchmark operation 内含四次服务调用。

命令：

```bash
java -Xms4g -Xmx4g \
  -jar holiday-benchmarks/target/benchmarks.jar \
  -wi 1 -i 3 -w 1s -r 1s -f 1
```
