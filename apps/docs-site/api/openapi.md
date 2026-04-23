# OpenAPI

`holiday-api-j25` 已经集成 `springdoc-openapi`，启动后可访问：

- Swagger UI：[`/swagger-ui.html`](http://localhost:8080/swagger-ui.html)
- OpenAPI JSON：[`/v3/api-docs`](http://localhost:8080/v3/api-docs)
- 分组 v1：`/v3/api-docs/v1`
- 分组 v2：`/v3/api-docs/v2`

## 自定义 OpenAPI 信息

通过 `application.yml` 配置：

```yaml
holiday:
  api:
    api-version: 2.0.0
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
```

## 与 SDK 同步

`@RegionCode` 元注解的 `@Pattern` 会自动出现在 OpenAPI schema 的 `pattern` 字段，
SDK 与文档站只需引用同一份 OpenAPI 即可保持口径一致。
