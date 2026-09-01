# Calendar API `1.0.0-rc.2` JMH

运行环境：JDK 21.0.6，`-Xms4g -Xmx4g -XX:+UseG1GC`，单 fork，1 次预热和 1 次测量。
命令：

```bash
java -Xms4g -Xmx4g -XX:+UseG1GC \
  -Dcalendar.assets.path="$PWD/apis/calendar/assets/runtime" \
  -jar apis/calendar/runtime/java/calendar-benchmarks/target/benchmarks-jar-with-dependencies.jar \
  -wi 1 -i 1 -f 1
```

本次输出（同机 smoke/回归参考，不代表生产容量）：

| 场景 | 结果 |
| --- | ---: |
| `singleDay` | 337,420,631 ops/s |
| `isHoliday` | 369,276,304 ops/s |
| `month` | 45,591,943 ops/s |
| `batch366Days` | 734,576 ops/s |
| `batch4096Days` | 27,505 ops/s |
| `holidaySummary` | 82,699 ops/s |
| `workdayStatsForYear` | 1,500,731 ops/s |
| `lunarConversion` | 8,593,597 ops/s |
| `concurrentMixedQueries` | 32,634,622 ops/s |
| `coldBundleParse` | 164.650 us/op |
| `firstServiceQuery` | 267.984 us/op |
