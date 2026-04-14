# cn-holiday-kit

**Holiday Data Platform** — A cross-platform holiday toolkit for China (and extensible to other regions).

## Overview

This is not just an `isHoliday(date)` utility — it's a complete holiday data platform with:

- **Specification Layer**: Frozen metadata schemas, binary format spec, API contract
- **Data Layer**: Raw → Canonical → Materialized → Binary Bundle pipeline
- **Tool Layer**: CLI compiler for import, validate, materialize, compile, inspect
- **Runtime Layer**: TypeScript SDK for querying holiday data from `.hday` bundles
- **Extensibility**: Designed for lunar calendar, regional inheritance, enterprise overrides

## Quick Start

### Install

```bash
pnpm install
pnpm run build
```

### Query holidays (TypeScript/Node)

```ts
import { createHolidayService } from '@holiday/core';

const service = createHolidayService({
  dataPath: './data/bundles',
  defaultRegion: 'CN',
});

const info = await service.getDayInfo('2025-10-01');
// {
//   date: '2025-10-01',
//   isHoliday: true,
//   isStatutoryHoliday: true,
//   holidayNames: { 'zh-CN': ['国庆节'], 'en-US': ['National Day'] },
//   labels: ['NATIONAL_DAY', 'STATUTORY'],
//   ...
// }

const isWorkday = await service.isWorkday('2025-01-26'); // true (adjusted workday)
```

### Compiler CLI

```bash
# Validate canonical data
node packages/ts-compiler/dist/esm/cli.js validate --input data/canonical/CN/2025.canon.json

# Materialize (expand rules to daily records)
node packages/ts-compiler/dist/esm/cli.js materialize --input data/canonical/CN/2025.canon.json --output data/materialized/CN/2025.year.json

# Compile to binary bundle
node packages/ts-compiler/dist/esm/cli.js compile --input data/materialized/CN/2025.year.json --output data/bundles/CN/2025.hday

# Build manifest
node packages/ts-compiler/dist/esm/cli.js build-manifest --bundles-dir data/bundles --output data/manifest.json

# Inspect a bundle
node packages/ts-compiler/dist/esm/cli.js inspect --bundle data/bundles/CN/2025.hday
```

## Architecture

### Four-Layer Data Model

```
Raw Source  →  Canonical Spec  →  Materialized Year Data  →  Binary Bundle (.hday)
(untrusted)    (source of truth)   (expanded daily records)   (runtime format)
```

### Key Concepts

| Concept | Description |
|---------|-------------|
| **Statutory Holiday** (法定节假日) | Holiday defined by law |
| **Official Holiday** (放假日) | Day off in the annual arrangement |
| **Adjusted Workday** (调休补班) | Weekend reclassified as workday |
| **Weekend** (周末) | Default rest day per calendar |

### Packages

| Package | Description |
|---------|-------------|
| `@holiday/spec` | Shared TypeScript types, enums, and constants |
| `@holiday/core` | Runtime SDK — load `.hday` bundles and query dates |
| `@holiday/compiler` | Compiler pipeline — validate, materialize, compile, CLI |

## Data

Pre-built data for China (CN) 2025 and 2026 is included in `data/`.

## Specifications

- [`spec/holiday-spec.md`](spec/holiday-spec.md) — Master specification
- [`spec/bundle-format.md`](spec/bundle-format.md) — `.hday` binary format
- [`spec/api-contract.md`](spec/api-contract.md) — HTTP API contract
- [`spec/enums.md`](spec/enums.md) — Enumeration dictionary
- [`spec/holiday-json-schema/`](spec/holiday-json-schema/) — JSON Schemas

## License

Apache-2.0
