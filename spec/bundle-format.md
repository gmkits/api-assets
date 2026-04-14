# `.hday` Binary Bundle Format Specification v1.0.0

## Overview

The `.hday` format is a compact binary format for storing holiday data for a single year and region. It is designed to be:

- Simple to parse in any language (no protobuf/thrift dependency)
- Cross-platform (fixed endianness)
- Forward-compatible (section table allows unknown sections to be skipped)
- Small (a typical year is under 10 KB)

All multi-byte integers are **little-endian**.
All strings are **UTF-8** encoded.

---

## File Structure

```
[Header]           32 bytes fixed
[Section Table]    sectionCount × 8 bytes
[DAY_TABLE]        dayCount × 8 bytes
[STRING_TABLE]     variable length
[NAME_LIST_TABLE]  variable length
[EXT_JSON]         variable length (optional)
[CRC32]            4 bytes
```

---

## Header (32 bytes)

| Dec Offset | Hex Offset | Size | Field             | Type     | Description                                          |
|------------|------------|------|-------------------|----------|------------------------------------------------------|
| 0          | 0x00       | 4    | `magic`           | char[4]  | Magic bytes: `HDAY` (0x48 0x44 0x41 0x59)           |
| 4          | 0x04       | 1    | `majorVersion`    | u8       | Major format version (currently 1)                   |
| 5          | 0x05       | 1    | `minorVersion`    | u8       | Minor format version (currently 0)                   |
| 6          | 0x06       | 2    | `flags`           | u16      | Global flags (reserved, set to 0)                    |
| 8          | 0x08       | 2    | `year`            | u16      | Calendar year (e.g., 2026)                           |
| 10         | 0x0A       | 1    | `regionCodeLen`   | u8       | Length of region code string in bytes                 |
| 11         | 0x0B       | 16   | `regionCode`      | char[16] | UTF-8 region code, zero-padded (e.g., "CN\0\0...")   |
| 27         | 0x1B       | 1    | `calendarSystem`  | u8       | Calendar system enum (0x00 = GREGORIAN)              |
| 28         | 0x1C       | 2    | `dayCount`        | u16      | Number of day entries (365 or 366)                   |
| 30         | 0x1E       | 2    | `sectionCount`    | u16      | Number of sections in section table                  |

Total: **32 bytes** (0x00–0x1F)

### Header Notes

- `magic` must be exactly `0x48 0x44 0x41 0x59` (ASCII "HDAY"). Files not starting with this magic are not valid `.hday` files.
- `regionCode` is zero-padded to exactly 16 bytes. The `regionCodeLen` field indicates the number of meaningful bytes. Maximum region code length is 16 bytes of UTF-8.
- `dayCount` should be 365 for non-leap years, 366 for leap years.
- `calendarSystem` is an enum. Only `0x00` (GREGORIAN) is defined in v1.0. All other values are reserved.
- `flags` is reserved and must be set to `0x0000` in v1.0. Readers must ignore unknown flag bits.

---

## Section Table

Immediately follows the header at byte offset **32** (0x20). Contains `sectionCount` entries, each 8 bytes.

### Section Table Entry (8 bytes)

| Dec Offset | Hex Offset | Size | Field    | Type | Description                                                                  |
|------------|------------|------|----------|------|------------------------------------------------------------------------------|
| 0          | 0x00       | 2    | `type`   | u16  | Section type code                                                            |
| 2          | 0x02       | 4    | `offset` | u32  | Byte offset from file start to the section body                              |
| 6          | 0x06       | 2    | `length` | u16  | Section length in bytes (max 65 535; for larger sections use EXT_JSON)        |

### Defined Section Types

| Code   | Name            | Required | Description                        |
|--------|-----------------|----------|------------------------------------|
| 0x0001 | DAY_TABLE       | Yes      | Per-day flag and index records     |
| 0x0002 | STRING_TABLE    | Yes      | Deduplicated UTF-8 string pool     |
| 0x0003 | NAME_LIST_TABLE | Yes      | Locale-name pair lists             |
| 0x0004 | EXT_JSON        | No       | Arbitrary extension JSON           |

### Section Table Rules

- Sections may appear in any order in the table, but their body data must not overlap.
- Parsers **must** skip unknown section types gracefully (use `offset` + `length` to jump past).
- A conforming v1.0 file must contain at least sections 0x0001, 0x0002, and 0x0003.

