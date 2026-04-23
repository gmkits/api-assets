# 虚拟线程与结构化并发

cn-holiday-kit 的 JDK 25 路径深度使用虚拟线程（JEP 444），但有几条要点务必避免踩坑。

## ✅ 推荐写法

- **IO 密集（HTTP / DB）**：`Executors.newVirtualThreadPerTaskExecutor()`，每个请求一个虚拟线程。
- **CPU 密集（编译 `.hday` / bitset 聚合）**：固定大小线程池或 `ForkJoinPool.commonPool()`，**不要**放进虚拟线程池。
- **传递上下文**：用 `ScopedValue<RequestContext>`（JDK 21+ preview / 后续转正）替代 `ThreadLocal`，
  避免大量虚拟线程持有冗余引用。

## ⚠️ 注意 pin 现象

旧版本 JDK 的虚拟线程在执行 `synchronized` 块时会 **pin 载体线程**（Carrier Thread Pinning）。
JDK 24 起已解除。我们：

- 在 SDK 与服务端核心模块统一使用 `ReentrantLock` / `StampedLock`；
- 提供 `holiday.virtualthread.pin.detect` 指标在 Micrometer 中可观测。

## ⚠️ 限并发

虚拟线程**不会自动限流**，IO 后端（数据库连接池、上游 HTTP）会被压垮。

```java
HolidayClient client = HolidayClient.builder()
        .endpoint(...)
        .maxInflight(64)   // 用 Semaphore 限并发
        .build();
```

## 批量 fan-out

服务端 `POST /api/v2/days:batch` 与 SDK `batchDays(...)` 等价于：

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    var futures = executor.invokeAll(callables);  // 每个 date 一个虚拟线程
    // 收集结果，单条失败不影响其它结果
}
```

未来 `StructuredTaskScope` 转正后，可平滑替换为：

```java
try (var scope = StructuredTaskScope.open(Joiner.awaitAll())) {
    var subtasks = dates.stream()
            .map(d -> scope.fork(() -> queryOne(d)))
            .toList();
    scope.join();
    // 遍历 subtask.state() 收集结果
}
```

两种形态语义完全一致。
