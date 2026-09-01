const base = (process.env.CALENDAR_BASE_URL || 'http://127.0.0.1:8080').replace(/\/$/, '');
const token = process.env.CALENDAR_TOKEN || '';
const headers = token ? { Authorization: `Bearer ${token}` } : {};

async function call(path, init = {}) {
  const response = await fetch(`${base}${path}`, {
    ...init,
    headers: { ...headers, ...(init.headers || {}) },
  });
  const type = response.headers.get('content-type') || '';
  let body;
  if (type.includes('json')) {
    body = await response.json();
  } else if (type.includes('octet-stream')) {
    body = { bytes: (await response.arrayBuffer()).byteLength,
      sha256: response.headers.get('x-checksum-sha256') };
  } else {
    body = await response.text();
  }
  if (!response.ok) throw new Error(`${response.status} ${path}: ${JSON.stringify(body)}`);
  console.log(`\n${init.method || 'GET'} ${path}`);
  console.log(JSON.stringify(body, null, 2));
  return { body, response };
}

await call('/internal/health/ready');
await call('/v1/calendar/dates/2025-10-06?region=CN');
await call('/v1/calendar/months/2025/10?locale=en-US&fields=holidayNames,lunar,festivals');
await call('/v1/calendar/years/2026?fields=holidayNames,labels');
await call('/v1/calendar/dates?from=2025-01-01&to=2025-01-07&fields=holidayNames,lunar');
await call('/v1/calendar/dates:batch', {
  method: 'POST',
  headers: { 'content-type': 'application/json' },
  body: JSON.stringify({
    locale: 'en-US',
    fields: ['holidayNames', 'festivals'],
    ranges: [
      { from: '2025-01-01', to: '2025-01-03' },
      { from: '2025-01-03', to: '2025-01-05' },
    ],
  }),
});
await call('/v1/calendar/workdays/count?from=2026-01-01&to=2026-12-31');
await call('/v1/calendar/holidays?year=2025&locale=en-US');
await call('/v1/calendar/holidays/next?from=2025-01-01&locale=en-US');
await call('/v1/calendar/lunar/from-solar?date=2025-01-29');
await call('/v1/calendar/solar/from-lunar?year=2025&month=1&day=1&leapMonth=false');
await call('/v1/calendar/solar-terms/2025');
await call('/v1/calendar/regions');
await call('/v1/calendar/metadata');
const asset = await call('/v1/calendar/assets/calendar.cdat');
const etag = asset.response.headers.get('etag');
if (!etag) throw new Error('calendar.cdat response did not include ETag');
const conditional = await fetch(`${base}/v1/calendar/assets/calendar.cdat`, {
  headers: { ...headers, 'if-none-match': etag },
});
if (conditional.status !== 304) throw new Error(`expected 304, got ${conditional.status}`);
console.log('\nconditional calendar.cdat: 304');
await call('/v1/calendar/assets/holidays/CN/2026.hday');
