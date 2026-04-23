# 数据格式

## `.hday` 二进制 bundle

每个 `(region, year)` 对应一个 `.hday` 文件，按以下结构布局：

```
+----+----+----+----+
| Magic 'HDAY'      |
+----+----+----+----+
| Version (uint16)  |
+----+----+----+----+
| Day count         |
+----+----+----+----+
| Day table offset  |
| String table off  |
| NameList table off|
+----+----+----+----+
| Day Table  ...    |
| String Table ...  |
| NameList Table .. |
+-------------------+
```

- **位字段密集编码**：每天 1 个 `uint8` 即可装下 holiday/workday/weekend/statutoryHoliday/adjustedWorkday 等布尔。
- **字符串表去重**：所有节假日名称、标签共用同一张字符串表，行内只存 `uint16` index。
- **Canonical JSON Schema**：参见 `packages/spec/holiday-bundle.schema.json`，用于 CI 校验。

## 解析

- TS：`@holiday/core/hday-parser` 一次性 `DataView` 读取，结果调用 `Object.freeze` 防止外部修改。
- Java：`holiday-core-java` 提供等价 reader，零反射、纯字节。
