# Holiday Data Platform — API Contract v1.0.0

## Base URL

```
{host}/api/v1
```

## Common Conventions

- All responses are JSON with `Content-Type: application/json; charset=utf-8`
- All dates use `YYYY-MM-DD` format
- All enum values use `UPPER_SNAKE_CASE`
- All field names use `lowerCamelCase`
- Error responses use standard format: `{ "error": { "code": "...", "message": "..." } }`

## Query APIs

### GET /api/v1/day

Query a single day's holiday info.

**Parameters:**

| Param | Type | Required | Default | Description |
|---|---|---|---|---|
| `regionCode` | string | No | `CN` | Region code |
| `date` | string | Yes | — | Date in YYYY-MM-DD |

**Response:** `DayInfoDTO`

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

### GET /api/v1/range

Query a date range.

**Parameters:**

| Param | Type | Required | Description |
|---|---|---|---|
| `regionCode` | string | No (default CN) | Region code |
| `from` | string | Yes | Start date YYYY-MM-DD |
| `to` | string | Yes | End date YYYY-MM-DD (inclusive) |

**Response:** `{ "days": DayInfoDTO[] }`

### GET /api/v1/year

Query an entire year.

**Parameters:**

| Param | Type | Required | Description |
|---|---|---|---|
| `regionCode` | string | No (default CN) | Region code |
| `year` | number | Yes | Year (e.g. 2026) |

**Response:** `{ "days": DayInfoDTO[] }`

### GET /api/v1/manifest

Get the current manifest.

**Response:** Full manifest.json content

### GET /api/v1/bundle/{region}/{year}

Download a .hday bundle file.

**Response:** Binary file (`application/octet-stream`)

### GET /api/v1/regions

List supported regions.

**Response:**

```json
{
  "regions": [
    { "code": "CN", "name": { "zh-CN": "中国大陆", "en-US": "Mainland China" } }
  ]
}
```

### GET /api/v1/version

Get current data version info.

**Response:**

```json
{
  "specVersion": "1.0.0",
  "dataVersion": "2025.11.04",
  "publishedAt": "2025-11-04T20:35:00+08:00"
}
```

## Admin APIs

All admin APIs require authentication (implementation-specific).

### POST /api/v1/admin/import

Upload and import a raw source file.

**Request:** `multipart/form-data`

| Field | Type | Description |
|---|---|---|
| `file` | file | Raw source file |
| `sourceType` | string | One of: GOV_NOTICE, ICS_FEED, THIRD_PARTY_JSON, CSV_IMPORT, MANUAL_ENTRY, ENTERPRISE_PATCH |
| `regionCode` | string | Target region |
| `year` | number | Target year |

**Response:**

```json
{
  "success": true,
  "canonicalPath": "canonical/CN/2026.canon.json",
  "warnings": []
}
```

### POST /api/v1/admin/validate

Validate a canonical file.

**Request:**

```json
{
  "regionCode": "CN",
  "year": 2026
}
```

**Response:**

```json
{
  "valid": true,
  "errors": [],
  "warnings": []
}
```

### POST /api/v1/admin/compile

Compile canonical → materialized → bundle.

**Request:**

```json
{
  "regionCode": "CN",
  "year": 2026
}
```

**Response:**

```json
{
  "success": true,
  "bundlePath": "bundles/CN/2026.hday",
  "sha256": "...",
  "crc32": "..."
}
```

### POST /api/v1/admin/publish

Publish compiled bundles (update manifest).

**Request:**

```json
{
  "regionCode": "CN",
  "year": 2026,
  "note": "Annual release"
}
```

**Response:**

```json
{
  "success": true,
  "publishedAt": "2025-11-04T20:35:00+08:00",
  "manifestVersion": "2025.11.04"
}
```

### POST /api/v1/admin/rollback

Rollback to a previous version.

**Request:**

```json
{
  "regionCode": "CN",
  "year": 2026,
  "targetVersion": "2025.10.01"
}
```

**Response:**

```json
{
  "success": true,
  "rolledBackTo": "2025.10.01"
}
```

## Error Codes

| Code | HTTP Status | Description |
|---|---|---|
| `INVALID_DATE` | 400 | Date format is invalid |
| `INVALID_REGION` | 400 | Region code not recognized |
| `DATE_OUT_OF_RANGE` | 404 | No data available for requested date |
| `YEAR_NOT_FOUND` | 404 | No bundle for requested year |
| `BUNDLE_NOT_FOUND` | 404 | Bundle file not found |
| `VALIDATION_FAILED` | 422 | Canonical validation failed |
| `COMPILE_FAILED` | 500 | Compilation error |
| `INTERNAL_ERROR` | 500 | Unexpected server error |

## Rate Limiting

Query APIs: implementation-specific, suggested 1000 req/min.
Admin APIs: implementation-specific, suggested 10 req/min.
