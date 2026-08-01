ROOT := ../..
VERSION ?= 1.0.0-rc.1
IMAGE ?= api-assets/calendar:$(VERSION)
MAVEN := mvn -B -f runtime/java/pom.xml

.PHONY: verify build image smoke assets

verify: assets
	pnpm --dir $(ROOT) --filter @api-assets/calendar-compiler lint
	pnpm --dir $(ROOT) --filter @api-assets/calendar-compiler test
	$(MAVEN) clean verify

build:
	pnpm --dir $(ROOT) --filter @api-assets/calendar-compiler build
	$(MAVEN) -pl calendar-service -am package -DskipTests

assets:
	bash tooling/scripts/update-bundles.sh
	git -C $(ROOT) diff --exit-code -- \
		apis/calendar/assets/runtime/calendar \
		apis/calendar/assets/runtime/holidays \
		apis/calendar/assets/runtime/manifest.json

image:
	docker build --build-arg VERSION=$(VERSION) --build-arg SOURCE_COMMIT=$$(git -C $(ROOT) rev-parse HEAD) -t $(IMAGE) -f runtime/java/calendar-service/Dockerfile $(ROOT)

smoke:
	IMAGE=$(IMAGE) bash tooling/scripts/smoke.sh
