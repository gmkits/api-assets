# `.hday` 二进制格式说明

`.hday` 是仓库运行时查询使用的年度 bundle 格式。设计目标是：

- 可被任意语言直接解析，不依赖 protobuf/thrift
- 体积小，单个地区单年通常只有数 KB
- 固定字节序，跨平台一致
- 允许跳过未知 section，便于后向兼容

所有多字节整数均为 **小端序**，所有字符串均为 **UTF-8**。

## 1. 文件布局

```text
[Header]           32 字节
[Section Table]    sectionCount × 8 字节
[DAY_TABLE]        dayCount × 8 字节
[STRING_TABLE]     变长
[NAME_LIST_TABLE]  变长
[EXT_JSON]         可选
[CRC32]            4 字节
```

## 2. Header（32 字节）

| 偏移 | 大小 | 字段 | 类型 | 说明 |
| --- | --- | --- | --- | --- |
| `0x00` | 4 | `magic` | `char[4]` | 固定为 `HDAY` |
| `0x04` | 1 | `majorVersion` | `u8` | 主版本号，当前为 `1` |
| `0x05` | 1 | `minorVersion` | `u8` | 次版本号，当前为 `0` |
| `0x06` | 2 | `flags` | `u16` | 全局标志位，当前保留为 `0` |
| `0x08` | 2 | `year` | `u16` | 数据所属年份 |
| `0x0A` | 1 | `regionCodeLen` | `u8` | 地区代码字节长度 |
| `0x0B` | 16 | `regionCode` | `char[16]` | UTF-8 地区代码，零填充 |
| `0x1B` | 1 | `calendarSystem` | `u8` | 历法编码 |
| `0x1C` | 2 | `dayCount` | `u16` | 365 或 366 |
| `0x1E` | 2 | `sectionCount` | `u16` | section 数量 |

### Header 约束

1. `magic` 必须为 `HDAY`
2. `regionCode` 最长 16 字节
3. `dayCount` 与年份闰平情况一致
4. 读取器必须忽略未知 `flags`

## 3. Section Table

Header 之后紧跟 `sectionCount` 个 8 字节 section 表项：

| 偏移 | 大小 | 字段 | 类型 | 说明 |
| --- | --- | --- | --- | --- |
| `0x00` | 2 | `type` | `u16` | section 类型 |
| `0x02` | 4 | `offset` | `u32` | section 数据起始偏移 |
| `0x06` | 2 | `length` | `u16` | section 数据长度 |

### 已定义 section

| 编码 | 名称 | 必选 | 说明 |
| --- | --- | --- | --- |
| `0x0001` | `DAY_TABLE` | 是 | 每天的标志与索引 |
| `0x0002` | `STRING_TABLE` | 是 | 去重字符串池 |
| `0x0003` | `NAME_LIST_TABLE` | 是 | 名称/标签列表 |
| `0x0004` | `EXT_JSON` | 否 | 年级扩展 JSON |

读取器必须能跳过未知 section。

## 4. DAY_TABLE

按 1 月 1 日到 12 月 31 日顺序存放，每天一个 8 字节条目：

| 偏移 | 大小 | 字段 | 类型 | 说明 |
| --- | --- | --- | --- | --- |
| `0x00` | 2 | `flags` | `u16` | 当日位标记 |
| `0x02` | 2 | `nameListIndex` | `u16` | 名称列表索引；无则为 `0xFFFF` |
| `0x04` | 2 | `labelListIndex` | `u16` | 标签列表索引；无则为 `0xFFFF` |
| `0x06` | 2 | `extIndex` | `u16` | 预留扩展索引；当前通常为 `0xFFFF` |

### 标志位语义

