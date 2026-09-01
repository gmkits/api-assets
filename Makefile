API ?=
VERSION ?= 1.0.0-rc.2

.PHONY: verify build image smoke demo list require-api

list:
	@for manifest in apis/*/api-asset.json; do basename "$$(dirname "$$manifest")"; done

verify:
	pnpm run validate:assets
	pnpm run lint:openapi
	@if [ -n "$(API)" ]; then \
		$(MAKE) -C "apis/$(API)" verify; \
	else \
		for manifest in apis/*/api-asset.json; do \
			asset="$$(basename "$$(dirname "$$manifest")")"; \
			$(MAKE) -C "apis/$$asset" verify || exit $$?; \
		done; \
	fi

require-api:
	@test -n "$(API)" || (echo "API is required, for example: make build API=calendar" >&2; exit 2)

build: require-api
	$(MAKE) -C apis/$(API) build

image: require-api
	$(MAKE) -C apis/$(API) image VERSION=$(VERSION)

smoke: require-api
	$(MAKE) -C apis/$(API) smoke VERSION=$(VERSION)

demo: require-api
	$(MAKE) -C apis/$(API) image VERSION=$(VERSION)
	CALENDAR_VERSION=$(VERSION) docker compose up -d calendar
	trap 'if [ "$(KEEP_DEMO)" != "1" ]; then CALENDAR_VERSION=$(VERSION) docker compose down; fi' EXIT; \
	for i in $$(seq 1 60); do \
		if curl --fail --silent http://127.0.0.1:8080/internal/health/ready >/dev/null; then break; fi; \
		sleep 1; \
		if [ "$$i" = "60" ]; then docker compose logs calendar; exit 1; fi; \
	done; \
	CALENDAR_BASE_URL=$${CALENDAR_BASE_URL:-http://127.0.0.1:8080} CALENDAR_TOKEN=$${CALENDAR_TOKEN:-$${UPSTREAM_TOKEN:-}} \
		bash apis/$(API)/demo/curl.sh; \
	CALENDAR_BASE_URL=$${CALENDAR_BASE_URL:-http://127.0.0.1:8080} CALENDAR_TOKEN=$${CALENDAR_TOKEN:-$${UPSTREAM_TOKEN:-}} \
		node apis/$(API)/demo/client.mjs
