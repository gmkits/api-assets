import { writeFileSync, mkdirSync, existsSync, readFileSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));

export const CACHE_DIR = join(__dirname, '..', '..', '.hko-cache');
export const START_YEAR = 1901;
export const END_YEAR = 2100;

const CONCURRENCY = 10;
const MAX_RETRIES = 3;
const RETRY_DELAY_MS = 1000;
const DATE_RE = /^(\d{4})年(\d{1,2})月(\d{1,2})日/;
const WEEKDAY_RE = /^(星期[一二三四五六日天])/;

export function hkoUrl(year) {
  return `https://www.hko.gov.hk/tc/gts/time/calendar/text/files/T${year}c.txt`;
}

export function cachePath(year) {
  return join(CACHE_DIR, `T${year}c.txt`);
}

function toIsoDate(year, month, day) {
  return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
}

async function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function fetchWithRetry(year) {
  const cached = cachePath(year);
  if (existsSync(cached)) {
    return readFileSync(cached, 'utf-8');
  }

  let lastErr;
  for (let attempt = 1; attempt <= MAX_RETRIES; attempt++) {
    try {
      const res = await fetch(hkoUrl(year));
      if (!res.ok) throw new Error(`HTTP ${res.status} for year ${year}`);
      const buf = await res.arrayBuffer();
      let text;
      try {
        text = new TextDecoder('utf-8', { fatal: true }).decode(buf);
      } catch {
        text = new TextDecoder('big5').decode(buf);
      }
      mkdirSync(CACHE_DIR, { recursive: true });
      writeFileSync(cached, text, 'utf-8');
      return text;
    } catch (err) {
      lastErr = err;
      if (attempt < MAX_RETRIES) {
        process.stderr.write(`  ⚠ year ${year} attempt ${attempt} failed: ${err.message}, retrying…\n`);
        await sleep(RETRY_DELAY_MS * attempt);
      }
    }
  }

  throw new Error(`Failed to download year ${year} after ${MAX_RETRIES} retries: ${lastErr.message}`);
}

export async function downloadAll() {
  mkdirSync(CACHE_DIR, { recursive: true });

  const years = [];
  for (let year = START_YEAR; year <= END_YEAR; year++) {
    years.push(year);
  }

  const results = new Map();
  let done = 0;

  for (let index = 0; index < years.length; index += CONCURRENCY) {
    const batch = years.slice(index, index + CONCURRENCY);
    const texts = await Promise.all(batch.map((year) => fetchWithRetry(year)));
    batch.forEach((year, batchIndex) => results.set(year, texts[batchIndex]));
    done += batch.length;
    process.stderr.write(`\r  Downloaded ${done}/${years.length} files`);
  }

  process.stderr.write('\n');
  return results;
}

export function parseCalendarRow(line) {
  const dateMatch = line.match(DATE_RE);
  if (!dateMatch) return null;

  const weekdayIdx = line.indexOf('星期');
  if (weekdayIdx === -1) return null;

  const lunarRaw = line.slice(dateMatch[0].length, weekdayIdx).trim();
  if (!lunarRaw) return null;

  const weekdayMatch = line.slice(weekdayIdx).match(WEEKDAY_RE);
  if (!weekdayMatch) return null;

  const solarYear = parseInt(dateMatch[1], 10);
  const solarMonth = parseInt(dateMatch[2], 10);
  const solarDay = parseInt(dateMatch[3], 10);
  const weekday = weekdayMatch[1];
  const solarTermName = line.slice(weekdayIdx + weekday.length).trim() || null;

  return {
    solarYear,
    solarMonth,
    solarDay,
    solarDate: toIsoDate(solarYear, solarMonth, solarDay),
    lunarRaw,
    weekday,
    solarTermName,
  };
}

export function* iterateCalendarRows(text) {
  for (const line of text.split(/\r?\n/)) {
    const row = parseCalendarRow(line);
    if (row) {
      yield row;
    }
  }
}