| 位 | 掩码 | 名称 | 说明 |
| --- | --- | --- | --- |
| 0 | `0x0001` | `IS_HOLIDAY` | 休息日 |
| 1 | `0x0002` | `IS_WORKDAY` | 工作日 |
| 2 | `0x0004` | `IS_WEEKEND` | 默认周末 |
| 3 | `0x0008` | `IS_STATUTORY_HOLIDAY` | 法定节假日 |
| 4 | `0x0010` | `IS_ADJUSTED_WORKDAY` | 调休补班 |
| 5 | `0x0020` | `HAS_NAME` | `nameListIndex` 有效 |
| 6 | `0x0040` | `HAS_LABEL` | `labelListIndex` 有效 |

### 语义约束

1. `IS_HOLIDAY` 与 `IS_WORKDAY` 必须互斥，且二者必有其一
2. `IS_WEEKEND` 表示默认周末，不受调休影响
3. `IS_ADJUSTED_WORKDAY` 必须与 `IS_WORKDAY` 同时出现
4. `HAS_NAME/HAS_LABEL` 为 0 时，对应索引必须是 `0xFFFF`

## 5. STRING_TABLE

`STRING_TABLE` 是一个去重字符串池：

| 偏移 | 大小 | 字段 | 类型 | 说明 |
| --- | --- | --- | --- | --- |
| `0x00` | 2 | `stringCount` | `u16` | 字符串总数 |
| `0x02` | 变长 | `entries` | 数组 | 每个字符串依次存放 |

单个字符串结构：

| 偏移 | 大小 | 字段 | 类型 | 说明 |
| --- | --- | --- | --- | --- |
| `0x00` | 2 | `length` | `u16` | 字节长度 |
| `0x02` | `length` | `data` | bytes | UTF-8 内容 |

常见内容包括 locale、节假日名称、标签名等。

## 6. NAME_LIST_TABLE

`NAME_LIST_TABLE` 用于复用多语言名称和标签列表。

### 表结构

| 偏移 | 大小 | 字段 | 类型 | 说明 |
| --- | --- | --- | --- | --- |
| `0x00` | 2 | `listCount` | `u16` | 列表数量 |
| `0x02` | 变长 | `entries` | 数组 | 每个列表依次存放 |

### 单个列表

每个列表先写一个 `pairCount`，随后写入若干 `(keyStringIndex, valueStringIndex)` 对：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `pairCount` | `u16` | 键值对数量 |
| `keyStringIndex` | `u16` | locale 字符串索引；标签场景使用 `0xFFFF` |
| `valueStringIndex` | `u16` | 具体名称或标签值 |

### 约定

- 节假日名称使用 `locale -> 名称`
- 标签列表使用 `0xFFFF -> 标签值`

## 7. EXT_JSON

`EXT_JSON` 是可选的年级扩展数据段：

| 偏移 | 大小 | 字段 | 类型 | 说明 |
| --- | --- | --- | --- | --- |
| `0x00` | 4 | `jsonLength` | `u32` | JSON 字节长度 |
| `0x04` | `jsonLength` | `jsonData` | bytes | UTF-8 JSON 对象 |

当前编译器写入 `specVersion`、`sourceVersion` 和 `generatedAt`。读取器可以用它们
做审计与版本展示，也必须能够忽略未知字段或整个可选段。

## 8. CRC32

文件最后 4 字节是 CRC32 校验值，覆盖范围为 `[0, fileSize - 4)`。

### 校验步骤

1. 读取整个文件
2. 取最后 4 字节作为已存 CRC32
3. 对前面的所有字节重新计算 CRC32
4. 两者不一致则视为文件损坏

## 9. 与运行时查询的关系

`.hday` 只保存节假日主数据、名称、标签和年级扩展。农历与节气能力当前不直接存入日级二进制表，而是由运行时通过以下方式补齐：

1. 农历：基于 1900-2100 压缩算法表实时计算
2. 节气：基于 HKO 基线离线生成的年度 dayIndex 查表

因此，`DayInfo.lunar`、`DayInfo.solarTerm` 与 `DayInfo.ganZhi` 属于**查询层组合字段**，不是 `.hday` 日条目直接序列化字段。
