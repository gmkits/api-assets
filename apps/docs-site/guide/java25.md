# Java 25 SDK

`holiday-sdk-j25` 是无 Spring 依赖的纯 JDK 客户端，对内的 `holiday-api-j25` 服务端已经全面虚拟线程化。

## 安装（Maven）

```xml
<dependency>
  <groupId>com.github.gmkits</groupId>
  <artifactId>holiday-sdk-j25</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## 三种调用形态

### 1. 同步

```java
DayInfo d = client.getDay(LocalDate.of(2025, 1, 1));
```

### 2. 异步（CompletableFuture）

```java
client.getDayAsync(LocalDate.of(2025, 1, 1))
      .thenAccept(System.out::println);
```

### 3. 批量（虚拟线程并行 fan-out）

```java
List<HolidayClient.BatchItem> items = client.batchDays(
        List.of(LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 5, 1),
                LocalDate.of(2025, 10, 1)));

for (var item : items) {
    if (item.isSuccess()) {
        System.out.println(item.date() + " → " + item.data().statutoryHoliday());
    } else {
        System.err.println(item.date() + " ✗ " + item.error());
    }
}
```

> 当 `java.util.concurrent.StructuredTaskScope` 在未来 JDK 转正后，可平滑替换为
> `StructuredTaskScope.open(Joiner.awaitAll())`。

## Builder 选项

```java
HolidayClient client = HolidayClient.builder()
        .endpoint("https://holiday.example.com")
        .defaultRegion("CN")
        .timeout(Duration.ofSeconds(3))
        .maxInflight(64)              // 用 Semaphore 限流，避免击穿后端
        .cache(Duration.ofSeconds(60), 128)
        .userAgent("my-app/1.0")
        .build();
```

详见 [虚拟线程与结构化并发](/sdk-j25/virtual-threads)。
