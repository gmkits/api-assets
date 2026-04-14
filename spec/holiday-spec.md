# Holiday Data Platform Specification v1.0.0

> **Status**: Draft  
> **Last Updated**: 2025-07-14  
> **Maintainers**: cn-holiday-kit contributors

---

## Table of Contents

1. [Product Positioning](#1-product-positioning)
2. [Design Principles](#2-design-principles)
3. [Four-Layer Data Model](#3-four-layer-data-model)
4. [Core Concept Model](#4-core-concept-model)
5. [Unified Metadata Specification](#5-unified-metadata-specification)
6. [DayInfoDTO (Frontend/Backend ABI)](#6-dayinfodto-frontendbackend-abi)
7. [Canonical Spec Design](#7-canonical-spec-design)
8. [Lunar Calendar Extension](#8-lunar-calendar-extension)
9. [Manifest Design](#9-manifest-design)
10. [Offline Update Mechanism](#10-offline-update-mechanism)
11. [Performance Strategy](#11-performance-strategy)

---

## 1. Product Positioning

This platform is **not** just an `isHoliday(date)` utility. It is a complete **Holiday Data Platform** composed of five layers:

| Layer | Scope | Examples |
|-------|-------|----------|
| **Data Layer** | Manage raw, canonical, materialized, and runtime data | JSON specs, binary bundles |
| **Tool Layer** | CLI tools, converters, validators, compilers | `holiday-compiler`, `holiday-lint` |
| **Runtime Layer** | Unified query across Java / Node / TypeScript / HTTP API | SDKs, REST endpoints |
| **Frontend Layer** | Admin UI and business calendar components | Holiday admin dashboard, date-picker |
| **Extension Layer** | Lunar calendar, regional inheritance, enterprise custom calendars | `CHINESE_LUNAR`, `CN-SH-ACME` org calendar |

### Key Insight

Every layer has a clear contract boundary. The **Data Layer** owns truth; the **Tool Layer** transforms it; the **Runtime Layer** serves it; the **Frontend Layer** presents it; the **Extension Layer** enriches it. No layer may bypass the one beneath it.

---

## 2. Design Principles

1. **Spec before implementation** — This document is the authority. Code follows the spec, not the other way around.
2. **Pipeline-driven data flow** — Data flows through a strict pipeline:

   ```
   Raw → Canonical → Materialized → Binary Bundle → SDK / API
   ```

   Each stage is independently auditable and diffable.

3. **Client simplicity** — Clients only query pre-computed date results. No client ever performs complex calendar computation (e.g., lunar-to-Gregorian conversion).
4. **Universal parsability** — File formats must be simple enough for **any** programming language to parse without specialized libraries.
5. **Frontend/backend ABI consistency** — The `DayInfoDTO` structure is identical whether served by a Java backend, a Node.js SDK, or consumed directly by a frontend component.

---

## 3. Four-Layer Data Model

```
┌─────────────────────────────────────────────────────────────┐
│                    3.1  Raw Source Layer                     │
│          raw/{region}/{year}-{source}.source.json           │
├─────────────────────────────────────────────────────────────┤
│                  3.2  Canonical Spec Layer                   │
│            canonical/{region}/{year}.canon.json              │
├─────────────────────────────────────────────────────────────┤
│              3.3  Materialized Year Data Layer               │
│           materialized/{region}/{year}.year.json             │
├─────────────────────────────────────────────────────────────┤
│              3.4  Runtime Binary Bundle Layer                │
│               bundles/{region}/{year}.hday                   │
└─────────────────────────────────────────────────────────────┘
```

### 3.1 Raw Source Layer

| Attribute | Description |
|-----------|-------------|
| **Purpose** | Ingest and preserve original data from external sources |
| **Trust level** | Untrusted — formats are inconsistent, quality varies |
| **Sources** | Government notices (政府公告), ICS feeds, third-party JSON, CSV, enterprise patches |
| **Provenance** | Every raw file preserves its origin URL, fetch timestamp, and original format |
| **File pattern** | `raw/{region}/{year}-{source}.source.json` |

**Example path**: `raw/CN/2026-gov-notice.source.json`

Raw files are **never** consumed directly by runtimes. They exist solely for:
- Auditing and traceability
- Re-importing when canonical rules change
- Diffing against updated government publications

### 3.2 Canonical Spec Layer

| Attribute | Description |
|-----------|-------------|
| **Purpose** | The **single source of truth** contract for a given region and year |
| **Trust level** | Trusted — validated, reviewed, version-controlled |
| **File pattern** | `canonical/{region}/{year}.canon.json` |

**Structure overview**:

```json
{
  "meta": { /* CommonMeta — see §5.1 */ },
  "sources": [ /* provenance references */ ],
  "rules": [ /* holiday rules */ ],
  "overrides": [ /* exception overrides */ ],
  "extensions": {}
}
```

**Critical invariant**: All importers (government notice parser, ICS converter, CSV importer, etc.) **must** output Canonical format. No importer may write runtime files directly.

### 3.3 Materialized Year Data Layer

| Attribute | Description |
|-----------|-------------|
| **Purpose** | All rules expanded to concrete Gregorian dates for a specific year and region |
| **Trust level** | Derived — deterministically produced from Canonical |
| **File pattern** | `materialized/{region}/{year}.year.json` |

**Use cases**:
- Debugging rule expansion logic
- Generating diffs between versions
- Validation and golden-file tests
- Cross-language comparison (Java compiler output vs. Node compiler output must match)

**Example entry** (one day within the year file):

```json
{
  "date": "2026-01-01",
  "isHoliday": true,
  "isWorkday": false,
  "isWeekend": false,
  "isStatutoryHoliday": true,
  "isAdjustedWorkday": false,
  "holidayNames": { "zh-CN": ["元旦"], "en-US": ["New Year's Day"] },
  "labels": ["NEW_YEAR", "STATUTORY"]
}
```

### 3.4 Runtime Binary Bundle Layer

| Attribute | Description |
|-----------|-------------|
| **Purpose** | Optimized payload for SDK, API, and frontend direct loading |
| **Trust level** | Derived — deterministically produced from Materialized |
| **File extension** | `.hday` |
| **File pattern** | `bundles/{region}/{year}.hday` |

**Design goals**:
- Minimal size (a few KB per year)
- O(1) lookup by day-of-year index
- No external dependencies to parse
- Integrity verified via SHA-256 and CRC-32 checksums

---

## 4. Core Concept Model

### 4.1 Key Semantics

Each date in the system supports the following queries:

| Query | Type | Description |
|-------|------|-------------|
| `isHoliday` | `boolean` | Whether the date is a day off (休息日) |
| `isWorkday` | `boolean` | Whether the date is a working day (工作日) |
| `isWeekend` | `boolean` | Whether the date falls on a default weekend (周末) |
| `isStatutoryHoliday` | `boolean` | Whether the date is a statutory holiday proper (法定节假日) |
| `isAdjustedWorkday` | `boolean` | Whether the date is an adjusted workday, i.e., a weekend overridden to be a workday (调休补班) |
| `holidayNames` | `Map<locale, string[]>` | Holiday names, multi-language |
| `labels` | `string[]` | Enum labels (e.g., `SPRING_FESTIVAL`, `STATUTORY`) |
| `regionCode` | `string` | Region identifier |
| `calendarSystem` | `string` | Source calendar system (e.g., `GREGORIAN`, `CHINESE_LUNAR`) |
| `sourceVersion` | `string` | Data version stamp |

**Invariants**:
- `isHoliday` and `isWorkday` are **mutually exclusive** and **exhaustive**: exactly one is `true` for any date.
- `isWeekend` reflects the **default** weekend mask, regardless of holiday adjustments. A Saturday that is an adjusted workday still has `isWeekend = true`.
- `isStatutoryHoliday` is a subset of `isHoliday`. A date can be `isHoliday = true` (e.g., a normal Saturday) without being a statutory holiday.

### 4.2 Concept Distinctions

These four concepts are frequently confused but are **semantically distinct**:

| Concept | Chinese | Definition | Example |
|---------|---------|------------|---------|
| **Statutory Holiday** | 法定节假日 | A holiday defined by law or regulation. Applies every year by statute. | New Year's Day (元旦), National Day (国庆节) |
| **Holiday Arrangement** | 放假安排 | A specific year's concrete holiday periods, published annually by the State Council (国务院). May extend statutory holidays with adjacent weekends. | 2026 Spring Festival: Jan 27 – Feb 2 |
| **Adjusted Workday** | 调休补班 | A date that would normally be a rest day (weekend) but is overridden to be a working day to compensate for an extended holiday. | Saturday Jan 24, 2026 made a workday to compensate for Spring Festival |
| **Weekend** | 周末 | System default rest days, determined by `weekendMask`. | Saturday and Sunday by default |

> **Warning**: Weekend ≠ Statutory Holiday. A normal Saturday is a weekend rest day but is **not** a statutory holiday. An adjusted workday is still a weekend day by mask (`isWeekend = true`) but is a working day (`isWorkday = true`).

---

## 5. Unified Metadata Specification

### 5.1 CommonMeta

`CommonMeta` is the shared metadata header used by **all** data layers (Canonical, Materialized, and Bundle).

```json
{
  "specVersion": "1.0.0",
  "bundleId": "{region}-{year}",
  "regionCode": "CN",
  "parentRegionCode": null,
  "year": 2026,
  "validFrom": "2026-01-01",
  "validTo": "2026-12-31",
  "calendarSystem": "GREGORIAN",
  "timezone": "Asia/Shanghai",
  "weekendMask": ["SAT", "SUN"],
  "locales": ["zh-CN", "en-US"],
  "sourceVersion": "2025.11.04",
  "generatedAt": "2025-11-04T20:30:00+08:00",
  "generator": {
    "name": "holiday-compiler",
    "version": "1.0.0"
  },
  "extensions": {}
}
```

**Field descriptions**:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `specVersion` | `string` | Yes | Version of this specification the file conforms to |
| `bundleId` | `string` | Yes | Unique identifier: `{regionCode}-{year}` |
| `regionCode` | `string` | Yes | Region code (see §5.2 for format) |
| `parentRegionCode` | `string \| null` | Yes | Parent region for inheritance, or `null` |
| `year` | `integer` | Yes | Calendar year |
| `validFrom` | `string` | Yes | Start of validity range (`YYYY-MM-DD`) |
| `validTo` | `string` | Yes | End of validity range (`YYYY-MM-DD`) |
| `calendarSystem` | `string` | Yes | Primary calendar system |
| `timezone` | `string` | Yes | IANA timezone identifier |
| `weekendMask` | `string[]` | Yes | Default weekend days |
| `locales` | `string[]` | Yes | Supported locales |
| `sourceVersion` | `string` | Yes | Data version stamp |
| `generatedAt` | `string` | Yes | ISO 8601 generation timestamp with timezone |
| `generator` | `object` | Yes | Tool that generated this file |
| `extensions` | `object` | Yes | Extension data, default `{}` |

### 5.2 Conventions

#### Date Format
- **Always** `YYYY-MM-DD`. No timestamps, no time components.
- Example: `"2026-01-01"`, never `"2026-01-01T00:00:00"`.

#### Timezone
- IANA format only.
- Example: `"Asia/Shanghai"`, never `"UTC+8"` or `"CST"`.

#### Region Codes
Region codes follow a hierarchical scheme:

| Level | Format | Example | Description |
|-------|--------|---------|-------------|
| Country | `{CC}` | `CN` | ISO 3166-1 alpha-2 |
| Province / State | `{CC}-{SUB}` | `CN-SH` | ISO 3166-2 subdivision |
| Organization | `{CC}-{SUB}-{ORG}` | `CN-SH-ACME` | Enterprise custom calendar |

Inheritance flows downward: `CN-SH-ACME` inherits from `CN-SH`, which inherits from `CN`. Overrides at a lower level take precedence.

#### Multi-Language Names
Names are stored as a map of locale to string arrays:

```json
{
  "zh-CN": ["元旦"],
  "en-US": ["New Year's Day"]
}
```

Arrays allow multiple names per locale (e.g., a holiday with both a formal and a colloquial name).

#### Enum Values
- Always `UPPER_SNAKE_CASE` strings. Never numeric codes.
- Examples: `"SPRING_FESTIVAL"`, `"STATUTORY"`, `"GOV_NOTICE"`, `"SAT"`, `"SUN"`.

#### JSON Field Naming
- Always `lowerCamelCase`.
- Examples: `"isHoliday"`, `"regionCode"`, `"weekendMask"`.

#### Boolean Fields
- **Never omitted**. Every boolean field must be explicitly `true` or `false`.
- Rationale: Omitted booleans create ambiguity (`undefined` vs. `false`), causing bugs across languages.

#### Extensions Field
- **Always present**, even if empty. Default value: `{}`.
- Rationale: Ensures forward compatibility. Consumers can always safely access `extensions` without null checks.

---

## 6. DayInfoDTO (Frontend/Backend ABI)

`DayInfoDTO` is the **unified response structure** returned by all SDKs and APIs for a single date query. It is the contract that guarantees frontend/backend ABI consistency.

```json
{
  "date": "2026-01-01",
  "regionCode": "CN",
  "calendarSystem": "GREGORIAN",
  "isHoliday": true,
  "isWorkday": false,
  "isWeekend": false,
  "isStatutoryHoliday": true,
  "isAdjustedWorkday": false,
  "holidayNames": {
    "zh-CN": ["元旦"],
    "en-US": ["New Year's Day"]
  },
  "labels": ["NEW_YEAR", "STATUTORY"],
  "sourceVersion": "2025.11.04",
  "extensions": {}
}
```

**Field descriptions**:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `date` | `string` | Yes | `YYYY-MM-DD` format |
| `regionCode` | `string` | Yes | Region this result applies to |
| `calendarSystem` | `string` | Yes | Calendar system used |
| `isHoliday` | `boolean` | Yes | Whether the date is a day off |
| `isWorkday` | `boolean` | Yes | Whether the date is a working day |
| `isWeekend` | `boolean` | Yes | Whether the date falls on a default weekend |
| `isStatutoryHoliday` | `boolean` | Yes | Whether the date is a statutory holiday |
| `isAdjustedWorkday` | `boolean` | Yes | Whether the date is an adjusted workday (调休补班) |
| `holidayNames` | `Map<string, string[]>` | Yes | Multi-language holiday names, empty map `{}` if none |
| `labels` | `string[]` | Yes | Enum labels, empty array `[]` if none |
| `sourceVersion` | `string` | Yes | Data version stamp |
| `extensions` | `object` | Yes | Extension data, default `{}` |

**Cross-platform guarantee**: A Java SDK, a Node.js SDK, a TypeScript SDK, and an HTTP API endpoint must all return **structurally identical** JSON for the same `(date, regionCode)` query.

---

## 7. Canonical Spec Design

### 7.1 Structure

The canonical file is the **single source of truth** for a given region and year.

```json
{
  "meta": { /* CommonMeta — see §5.1 */ },
  "sources": [ /* provenance references — see §7.2 */ ],
  "rules": [ /* holiday rules — see §7.3 */ ],
  "overrides": [ /* exception overrides — see §7.4 */ ],
  "extensions": {}
}
```

### 7.2 Sources

Each source entry records the provenance of the data:

```json
{
  "id": "gov-notice-2026",
  "type": "GOV_NOTICE",
  "title": "国务院办公厅关于2026年部分节假日安排的通知",
  "url": "https://www.gov.cn/...",
  "publishedAt": "2025-11-04"
}
```

**Source types**:

| Type | Description |
|------|-------------|
| `GOV_NOTICE` | Official government notice (政府公告) |
| `ICS_FEED` | iCalendar feed |
| `THIRD_PARTY_JSON` | Third-party JSON data |
| `CSV_IMPORT` | CSV file import |
| `ENTERPRISE_PATCH` | Enterprise-specific override |
| `MANUAL_ENTRY` | Manually entered by administrator |

### 7.3 Rules

Rules define how holidays are computed. Each rule has a `type` field that determines its structure.

**Supported rule types**:

| Type | Description | Example |
|------|-------------|---------|
| `FIXED_DATE` | A specific Gregorian date | New Year's Day: Jan 1 |
| `DATE_RANGE` | A contiguous range of dates | Spring Festival: Jan 28 – Feb 3 |
| `WEEKDAY_OVERRIDE` | Override a weekday/weekend to workday/holiday | Saturday Jan 24 → workday |
| `LUNAR_DATE` | A date in the Chinese lunar calendar (see §8) | Mid-Autumn Festival: Lunar 8/15 |
| `RECURRENCE` | A recurring pattern (e.g., nth weekday of month) | Thanksgiving (US): 4th Thursday of November |
| `PATCH` | A freeform patch for edge cases | Enterprise-specific adjustments |

**Example — `FIXED_DATE` rule**:

```json
{
  "id": "new-year-2026",
  "type": "FIXED_DATE",
  "sourceId": "gov-notice-2026",
  "date": "2026-01-01",
  "isStatutoryHoliday": true,
  "isHoliday": true,
  "isAdjustedWorkday": false,
  "names": {
    "zh-CN": ["元旦"],
    "en-US": ["New Year's Day"]
  },
  "labels": ["NEW_YEAR", "STATUTORY"]
}
```

**Example — `DATE_RANGE` rule**:

```json
{
  "id": "spring-festival-2026",
  "type": "DATE_RANGE",
  "sourceId": "gov-notice-2026",
  "startDate": "2026-01-28",
  "endDate": "2026-02-03",
  "isStatutoryHoliday": true,
  "isHoliday": true,
  "isAdjustedWorkday": false,
  "names": {
    "zh-CN": ["春节"],
    "en-US": ["Spring Festival"]
  },
  "labels": ["SPRING_FESTIVAL", "STATUTORY"]
}
```

**Example — `WEEKDAY_OVERRIDE` rule**:

```json
{
  "id": "spring-festival-makeup-1",
  "type": "WEEKDAY_OVERRIDE",
  "sourceId": "gov-notice-2026",
  "date": "2026-01-24",
  "isStatutoryHoliday": false,
  "isHoliday": false,
  "isAdjustedWorkday": true,
  "names": {},
  "labels": ["SPRING_FESTIVAL_MAKEUP"]
}
```

### 7.4 Overrides

Overrides have the **same structure** as rules but serve a different purpose: they represent exceptions applied at a lower level in the region hierarchy.

**Evaluation order**:
1. Load parent region's rules.
2. Apply current region's rules (merge/replace by `id`).
3. Apply current region's overrides (highest priority).

**Use cases**:
- A province declares an additional local holiday.
- An enterprise removes a national holiday for operational reasons.
- A regional override adjusts the makeup workday schedule.

---

## 8. Lunar Calendar Extension

### Design Principle

> **Never let clients perform lunar-to-Gregorian computation at runtime.**

Lunar calendar support is handled entirely in the **compilation pipeline**, not in SDKs or APIs.

### Workflow

1. **Declare** in Canonical using `LUNAR_DATE` rule type:

   ```json
   {
     "id": "mid-autumn-2026",
     "type": "LUNAR_DATE",
     "sourceId": "gov-notice-2026",
     "calendarSystem": "CHINESE_LUNAR",
     "month": 8,
     "day": 15,
     "isStatutoryHoliday": true,
     "isHoliday": true,
     "isAdjustedWorkday": false,
     "names": {
       "zh-CN": ["中秋节"],
       "en-US": ["Mid-Autumn Festival"]
     },
     "labels": ["MID_AUTUMN", "STATUTORY"]
   }
   ```

2. **Compile** — The `holiday-compiler` expands `LUNAR_DATE` rules to concrete Gregorian dates in the Materialized layer. The compiler uses a trusted lunar-to-Gregorian conversion table.

3. **Serve** — The Runtime Binary Bundle and SDKs only ever see Gregorian dates. No lunar logic leaks into runtime.

### Rationale

- Lunar-to-Gregorian conversion is complex and locale-sensitive.
- Different languages have different (or no) lunar calendar libraries.
- Pre-computing guarantees cross-language consistency.
- Runtime ABI is **never polluted** by lunar logic.

---

## 9. Manifest Design

The manifest file is the **index** that SDKs and APIs use to discover and load bundles.

```json
{
  "specVersion": "1.0.0",
  "bundleFormatVersion": "1.0.0",
  "defaultRegion": "CN",
  "publishedAt": "2025-11-04T20:35:00+08:00",
  "bundles": {
    "CN": {
      "2026": {
        "file": "bundles/CN/2026.hday",
        "sha256": "a1b2c3d4e5f6...",
        "crc32": "DEADBEEF",
        "size": 4096,
        "sourceVersion": "2025.11.04"
      },
      "2025": {
        "file": "bundles/CN/2025.hday",
        "sha256": "f6e5d4c3b2a1...",
        "crc32": "CAFEBABE",
        "size": 4012,
        "sourceVersion": "2024.11.08"
      }
    }
  }
}
```

**Field descriptions**:

| Field | Type | Description |
|-------|------|-------------|
| `specVersion` | `string` | Specification version |
| `bundleFormatVersion` | `string` | Binary bundle format version |
| `defaultRegion` | `string` | Default region code when none specified |
| `publishedAt` | `string` | ISO 8601 publish timestamp |
| `bundles` | `object` | Nested map: `regionCode → year → bundle descriptor` |

**Bundle descriptor fields**:

| Field | Type | Description |
|-------|------|-------------|
| `file` | `string` | Relative path to the `.hday` bundle file |
| `sha256` | `string` | SHA-256 hash for integrity verification |
| `crc32` | `string` | CRC-32 checksum for quick validation |
| `size` | `integer` | File size in bytes |
| `sourceVersion` | `string` | Data version stamp |

---

## 10. Offline Update Mechanism

### Architecture

```
┌───────────────────────────────────────────┐
│            Application Startup            │
├───────────────────────────────────────────┤
│  1. Load built-in base bundles            │
│     (from classpath / node_modules)       │
├───────────────────────────────────────────┤
│  2. Check external override directory     │
│     (higher priority than built-in)       │
├───────────────────────────────────────────┤
│  3. Merge manifests                       │
│     (external overrides win on conflict)  │
├───────────────────────────────────────────┤
│  4. Ready to serve queries                │
└───────────────────────────────────────────┘
```

### Built-in Base Bundles

Every SDK ships with a set of base bundles embedded in its distribution:
- **Java**: bundles in classpath resources (`/holiday-data/bundles/`)
- **Node / TypeScript**: bundles in `node_modules/cn-holiday-kit/bundles/`
- **HTTP API**: bundles in the application's data directory

These bundles ensure the SDK works **offline** and **out-of-the-box** without any network access.

### External Override Directory

An external directory can be configured to override or supplement built-in bundles:

```
/etc/holiday-data/          # or configured path
├── manifest.json
└── bundles/
    └── CN/
        └── 2026.hday
```

**Priority**: External bundles **always** take precedence over built-in bundles for the same `(regionCode, year)`.

### Update Package Format

An update package is a ZIP archive containing:

```
holiday-update-2026.zip
├── manifest.json           # partial manifest with only updated bundles
└── bundles/
    └── CN/
        └── 2026.hday
```

### Hot Update Sequence

The hot update process is **atomic** and **safe**:

1. **Write** new bundles to a staging directory.
2. **Verify** integrity: check SHA-256 and CRC-32 for every bundle in the update manifest.
3. **Atomic replace** — Swap the manifest pointer from the old directory to the staging directory.
4. **Clear cache** — Invalidate all in-memory LRU caches.
5. **Effective immediately** — New queries hit the updated bundles.

**Failure handling**: If verification fails at step 2, the update is **rejected** entirely. The old bundles remain in effect. No partial updates.

---

## 11. Performance Strategy

### Query Path

```
Client query (date, regionCode)
  │
  ▼
Read manifest.json
  │
  ▼
Resolve bundle file for (regionCode, year)
  │
  ▼
Lazy-load year bundle (if not cached)
  │
  ▼
dayOfYear index → read DAY_TABLE entry
  │
  ▼
Assemble DayInfoDTO
  │
  ▼
Return to client
```

### Memory Budget

| Item | Size | Notes |
|------|------|-------|
| One day record | ~50–200 bytes | Depends on name/label count |
| One year bundle (365/366 days) | ~2–8 KB | After binary encoding |
| LRU cache (16 bundles) | ~32–128 KB | Typical workload |
| LRU cache (64 bundles) | ~128–512 KB | High-cardinality regions |

### Caching Strategy

- **Cache key**: `(regionCode, year)` tuple.
- **Eviction**: LRU (Least Recently Used).
- **Default capacity**: 16 bundles (configurable, recommended range: 16–64).
- **Cache invalidation**: On hot update (see §10), all entries are cleared atomically.

### Lookup Complexity

| Operation | Complexity | Notes |
|-----------|-----------|-------|
| Manifest lookup | O(1) | Hash map by region + year |
| Day lookup within bundle | O(1) | Direct array index by day-of-year |
| Bundle load (cold) | O(n) | n = days in year (365/366), one-time cost |
| Bundle load (warm) | O(1) | LRU cache hit |

### Concurrency

- Manifest and bundles are **immutable once loaded**. No read locks required.
- Hot update uses **atomic pointer swap** — readers see either the old or the new version, never a partial state.
- LRU cache operations use a lightweight lock (or lock-free structure, implementation-dependent).

---

## Appendix A: File Path Summary

| Layer | Pattern | Example |
|-------|---------|---------|
| Raw Source | `raw/{region}/{year}-{source}.source.json` | `raw/CN/2026-gov-notice.source.json` |
| Canonical Spec | `canonical/{region}/{year}.canon.json` | `canonical/CN/2026.canon.json` |
| Materialized | `materialized/{region}/{year}.year.json` | `materialized/CN/2026.year.json` |
| Runtime Bundle | `bundles/{region}/{year}.hday` | `bundles/CN/2026.hday` |
| Manifest | `manifest.json` | `manifest.json` |

## Appendix B: Enum Reference

### Calendar Systems

| Value | Description |
|-------|-------------|
| `GREGORIAN` | Standard Gregorian calendar |
| `CHINESE_LUNAR` | Chinese lunisolar calendar (农历) |

### Day-of-Week

| Value | Description |
|-------|-------------|
| `MON` | Monday |
| `TUE` | Tuesday |
| `WED` | Wednesday |
| `THU` | Thursday |
| `FRI` | Friday |
| `SAT` | Saturday |
| `SUN` | Sunday |

### Rule Types

| Value | Description |
|-------|-------------|
| `FIXED_DATE` | A specific Gregorian date |
| `DATE_RANGE` | A contiguous range of Gregorian dates |
| `WEEKDAY_OVERRIDE` | Override default weekday/weekend behavior |
| `LUNAR_DATE` | A date in the Chinese lunar calendar |
| `RECURRENCE` | A recurring pattern |
| `PATCH` | Freeform patch for edge cases |

### Source Types

| Value | Description |
|-------|-------------|
| `GOV_NOTICE` | Government notice (政府公告) |
| `ICS_FEED` | iCalendar feed |
| `THIRD_PARTY_JSON` | Third-party JSON data |
| `CSV_IMPORT` | CSV file import |
| `ENTERPRISE_PATCH` | Enterprise-specific override |
| `MANUAL_ENTRY` | Manual entry by administrator |

### Common Labels

| Value | Description |
|-------|-------------|
| `STATUTORY` | Statutory holiday (法定节假日) |
| `NEW_YEAR` | New Year's Day (元旦) |
| `SPRING_FESTIVAL` | Spring Festival (春节) |
| `TOMB_SWEEPING` | Tomb-Sweeping Day (清明节) |
| `LABOUR_DAY` | Labour Day (劳动节) |
| `DRAGON_BOAT` | Dragon Boat Festival (端午节) |
| `MID_AUTUMN` | Mid-Autumn Festival (中秋节) |
| `NATIONAL_DAY` | National Day (国庆节) |
| `SPRING_FESTIVAL_MAKEUP` | Spring Festival makeup workday |

---

*End of specification.*
