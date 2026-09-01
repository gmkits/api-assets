#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${CALENDAR_BASE_URL:-http://127.0.0.1:8080}"
TOKEN="${CALENDAR_TOKEN:-}"

curl_auth() {
  if [[ -n "$TOKEN" ]]; then
    curl -H "Authorization: Bearer $TOKEN" "$@"
  else
    curl "$@"
  fi
}

request() {
  curl_auth --fail-with-body --silent --show-error "$@"
  printf '\n'
}

echo '== health =='
request "$BASE_URL/internal/health/live"
request "$BASE_URL/internal/health/ready"

echo '== calendar queries =='
request "$BASE_URL/v1/calendar/dates/2025-10-06?region=CN&locale=zh-CN"
request "$BASE_URL/v1/calendar/dates?from=2025-10-01&to=2025-10-08&fields=holidayNames,lunar,festivals"
request "$BASE_URL/v1/calendar/months/2025/10?locale=en-US"
request "$BASE_URL/v1/calendar/years/2026?fields=holidayNames,labels"
request "$BASE_URL/v1/calendar/workdays/count?from=2026-01-01&to=2026-12-31"
request "$BASE_URL/v1/calendar/holidays?year=2025&locale=en-US"
request "$BASE_URL/v1/calendar/holidays/next?from=2025-01-01"
request "$BASE_URL/v1/calendar/regions"

echo '== batch =='
curl_auth --fail-with-body --silent --show-error \
  -H 'Content-Type: application/json' \
  -d '{"locale":"en-US","fields":["holidayNames","festivals"],"ranges":[{"from":"2025-01-01","to":"2025-01-03"},{"from":"2025-01-03","to":"2025-01-05"}]}' \
  "$BASE_URL/v1/calendar/dates:batch"
printf '\n'

echo '== conversions and metadata =='
request "$BASE_URL/v1/calendar/lunar/from-solar?date=2025-01-29"
request "$BASE_URL/v1/calendar/solar/from-lunar?year=2025&month=1&day=1"
request "$BASE_URL/v1/calendar/solar-terms/2025"
request "$BASE_URL/v1/calendar/metadata"

echo '== binary ETag / 304 =='
headers="$(mktemp)"
trap 'rm -f "$headers"' EXIT
curl_auth --fail --silent --show-error -D "$headers" \
  "$BASE_URL/v1/calendar/assets/calendar.cdat" -o /tmp/calendar-demo.cdat
etag="$(awk 'tolower($1)=="etag:" {gsub("\r", "", $2); print $2; exit}' "$headers")"
test -n "$etag"
curl_auth --silent --show-error -o /dev/null -w 'conditional status: %{http_code}\n' \
  -H "If-None-Match: $etag" "$BASE_URL/v1/calendar/assets/calendar.cdat"
request "$BASE_URL/v1/calendar/assets/holidays/CN/2026.hday" >/dev/null

echo '== expected errors =='
curl_auth --silent --show-error -o /dev/null -w 'invalid date: %{http_code}\n' \
  "$BASE_URL/v1/calendar/dates/2026-02-30"
curl_auth --silent --show-error -o /dev/null -w 'reversed range: %{http_code}\n' \
  "$BASE_URL/v1/calendar/dates?from=2026-01-02&to=2026-01-01"
