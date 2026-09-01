# Calendar API Demo

该目录提供一个不依赖第三方库的完整客户端示例。需要 Docker、curl 和 Node.js 22.13
或更高版本。

## 一键运行

在仓库根目录执行：

```bash
make demo API=calendar
```

命令会构建 `api-assets/calendar:1.0.0-rc.2`，启动 Compose，等待就绪，依次运行
`curl.sh` 和 `client.mjs`，最后停止容器。设置 `KEEP_DEMO=1` 可以保留服务，地址为
`http://127.0.0.1:8080`。

## 手动运行

```bash
docker compose up --build -d calendar
./apis/calendar/demo/curl.sh
node apis/calendar/demo/client.mjs
docker compose down
```

配置 `UPSTREAM_TOKEN=demo-secret` 后，设置 `CALENDAR_TOKEN=demo-secret` 再运行客户端。
健康检查不需要令牌；业务接口和 `/internal/metrics` 需要 `Authorization: Bearer`。

示例响应（单日查询）：

```json
{
  "date": "2025-10-06",
  "regionCode": "CN",
  "locale": "zh-CN",
  "isHoliday": true,
  "isStatutoryHoliday": true,
  "lunar": { "year": 2025, "month": 8, "day": 15 },
  "holidayNames": ["中秋节"]
}
```
