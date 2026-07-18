# Unified offline date assets

This directory is the replaceable runtime data package for `cn-holiday-kit`.

```text
date-assets/
├── manifest.json
├── calendar/
│   ├── lunar-years.hex
│   └── solar-terms.csv
└── holidays/
    ├── manifest.json
    └── bundles/CN/<year>.hday
```

- `calendar/lunar-years.hex`: compressed lunar-year descriptors for 1900–2100.
- `calendar/solar-terms.csv`: 24 solar-term dates per year for 1900–2100.
- `holidays/bundles/CN`: replaceable Chinese statutory-holiday bundles.
- `manifest.json`: byte sizes, year ranges, and SHA-256 hashes for calendar assets.

Rebuild it from the canonical repository data:

```bash
node scripts/build-date-assets.mjs
```

Java applications can load this directory with:

```java
HolidayService service = CnHolidayKit.fromAssets(Paths.get("./data/date-assets"));
```

Calendar assets are initialized once per JVM, so configure the asset root before
the first lunar or solar-term call and restart the process after replacing them.
Holiday bundles are cached by year and can be reloaded with
`HolidayService.clearCache()`.
