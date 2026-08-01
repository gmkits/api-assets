# Calendar JMH

构建并在 4 GB 堆下运行全部基准：

```bash
mvn -B -f apis/calendar/runtime/java/pom.xml \
  -pl calendar-benchmarks -am package -DskipTests
java -Xms4g -Xmx4g -XX:+UseG1GC \
  -Dcalendar.assets.path="$PWD/apis/calendar/assets/runtime" \
  -jar apis/calendar/runtime/java/calendar-benchmarks/target/benchmarks-jar-with-dependencies.jar
```

基准覆盖单日查询、状态判断、月查询、年度工作日计数、农历转换、并发混合查询、
冷解析和首次服务查询。结果必须记录机器、JDK、参数和原始 JMH 输出；本机数字不能
直接当作生产容量。

当前 Monorepo 发布候选结果见
[`results/2026-08-02-api-assets-rc1-4g.md`](results/2026-08-02-api-assets-rc1-4g.md)，
迁移前基线见 [`results/2026-07-28-v2-4g.md`](results/2026-07-28-v2-4g.md)。
