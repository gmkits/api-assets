# `calendar.cdat` v1 二进制格式

`calendar.cdat` 是语言无关、可整体替换的精确日历资产。它只包含约 2 KB 的压缩数值，
CSV/HEX 继续作为审计源码，但不进入运行时发布物。全部多字节整数为小端序。

## Header 与目录

Header 固定 16 字节：

| 偏移 | 大小 | 字段 | 约束 |
| --- | --- | --- | --- |
| `0x00` | 4 | magic | `CDAT` |
| `0x04` | 1 | major | `1` |
| `0x05` | 1 | minor | `0` |
| `0x06` | 2 | sectionCount | 当前为 `2` |
| `0x08` | 8 | reserved | 必须为 `0` |

目录项与 `.hday` v2 相同，固定为
`type:u16 + flags:u16 + offset:u32 + length:u32`。两个现有 section 都带
`CRITICAL`：`1=LUNAR_YEARS`，`2=SOLAR_TERMS`。文件末尾 4 字节为覆盖前面全部内容的
标准 CRC32。

## LUNAR_YEARS

```text
startYear:u16 = 1900
endYear:u16   = 2100
count:u16     = 201
reserved:u16  = 0
descriptors[count]:u32
```

年度描述符的低 4 位表示闰月，bit 16 表示闰月大小，其余月份大小位与运行时农历算法一致。

## SOLAR_TERMS

```text
startYear:u16 = 1901
endYear:u16   = 2100
yearCount:u16 = 200
termCount:u8  = 24
reserved:u8   = 0
baseDays[24]:u8
packedYears[yearCount][6]:u8
```

每年 24 个节气各使用 2-bit 偏移：`dayOfMonth = baseDays[index] + offset`。
读取器超出 1901–2100 时必须报告范围错误，不得使用近似天文公式静默回退。