---

## DAY_TABLE Section (type 0x0001)

Contains `dayCount` entries, one per calendar day, ordered from January 1 (index 0) to December 31 (index 364 or 365 for leap years).

### Day Entry (8 bytes)

| Dec Offset | Hex Offset | Size | Field            | Type | Description                                                     |
|------------|------------|------|------------------|------|-----------------------------------------------------------------|
| 0          | 0x00       | 2    | `flags`          | u16  | Day flags (see Flag Bits below)                                 |
| 2          | 0x02       | 2    | `nameListIndex`  | u16  | Index into NAME_LIST_TABLE (0xFFFF if no names)                 |
| 4          | 0x04       | 2    | `labelListIndex` | u16  | Index into NAME_LIST_TABLE for labels (0xFFFF if no labels)     |
| 6          | 0x06       | 2    | `extIndex`       | u16  | Reserved for extensions (0xFFFF if unused)                      |

### Flag Bits

| Bit   | Mask     | Name                   | Meaning                                       |
|-------|----------|------------------------|-----------------------------------------------|
| 0     | 0x0001   | `IS_HOLIDAY`           | This day is a holiday (day off)               |
| 1     | 0x0002   | `IS_WORKDAY`           | This day is a workday                         |
| 2     | 0x0004   | `IS_WEEKEND`           | This day falls on a default weekend           |
| 3     | 0x0008   | `IS_STATUTORY_HOLIDAY` | This day is a statutory holiday proper         |
| 4     | 0x0010   | `IS_ADJUSTED_WORKDAY`  | This day is an adjusted workday (调休补班)     |
| 5     | 0x0020   | `HAS_NAME`             | `nameListIndex` is valid                      |
| 6     | 0x0040   | `HAS_LABEL`            | `labelListIndex` is valid                     |
| 7–15  | 0xFF80   | Reserved               | Must be 0                                     |

### Semantic Rules

- `IS_HOLIDAY` (bit 0) and `IS_WORKDAY` (bit 1) are **mutually exclusive** — exactly one must be set for every day entry.
- `IS_WEEKEND` (bit 2) reflects the **default calendar** (Saturday and Sunday), regardless of any holiday or workday overrides.
- A day can be `IS_WEEKEND` + `IS_WORKDAY` — this represents an adjusted workday that falls on a weekend (调休补班).
- A day can be `IS_STATUTORY_HOLIDAY` + `IS_HOLIDAY` — this is a statutory holiday that is also a day off.
- `IS_ADJUSTED_WORKDAY` (bit 4) indicates a day rescheduled as a workday to compensate for a holiday. It should always appear together with `IS_WORKDAY`.
- `HAS_NAME` and `HAS_LABEL` indicate whether the corresponding index fields point to valid NAME_LIST_TABLE entries.
- When `HAS_NAME` is 0, `nameListIndex` must be set to `0xFFFF`.
- When `HAS_LABEL` is 0, `labelListIndex` must be set to `0xFFFF`.

---

## STRING_TABLE Section (type 0x0002)

A pool of deduplicated UTF-8 strings. Strings are referenced by their 0-based index elsewhere in the file.

### Layout

| Dec Offset | Hex Offset | Size     | Field         | Type  | Description                  |
|------------|------------|----------|---------------|-------|------------------------------|
| 0          | 0x00       | 2        | `stringCount` | u16   | Total number of strings      |
| 2          | 0x02       | variable | `entries`     | array | Packed string entries         |

### String Entry

Each entry is variable-length:

| Dec Offset | Hex Offset | Size     | Field    | Type  | Description              |
|------------|------------|----------|----------|-------|--------------------------|
| 0          | 0x00       | 2        | `length` | u16   | String byte length       |
| 2          | 0x02       | `length` | `data`   | bytes | UTF-8 encoded string     |

Strings are **not** null-terminated. The `length` field provides the exact byte count.

### Typical Strings

Common strings stored in this table include:

- Locale keys: `"zh-CN"`, `"en-US"`, `"ja-JP"`
- Holiday names: `"元旦"`, `"New Year's Day"`, `"春节"`, `"Spring Festival"`
- Label strings: `"NEW_YEAR"`, `"STATUTORY"`, `"SPRING_FESTIVAL"`

### Deduplication

Each unique string should appear only once in the table. All references point to the same entry by index.

