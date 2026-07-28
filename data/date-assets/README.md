# Unified offline date assets

This directory is the replaceable runtime data package for `cn-holiday-kit`.

```text
date-assets/
├── manifest.json
├── calendar/
│   └── calendar.cdat
└── holidays/
    ├── manifest.json
    └── bundles/CN/<year>.hday
```

- `calendar/calendar.cdat`: CRC-protected lunar descriptors for 1900–2100 and
  compressed authoritative solar-term dates for 1901–2100.
- `holidays/bundles/CN`: replaceable sparse `.hday` v2 bundles for 2000–2026.
- `manifest.json`: byte sizes, supported ranges, and SHA-256 hashes.

The audit sources remain outside the runtime package:

- `data/source/calendar/lunar-years.hex`
- `tests/solar-terms.csv`
- `data/source/CN/holidays.csv`

Rebuild all generated assets with:

```bash
bash scripts/update-bundles.sh
```

Java applications can load this directory with:

```java
HolidayService service = CnHolidayKit.fromAssets(Paths.get("./data/date-assets"));
```

Calendar data initializes once per JVM, so configure the asset root before the
first lunar or solar-term call and restart after replacing `calendar.cdat`.
Holiday bundles are cached by year and can be reloaded with
`HolidayService.clearCache()`.

Do not edit generated bundles, `calendar.cdat`, or manifests by hand.
