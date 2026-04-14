# cn-holiday-kit

**Holiday Data Platform** — A cross-platform holiday toolkit for China (and extensible to other regions).

## Overview

This is not just an `isHoliday(date)` utility — it's a complete holiday data platform with:

- **Specification Layer**: Frozen metadata schemas, binary format spec, API contract
- **Data Layer**: Raw → Canonical → Materialized → Binary Bundle pipeline
- **Tool Layer**: CLI compiler for import, validate, materialize, compile, inspect
- **Runtime Layer**: TypeScript + Java SDKs for querying holiday data from `.hday` bundles
- **API Layer**: Spring Boot REST API service
- **Frontend Layer**: Vue 3 admin console + reusable calendar components
- **Extensibility**: Designed for lunar calendar, regional inheritance, enterprise overrides

## Quick Start

### Install & Build

```bash
# TypeScript packages
pnpm install
pnpm run build

# Java modules
cd java && gradle build
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

### Query holidays (Java)

```java
import com.github.gmkits.holiday.core.HolidayServiceBuilder;
import com.github.gmkits.holiday.core.HolidayService;
import java.time.LocalDate;

HolidayService service = HolidayServiceBuilder.newBuilder()
    .defaultRegion("CN")
    .dataPath(Paths.get("./data/bundles"))
    .build();

DayInfo info = service.getDayInfo(LocalDate.of(2025, 10, 1));
// info.isHoliday() => true
// info.isStatutoryHoliday() => true
```

### REST API

```bash
# Start the API server
cd java && gradle :holiday-api-j8:bootRun

# Query a date
curl 'http://localhost:8080/api/v1/day?regionCode=CN&date=2025-10-01'
```

### Compiler CLI

```bash
holiday-compiler validate    --input data/canonical/CN/2025.canon.json
holiday-compiler materialize --input data/canonical/CN/2025.canon.json --output data/materialized/CN/2025.year.json
holiday-compiler compile     --input data/materialized/CN/2025.year.json --output data/bundles/CN/2025.hday
holiday-compiler build-manifest --bundles-dir data/bundles --output data/manifest.json
holiday-compiler inspect     --bundle data/bundles/CN/2025.hday
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

#### TypeScript

| Package | Description |
|---------|-------------|
| `@holiday/spec` | Shared types, enums, and constants |
| `@holiday/core` | Runtime SDK — load `.hday` bundles and query dates |
| `@holiday/compiler` | Compiler pipeline — validate, materialize, compile, CLI |
| `@holiday/web-client` | Fetch-based HTTP API client |
| `@holiday/vue` | Vue 3 composables and calendar components |

#### Java

| Module | Description |
|--------|-------------|
| `holiday-spec-java` | DTOs, enums, CommonMeta (zero dependencies) |
| `holiday-core-java` | `.hday` reader, query engine, HolidayService |
| `holiday-spring-starter` | Spring Boot AutoConfiguration |
| `holiday-api-j8` | Spring Boot 2.7 REST API service |

#### Apps

| App | Description |
|-----|-------------|
| `admin-web` | Vue 3 admin console for data management |
| `demo-web` | Standalone browser demo |

## Project Structure

```
cn-holiday-kit/
├── spec/                      # Specifications & JSON Schemas
├── data/                      # Holiday data (raw → canonical → materialized → bundles)
├── packages/
│   ├── ts-spec/               # @holiday/spec
│   ├── ts-core/               # @holiday/core
│   ├── ts-compiler/           # @holiday/compiler
│   ├── ts-web-client/         # @holiday/web-client
│   └── ts-vue/                # @holiday/vue
├── java/
│   ├── holiday-spec-java/     # Java type definitions
│   ├── holiday-core-java/     # Java runtime SDK
│   ├── holiday-spring-starter/# Spring Boot starter
│   └── holiday-api-j8/        # REST API service
├── apps/
│   ├── admin-web/             # Admin console (Vue 3)
│   └── demo-web/              # Browser demo
├── scripts/                   # Build & verification scripts
├── examples/                  # Usage examples (ESM, CJS, Java, Vue)
├── tests/
│   ├── golden/                # Golden test corpus
│   └── cross-lang/            # Cross-language parity tests
└── .github/workflows/ci.yml   # CI pipeline
```

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
