API ?=
VERSION ?= 1.0.0-rc.1

.PHONY: verify build image smoke list require-api

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
