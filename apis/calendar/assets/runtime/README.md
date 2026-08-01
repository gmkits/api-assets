# Calendar runtime assets

这里保存服务和下载 API 使用的同一份不可变离线数据：

- `manifest.json`：统一发布版本、覆盖范围和 SHA-256 清单。
- `calendar/calendar.cdat`：1900–2100 农历描述符和 1901–2100 节气表。
- `holidays/manifest.json`：年度 bundle 哈希、来源版本和大小。
- `holidays/bundles/CN/*.hday`：2000–2026 中国大陆节假日与调休。

运行时文件由 `../source`、golden 和私有 TypeScript 编译器确定性生成。不要直接编辑
二进制；执行 `make -C apis/calendar assets`，并审查源数据与生成差异。
服务启动时还会校验 `releaseVersion` 与 `api-asset.json` 的版本一致；外置
`CALENDAR_ASSET_PATH` 也必须提供完整且匹配的目录，不能只替换单个文件。
