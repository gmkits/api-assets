#!/bin/bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../../.." && pwd)"
IMAGE="${IMAGE:-api-assets/calendar:1.0.0-rc.2}"
PORT="${CALENDAR_SMOKE_PORT:-18082}"
NAME="calendar-api-smoke-$$"
TMP_DIR="$(mktemp -d)"

cleanup() {
  docker stop "$NAME" >/dev/null 2>&1 || true
  rm -rf "$TMP_DIR"
}
trap cleanup EXIT

start_container() {
  local token="${1:-}"
  local args=(docker run --rm -d --name "$NAME" --read-only --tmpfs /tmp:size=64m
    -p "127.0.0.1:${PORT}:8080")
  if [[ -n "$token" ]]; then
    args+=(-e "UPSTREAM_TOKEN=$token")
  fi
  "${args[@]}" "$IMAGE" >/dev/null

  local ready=false
  for _ in $(seq 1 60); do
    if curl --fail --silent "http://127.0.0.1:${PORT}/internal/health/ready" >/dev/null; then
      ready=true
      break
    fi
    sleep 1
  done
  [[ "$ready" == true ]] || { docker logs "$NAME"; return 1; }
}

expect_status() {
  local expected="$1"
  shift
  local actual
  actual="$(curl --silent --output "$TMP_DIR/response.json" --write-out '%{http_code}' "$@")"
  [[ "$actual" == "$expected" ]] || {
    echo "expected HTTP $expected, got $actual from $*" >&2
    cat "$TMP_DIR/response.json" >&2
    return 1
  }
}

start_container

curl --fail --silent "http://127.0.0.1:${PORT}/v1/calendar/dates/2025-10-06" \
  -o "$TMP_DIR/day.json"
grep -q '"date":"2025-10-06"' "$TMP_DIR/day.json"
grep -q '"isStatutoryHoliday":true' "$TMP_DIR/day.json"
grep -q '"isAdjustedWorkday":false' "$TMP_DIR/day.json"
grep -q '"month":8' "$TMP_DIR/day.json"
grep -q '"day":15' "$TMP_DIR/day.json"
curl --fail --silent "http://127.0.0.1:${PORT}/v1/calendar/years/2026" \
  -o "$TMP_DIR/year.json"
[[ "$(grep -o '"date":"2026-' "$TMP_DIR/year.json" | wc -l | tr -d ' ')" == 365 ]]
grep -q '"date":"2026-12-31"' "$TMP_DIR/year.json"

expect_status 404 "http://127.0.0.1:${PORT}/v1/calendar/dates?from=1999-12-31&to=2000-01-02"
grep -q '"code":"CALENDAR_DATA_NOT_AVAILABLE"' "$TMP_DIR/response.json"
expect_status 400 "http://127.0.0.1:${PORT}/v1/calendar/dates/2026-02-30"
expect_status 400 "http://127.0.0.1:${PORT}/v1/calendar/dates?from=2026-01-02&to=2026-01-01"

curl --fail --silent --dump-header "$TMP_DIR/calendar.headers" \
  "http://127.0.0.1:${PORT}/v1/calendar/assets/calendar.cdat" \
  -o "$TMP_DIR/calendar.cdat"
cmp "$TMP_DIR/calendar.cdat" "$ROOT/apis/calendar/assets/runtime/calendar/calendar.cdat"
etag="$(awk 'tolower($1) == "etag:" { print $2; exit }' \
  "$TMP_DIR/calendar.headers" | tr -d '\r')"
declared_sha="$(awk 'tolower($1) == "x-checksum-sha256:" { print $2; exit }' \
  "$TMP_DIR/calendar.headers" | tr -d '\r')"
actual_sha="$(shasum -a 256 "$TMP_DIR/calendar.cdat" | awk '{print $1}')"
[[ -n "$etag" && "$declared_sha" == "$actual_sha" ]]
expect_status 304 -H "If-None-Match: $etag" \
  "http://127.0.0.1:${PORT}/v1/calendar/assets/calendar.cdat"

# 开启内部令牌后，业务 API 和指标受保护，健康检查仍可直接调用。
docker stop "$NAME" >/dev/null
start_container smoke-secret
expect_status 401 "http://127.0.0.1:${PORT}/v1/calendar/metadata"
expect_status 401 -H "Authorization: Bearer wrong" \
  "http://127.0.0.1:${PORT}/v1/calendar/metadata"
expect_status 200 -H "Authorization: Bearer smoke-secret" \
  "http://127.0.0.1:${PORT}/v1/calendar/metadata"
expect_status 401 "http://127.0.0.1:${PORT}/internal/metrics"
expect_status 200 "http://127.0.0.1:${PORT}/internal/health/ready"
echo "✓ calendar image smoke passed: $IMAGE"
