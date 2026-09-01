# Calendar API

`calendar` 是 `api-assets` 的第一个独立 API 资产。它把工作日状态、节假日名称、农历、
二十四节气、传统节日和干支合并为一份日期结果，并公开同源的 `.hday`、`calendar.cdat`
离线文件。

## 数据范围

- 中国大陆节假日与调休：2000–2026。
- 公历与农历互转：1900–2100。
- 二十四节气：1901–2100。

运行时只读取 [`assets/runtime`](assets/runtime)；CSV、HEX、来源信息和审计工具位于
[`assets/source`](assets/source) 与 [`tooling`](tooling)。更新数据后运行：

```bash
make -C apis/calendar assets
```

该命令会重建全部 bundle、校验 SHA-256/CRC32，并要求 Git 中的运行时资产完全可复现。
生产镜像使用内置数据。开发或应急场景可将同结构目录只读挂载，并设置
`CALENDAR_ASSET_PATH=/assets`；缺失或损坏会让服务启动失败，替换后需要重启。

## API 与配置

唯一契约是 [`contract/openapi.yaml`](contract/openapi.yaml)。常用入口：

```text
GET /v1/calendar/dates/{date}
GET /v1/calendar/dates?from=&to=
POST /v1/calendar/dates:batch
GET /v1/calendar/months/{year}/{month}
GET /v1/calendar/years/{year}
GET /v1/calendar/workdays/count?from=&to=
GET /v1/calendar/holidays?year=
GET /v1/calendar/holidays/next?from=
GET /v1/calendar/regions
GET /v1/calendar/lunar/from-solar?date=
GET /v1/calendar/solar/from-lunar?year=&month=&day=&leapMonth=
GET /v1/calendar/solar-terms/{year}
GET /v1/calendar/assets/manifest
```

所有集合响应均为 `{region, locale, from, to, count, items}`；批量接口额外返回合并后的
`ranges`。单日查询支持 `locale=zh-CN|en-US` 和严格字段投影 `fields`，英文名称缺失时
回退中文并将 `localeFallback` 设为 `true`。完整可运行示例见 [`demo`](demo/README.md)。

`UPSTREAM_TOKEN` 非空时保护 `/v1/calendar/**` 和 `/internal/metrics`。
`CALENDAR_RELEASE_VERSION` 与 `SOURCE_COMMIT` 由构建平台注入并显示在 metadata。

JMH 位于 `runtime/java/calendar-benchmarks`。基准统一使用 JDK 21 和
`-Xms4g -Xmx4g -XX:+UseG1GC`；历史同机结果只作为回归基线，不代表生产容量。