---

## NAME_LIST_TABLE Section (type 0x0003)

Maps a name list index to a list of (key, value) string pairs. Used for both locale-specific holiday names and label lists.

### Layout

| Dec Offset | Hex Offset | Size     | Field       | Type  | Description                  |
|------------|------------|----------|-------------|-------|------------------------------|
| 0          | 0x00       | 2        | `listCount` | u16   | Total number of name lists   |
| 2          | 0x02       | variable | `entries`   | array | Packed name list entries      |

### Name List Entry

Each entry begins with a pair count, followed by that many key-value pairs:

| Dec Offset | Hex Offset | Size | Field       | Type | Description              |
|------------|------------|------|-------------|------|--------------------------|
| 0          | 0x00       | 2    | `pairCount` | u16  | Number of pairs in list  |

Followed by `pairCount` pairs, each 4 bytes:

| Dec Offset | Hex Offset | Size | Field             | Type | Description                                                 |
|------------|------------|------|-------------------|------|-------------------------------------------------------------|
| 0          | 0x00       | 2    | `keyStringIndex`  | u16  | Index into STRING_TABLE (locale key, or 0xFFFF for labels)  |
| 2          | 0x02       | 2    | `valueStringIndex`| u16  | Index into STRING_TABLE (name or label value)               |

### Usage: Holiday Names

For holiday names, each pair maps a locale to a localized name:

| `keyStringIndex`            | `valueStringIndex`                  |
|-----------------------------|-------------------------------------|
| → `"zh-CN"` in STRING_TABLE | → `"元旦"` in STRING_TABLE          |
| → `"en-US"` in STRING_TABLE | → `"New Year's Day"` in STRING_TABLE |

### Usage: Labels

For labels, `keyStringIndex` is set to `0xFFFF` (no locale applies), and `valueStringIndex` points to the label string:

| `keyStringIndex` | `valueStringIndex`                |
|-------------------|-----------------------------------|
| 0xFFFF            | → `"NEW_YEAR"` in STRING_TABLE    |
| 0xFFFF            | → `"STATUTORY"` in STRING_TABLE   |

A single day may have multiple names (one per locale) and multiple labels. The DAY_TABLE entry's `nameListIndex` and `labelListIndex` point to separate name list entries.

---

## EXT_JSON Section (type 0x0004)

Optional section containing a UTF-8 encoded JSON object with arbitrary extension data.

### Layout

| Dec Offset | Hex Offset | Size         | Field        | Type  | Description          |
|------------|------------|--------------|--------------|-------|----------------------|
| 0          | 0x00       | 4            | `jsonLength` | u32   | JSON data byte length|
| 4          | 0x04       | `jsonLength` | `jsonData`   | bytes | UTF-8 JSON object    |

### Notes

- The JSON data must be a valid UTF-8 encoded JSON object (i.e., top-level value is `{}`).
- Used for: audit info, build metadata, debug fields, tooling hints, additional per-year metadata.
- Runtime parsers **may** ignore this section entirely. It is informational.
- The 4-byte `jsonLength` prefix allows this section to hold data beyond the 65 535-byte limit of the section table's `length` field. In that case, the section table `length` should be set to the actual section body size if it fits in u16, or the section should be the last section before CRC32 and parsers should use `jsonLength` as the authoritative size.

---

## CRC32 Checksum

The **last 4 bytes** of the file contain a CRC32 checksum.

| Dec Offset         | Hex Offset         | Size | Field  | Type | Description                                      |
|--------------------|--------------------|------|--------|------|--------------------------------------------------|
| `fileSize - 4`     | —                  | 4    | `crc32`| u32  | CRC32 over bytes `[0, fileSize - 4)`             |

- Algorithm: CRC32 as defined by ISO 3309 / ITU-T V.42 (same as used by zlib, PNG, gzip).
- Polynomial: `0xEDB88320` (reversed representation).
- Input: all bytes from offset 0 to `fileSize - 5` (inclusive).
- Stored as **little-endian u32**.

### Verification Procedure

1. Read the entire file into memory.
2. Extract the last 4 bytes as a little-endian u32 — this is the stored checksum.
3. Compute CRC32 over all bytes except the last 4.
4. Compare the computed value with the stored value. If they differ, the file is corrupt.

---

## Example File Layout

