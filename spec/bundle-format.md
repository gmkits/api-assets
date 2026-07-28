# `.hday` v2 二进制格式

`.hday` 是按“地区 + 公历年”独立替换的节假日运行时资产。v2 直接切换为稀疏覆盖表，
不兼容 v1。所有多字节整数使用小端序，所有文本使用严格 UTF-8，文件末尾始终带 CRC32。

## 1. 文件布局

```text
[Header]             32 B
[Section Directory]  sectionCount × 12 B
[DAY_OVERRIDES]      变长，必选、关键
[STRING_TABLE]       变长，必选、关键
[NAME_LIST_TABLE]    变长，必选、关键
[META_TABLE]         变长，可选
[CRC32]              4 B
```

section 可以按任意顺序放置，但不得与 header、目录、其他 section 或 CRC 重叠。

## 2. Header

| 偏移 | 大小 | 字段 | 类型 | 约束 |
| --- | --- | --- | --- | --- |
| `0x00` | 4 | `magic` | bytes | 固定为 `HDAY` |
| `0x04` | 1 | `majorVersion` | `u8` | 当前为 `2` |
| `0x05` | 1 | `minorVersion` | `u8` | 当前为 `0` |
| `0x06` | 2 | `flags` | `u16` | 当前为 `0` |
| `0x08` | 2 | `year` | `u16` | 大于 `0` |
| `0x0A` | 1 | `regionCodeLength` | `u8` | `1..16`，按 UTF-8 字节计 |
| `0x0B` | 16 | `regionCode` | bytes | 严格 UTF-8，剩余字节必须为 `0` |
| `0x1B` | 1 | `calendarSystem` | `u8` | `0` 公历，`1` 中国农历 |
| `0x1C` | 2 | `dayCount` | `u16` | 必须与该年平闰一致 |
| `0x1E` | 2 | `sectionCount` | `u16` | 大于 `0` |

读取器只接受主版本 `2`。次版本用于保持同一主版本下的可读扩展。

## 3. Section Directory

每个描述符固定 12 字节：

| 偏移 | 大小 | 字段 | 类型 | 说明 |
| --- | --- | --- | --- | --- |
| `0x00` | 2 | `type` | `u16` | section 类型 |
| `0x02` | 2 | `flags` | `u16` | bit 0 为 `CRITICAL` |
| `0x04` | 4 | `offset` | `u32` | 文件绝对偏移 |
| `0x08` | 4 | `length` | `u32` | section 字节长度 |

| 编码 | 名称 | 必选 | flags |
| --- | --- | --- | --- |
| `0x0001` | `DAY_OVERRIDES` | 是 | `CRITICAL` |
| `0x0002` | `STRING_TABLE` | 是 | `CRITICAL` |
| `0x0003` | `NAME_LIST_TABLE` | 是 | `CRITICAL` |
| `0x0004` | `META_TABLE` | 否 | `0` |

未知可选 section 必须跳过；未知 `CRITICAL` section 必须拒绝。同一类型不得重复，
当前未定义的 flags 位必须为 `0`。

## 4. DAY_OVERRIDES

普通周一至周五默认工作，周六、周日默认休息。这里只保存状态被强制覆盖，或带有法定、
调休、名称、标签属性的日期。

```text
count:u16
records[count]
```

单条记录固定 8 字节：

| 偏移 | 大小 | 字段 | 类型 | 说明 |
| --- | --- | --- | --- | --- |
| `0x00` | 2 | `dayIndex` | `u16` | 0 基年内序号，严格升序且唯一 |
| `0x02` | 1 | `state` | `u8` | 状态位 |
| `0x03` | 1 | `reserved` | `u8` | 必须为 `0` |
| `0x04` | 2 | `nameListIndex` | `u16` | 无名称时 `0xFFFF` |
| `0x06` | 2 | `labelListIndex` | `u16` | 无标签时 `0xFFFF` |

`state` 定义：

| 位 | 掩码 | 名称 |
| --- | --- | --- |
| 0 | `0x01` | `FORCE_HOLIDAY` |
| 1 | `0x02` | `FORCE_WORKDAY` |
| 2 | `0x04` | `STATUTORY_HOLIDAY` |
| 3 | `0x08` | `ADJUSTED_WORKDAY` |

两种强制状态必须且只能设置一个；法定属性要求 `FORCE_HOLIDAY`，调休补班要求
`FORCE_WORKDAY`。其余状态位必须为 `0`。

## 5. STRING_TABLE

```text
stringCount:u16
repeat stringCount:
  utf8Length:u16
  utf8Bytes[utf8Length]
```

section 必须恰好消费完毕。任何畸形 UTF-8、长度越界或尾随字节都属于格式错误。

## 6. NAME_LIST_TABLE

```text
listCount:u16
repeat listCount:
  pairCount:u16
  repeat pairCount:
    keyStringIndex:u16
    valueStringIndex:u16
```

节日名称使用 `locale -> name`；标签使用 `0xFFFF -> label`。除 key 的标签哨兵外，
所有索引都必须指向 `STRING_TABLE` 中的有效项。

## 7. META_TABLE

```text
entryCount:u16
repeat entryCount:
  key:u16
  valueStringIndex:u16
```

已定义 key 为 `1=specVersion`、`2=sourceVersion`、`3=generatedAt`。未知 key 必须跳过，
以便在不改变年度主数据的情况下扩展审计信息。

## 8. 完整性与错误语义

最后 4 字节是覆盖 `[0, fileSize - 4)` 的标准 CRC32
（多项式 `0xEDB88320`）。底层 Java/TypeScript 读取器始终验证 CRC32；
发布 manifest 另保存整文件 SHA-256。

读取器必须检查：主版本、CRC、平闰日数、UTF-8、目录边界、重复、重叠、未知关键段、
必选段、完整消费、全部索引以及状态互斥关系。Java 使用 `HdayFormatException.Code`，
TypeScript 使用 `HdayFormatError.code` 暴露相同的稳定错误分类。

## 9. 运行时展开

解析后由星期推导默认状态，并把稀疏覆盖展开为位图和直接索引。农历、节气、干支与节日
是查询层组合字段：农历年度描述符和 1901–2100 节气表来自独立 `calendar.cdat`，
不在每个年度 `.hday` 中重复保存。
