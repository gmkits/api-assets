# 新增 API 资产

每个 API 使用 `apis/<id>` 纵向目录，并且必须能够脱离其他业务资产独立构建和部署。

## 必需文件

- `api-asset.json`：版本、运行时、构建命令、容器、运维路径、数据范围和产物。
- `contract/openapi.yaml`：OpenAPI 3.0.3，operationId 稳定且全局唯一。
- `Makefile`：实现 `verify`、`build`、`image`、`smoke`。
- OCI Dockerfile：非 root 运行，支持只读根文件系统，包含健康检查。
- 契约测试：覆盖每个 operation 的成功、参数错误、越界和鉴权行为。
- 资产文档：说明数据来源、更新过程、运行配置和故障语义。

清单中的路径必须位于当前 API 目录，镜像名使用 `api-assets/<id>:<version>`。
`make verify` 必须同时验证领域测试、OpenAPI、生成可复现性和离线资产完整性。

## 语言边界

运行语言由资产自行选择，根目录不会提供跨语言业务框架。新资产只需遵守清单、
OpenAPI、OCI 和 Make 协议；Java、Go、Python、Node.js 可以各自使用原生构建工具。
同一个 API 不维护多语言服务端实现，也不预先创建空语言模板。

## 运行边界

服务可以校验前置平台注入的内部凭据，但不实现公网账号、套餐、计费或租户模型。
健康检查不得鉴权；指标是否鉴权必须在 OpenAPI 和资产清单中保持一致。数据或模型
损坏必须阻止就绪，不能静默返回不完整结果。