For a **2026 CN** bundle with **365 days** and **4 sections**:

```
Dec Offset  Hex Offset  Size (bytes)  Content
──────────  ──────────  ────────────  ───────────────────────────────────────
0           0x0000      32            Header
32          0x0020      32            Section Table (4 sections × 8 bytes)
64          0x0040      2920          DAY_TABLE (365 entries × 8 bytes)
2984        0x0BA8      varies        STRING_TABLE
...         ...         varies        NAME_LIST_TABLE
...         ...         varies        EXT_JSON (optional)
fileSize-4  ...         4             CRC32
```

### Byte-by-Byte Header Example (2026, CN, 365 days, 4 sections)

```
Offset  Hex Bytes                                       Field
──────  ──────────────────────────────────────────────  ─────────────────
0x0000  48 44 41 59                                     magic = "HDAY"
0x0004  01                                              majorVersion = 1
0x0005  00                                              minorVersion = 0
0x0006  00 00                                           flags = 0
0x0008  EA 07                                           year = 2026 (0x07EA LE)
0x000A  02                                              regionCodeLen = 2
0x000B  43 4E 00 00 00 00 00 00 00 00 00 00 00 00 00 00 regionCode = "CN" + padding
0x001B  00                                              calendarSystem = GREGORIAN
0x001C  6D 01                                           dayCount = 365 (0x016D LE)
0x001E  04 00                                           sectionCount = 4 (0x0004 LE)
```

### Section Table Example

```
Offset  Hex Bytes                     Field
──────  ────────────────────────────  ────────────────────────────────────────
0x0020  01 00 40 00 00 00 68 0B      type=0x0001 offset=0x00000040 length=0x0B68
0x0028  02 00 A8 0B 00 00 xx xx      type=0x0002 offset=0x00000BA8 length=varies
0x0030  03 00 xx xx xx xx xx xx      type=0x0003 offset=varies     length=varies
0x0038  04 00 xx xx xx xx xx xx      type=0x0004 offset=varies     length=varies
```

> `0x0B68` = 2920 decimal = 365 × 8 bytes (DAY_TABLE size).

### Total Size Estimate

A typical year file is approximately **4–8 KB**, depending on the number of unique holiday names, locales, and labels.

---

## Versioning

| Change Type                 | Version Bump                        |
|-----------------------------|-------------------------------------|
| Breaking layout change      | Increment `majorVersion`            |
| New optional section type   | Increment `minorVersion`            |
| New flag bit (reserved → defined) | Increment `minorVersion`      |

### Compatibility Rules

- **Unknown sections**: Parsers **must** skip unknown section types using the section table's `offset` and `length` fields. Unknown sections must **not** cause a parse error.
- **Unknown flag bits**: Parsers **must** ignore unknown flag bits (bits 7–15 in v1.0). Unknown bits must **not** cause a parse error.
- **Forward compatibility**: A v1.x parser can read any v1.y file (where y ≥ x) by ignoring unknown sections and flags.
- **Backward compatibility**: A v1.y parser can read any v1.x file (where x ≤ y) because new features are additive.
- **Major version change**: A v2.x file may have an entirely different layout. A v1.x parser must reject files with `majorVersion ≠ 1`.

---

## Cross-Language Parsing Guide

The format requires only the following primitives:

| Primitive                        | Used For                            |
|----------------------------------|-------------------------------------|
| Read u8                          | Version, region code length, enums  |
| Read u16 (little-endian)         | Flags, counts, indices, lengths     |
| Read u32 (little-endian)         | Section offsets, JSON length, CRC32 |
| Read UTF-8 bytes                 | Region code, string table entries   |
| Compute CRC32 (ISO 3309)        | File integrity check                |

No external schema files, code generation tools, or runtime libraries are needed. Any language with basic binary I/O can parse this format.

### Minimal Parsing Steps

1. Read 32-byte header; validate magic and major version.
2. Read `sectionCount` × 8-byte section table entries.
3. For each known section type, seek to `offset` and read `length` bytes.
4. Parse DAY_TABLE: read `dayCount` × 8-byte day entries.
5. Parse STRING_TABLE: read `stringCount`, then each length-prefixed string.
6. Parse NAME_LIST_TABLE: read `listCount`, then each pair-list.
7. Optionally parse EXT_JSON.
8. Verify CRC32 over all bytes preceding the final 4 bytes.
