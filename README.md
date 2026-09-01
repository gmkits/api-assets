# api-assets

面向开发平台的 API 资产 Monorepo。每个 `apis/<id>` 都是一个独立、可验证、可构建为
OCI 镜像的纵向资产；实现语言可以不同，但平台只需要理解同一份 `api-asset.json`、
OpenAPI 和 Make 约定。

当前资产：[`calendar`](apis/calendar/README.md)，提供中国大陆节假日、农历、二十四节气、
干支和离线二进制下载。运行时为 JDK 21，数据完全随镜像发布，无数据库和远程依赖。

## 平台入口

```bash
pnpm install --frozen-lockfile
make verify
make build API=calendar
make image API=calendar VERSION=1.0.0-rc.2
make smoke API=calendar VERSION=1.0.0-rc.2
make demo API=calendar
```

开发平台按以下顺序接入：

1. 扫描 `apis/*/api-asset.json` 并使用 `shared/schemas/api-asset.schema.json` 校验。
2. 导入清单声明的 OpenAPI。
3. 固定源码 commit，执行清单中的构建命令，得到独立 OCI 镜像。
4. 使用清单中的端口、存活、就绪和指标路径部署。
5. 如配置 `UPSTREAM_TOKEN`，平台调用业务 API 和指标端点时注入 Bearer Token。

仓库不发布 Maven/NPM 包、不维护 SDK，也不包含公网用户、限流、计费或套餐逻辑。
这些能力属于前置平台；资产服务只处理确定性的领域查询。

## 本地运行

```bash
docker compose up --build
curl http://127.0.0.1:8080/v1/calendar/dates/2025-10-06
curl http://127.0.0.1:8080/v1/calendar/metadata
```

完整的 curl 和 Node.js 22 客户端演示见 [`apis/calendar/demo`](apis/calendar/demo/README.md)，
也可以用一条命令启动、验证并清理：

```bash
make demo API=calendar
```

配置 `UPSTREAM_TOKEN=secret docker compose up` 后，请求需携带
`Authorization: Bearer secret`。健康检查始终无需鉴权。

## 仓库约定

- API 按领域纵向组织，不按 Java、Go、Python 或 Node.js 建顶级目录。
- 一个 API 只保留一种线上运行时；辅助编译器不能成为第二套业务实现。
- 资产版本独立演进，根仓库没有统一产品版本。
- 所有数据更新必须可离线重建、可审计，并在提交后保持生成结果无差异。
- 新增 API 的完整门槛见 [`docs/adding-an-api.md`](docs/adding-an-api.md)。

许可证见 [LICENSE](LICENSE)，外部数据来源见
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
