#!/usr/bin/env node
/**
 * 从香港天文台 (HKO) 下载公历-农历对照表，生成 golden test CSV。
 *
 * 数据来源: https://www.hko.gov.hk/tc/gts/time/calendar/text/files/T{YEAR}c.txt
 * 年份范围: 1901–2100 (共 200 个文件)
 *
 * Usage:
 *   node scripts/generate-hko-golden.mjs                         # CSV → stdout
 *   node scripts/generate-hko-golden.mjs --output path/to/out.csv  # CSV → file
 */

import { writeFileSync, mkdirSync, existsSync, readFileSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { parseArgs } from 'node:util';

const __dirname = dirname(fileURLToPath(import.meta.url));
const CACHE_DIR = join(__dirname, '..', '.hko-cache');

const START_YEAR = 1901;
const END_YEAR = 2100;
const CONCURRENCY = 10;
const MAX_RETRIES = 3;
const RETRY_DELAY_MS = 1000;

// ---------------------------------------------------------------------------
// CLI
// ---------------------------------------------------------------------------
const { values: cliArgs } = parseArgs({
  options: { output: { type: 'string', short: 'o' } },
  strict: false,
});

// ---------------------------------------------------------------------------
// Download helpers
// ---------------------------------------------------------------------------
function hkoUrl(year) {
  return `https://www.hko.gov.hk/tc/gts/time/calendar/text/files/T${year}c.txt`;
}

function cachePath(year) {
  return join(CACHE_DIR, `T${year}c.txt`);
}

async function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms));
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
      // Files are Big5 or UTF-8; try UTF-8 first, fall back to Big5
      let text;
      try {
        text = new TextDecoder('utf-8', { fatal: true }).decode(buf);
      } catch {
        text = new TextDecoder('big5').decode(buf);
      }
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

async function downloadAll() {
  mkdirSync(CACHE_DIR, { recursive: true });
  const years = [];
  for (let y = START_YEAR; y <= END_YEAR; y++) years.push(y);

  const results = new Map();
  let done = 0;

  for (let i = 0; i < years.length; i += CONCURRENCY) {
    const batch = years.slice(i, i + CONCURRENCY);
    const texts = await Promise.all(batch.map((y) => fetchWithRetry(y)));
    batch.forEach((y, idx) => results.set(y, texts[idx]));
    done += batch.length;
    process.stderr.write(`\r  Downloaded ${done}/${years.length} files`);
  }
  process.stderr.write('\n');
  return results;
}

// ---------------------------------------------------------------------------
// Lunar date parsing
// ---------------------------------------------------------------------------

const MONTH_NAMES = new Map([
  ['正月', 1],
  ['二月', 2],
  ['三月', 3],
  ['四月', 4],
  ['五月', 5],
  ['六月', 6],
  ['七月', 7],
  ['八月', 8],
  ['九月', 9],
  ['十月', 10],
  ['十一月', 11],
  ['十二月', 12],
]);

const DAY_NAMES = new Map([
  ['初一', 1],  ['初二', 2],  ['初三', 3],  ['初四', 4],  ['初五', 5],
  ['初六', 6],  ['初七', 7],  ['初八', 8],  ['初九', 9],  ['初十', 10],
  ['十一', 11], ['十二', 12], ['十三', 13], ['十四', 14], ['十五', 15],
  ['十六', 16], ['十七', 17], ['十八', 18], ['十九', 19], ['二十', 20],
  ['廿一', 21], ['廿二', 22], ['廿三', 23], ['廿四', 24], ['廿五', 25],
  ['廿六', 26], ['廿七', 27], ['廿八', 28], ['廿九', 29], ['三十', 30],
]);

/**
 * Parse the lunar column value.
 * Returns { month, day, isLeapMonth, isNewMonth }.
 *  - isNewMonth: true when the column is a month name (this row is 初一 of that month)
 */
function parseLunarColumn(raw) {
  const s = raw.trim();

  // Leap month: "閏X月"
  const leapMatch = s.match(/^閏(.+)月$/);
  if (leapMatch) {
    const inner = leapMatch[1] + '月';
    const m = MONTH_NAMES.get(inner);
    if (m == null) throw new Error(`Unknown leap month: "${s}"`);
    return { month: m, day: 1, isLeapMonth: true, isNewMonth: true };
  }

  // Regular month name ending with 月
  if (s.endsWith('月')) {
    const m = MONTH_NAMES.get(s);
    if (m == null) throw new Error(`Unknown month: "${s}"`);
    return { month: m, day: 1, isLeapMonth: false, isNewMonth: true };
  }

  // Day name
  const d = DAY_NAMES.get(s);
  if (d == null) throw new Error(`Unknown day: "${s}"`);
  return { month: null, day: d, isLeapMonth: false, isNewMonth: false };
}

// Regex that matches both old format (2010年01月01日) and new format (2025年1月1日)
const DATE_RE = /^(\d{4})年(\d{1,2})月(\d{1,2})日/;

/**
 * Infer the starting month for the FIRST file (1901) by pre-scanning.
 */
function inferInitialMonth(text) {
  const lines = text.split(/\r?\n/);
  for (const line of lines) {
    const dm = line.match(DATE_RE);
    if (!dm) continue;
    const xqi = line.indexOf('星期');
    if (xqi === -1) continue;
    const raw = line.slice(dm.index + dm[0].length, xqi).trim();
    if (!raw) continue;
    const p = parseLunarColumn(raw);
    if (p.isNewMonth) {
      if (p.isLeapMonth) {
        return { month: p.month, isLeapMonth: false };
      } else if (p.month === 1) {
        return { month: 12, isLeapMonth: false };
      } else {
        return { month: p.month - 1, isLeapMonth: false };
      }
    }
  }
  throw new Error('Could not determine starting month');
}

/**
 * Parse a single year file.
 * `prevState` carries over from the previous file for correct cross-year tracking.
 * Returns { rows, endState } where endState can seed the next file.
 */
function parseFile(year, text, prevState) {
  const lines = text.split(/\r?\n/);
  const rows = [];

  let lunarYear = prevState?.lunarYear ?? year - 1;
  let currentMonth = prevState?.month ?? null;
  let isLeapMonth = prevState?.isLeapMonth ?? false;
  let seenZhengYue = prevState?.seenZhengYue ?? false;

  // For the very first file, infer from pre-scan if no prevState
  if (currentMonth == null) {
    const init = inferInitialMonth(text);
    currentMonth = init.month;
    isLeapMonth = init.isLeapMonth;
  }

  for (const line of lines) {
    const dateMatch = line.match(DATE_RE);
    if (!dateMatch) continue;

    const solarYear = parseInt(dateMatch[1], 10);
    const solarMonth = parseInt(dateMatch[2], 10);
    const solarDay = parseInt(dateMatch[3], 10);

    const xingqiIdx = line.indexOf('星期');
    if (xingqiIdx === -1) continue;
    const lunarRaw = line.slice(dateMatch.index + dateMatch[0].length, xingqiIdx).trim();
    if (!lunarRaw) continue;

    const parsed = parseLunarColumn(lunarRaw);

    if (parsed.isNewMonth) {
      currentMonth = parsed.month;
      isLeapMonth = parsed.isLeapMonth;

      if (parsed.month === 1 && !parsed.isLeapMonth && !seenZhengYue) {
        lunarYear = year;
        seenZhengYue = true;
      }
    }

    const lunarDay = parsed.day;
    const lunarMonth = parsed.isNewMonth ? parsed.month : currentMonth;
    const leap = parsed.isNewMonth ? parsed.isLeapMonth : isLeapMonth;

    rows.push({
      solarDate: `${solarYear}-${String(solarMonth).padStart(2, '0')}-${String(solarDay).padStart(2, '0')}`,
      lunarYear,
      lunarMonth,
      lunarDay,
      isLeapMonth: leap ? 1 : 0,
    });
  }

  const endState = { lunarYear, month: currentMonth, isLeapMonth, seenZhengYue: false };
  return { rows, endState };
}

// ---------------------------------------------------------------------------
// Validation
// ---------------------------------------------------------------------------
function validate(allRows) {
  const errors = [];
  let leapMonthCount = 0;

  // Group by (lunarYear, lunarMonth, isLeapMonth)
  const monthGroups = new Map();
  for (const r of allRows) {
    const key = `${r.lunarYear}-${r.lunarMonth}-${r.isLeapMonth}`;
    if (!monthGroups.has(key)) monthGroups.set(key, []);
    monthGroups.get(key).push(r);
  }

  // Identify first and last month keys (boundary months with partial data)
  const keys = [...monthGroups.keys()];
  const firstKey = keys[0];
  const lastKey = keys[keys.length - 1];

  for (const [key, rows] of monthGroups) {
    const days = rows.map((r) => r.lunarDay);
    const maxDay = Math.max(...days);
    const minDay = Math.min(...days);
    const isBoundary = key === firstKey || key === lastKey;

    if (!isBoundary) {
      if (maxDay !== 29 && maxDay !== 30) {
        errors.push(`Month ${key}: has ${maxDay} days (expected 29 or 30)`);
      }
      if (minDay !== 1) {
        errors.push(`Month ${key}: starts at day ${minDay} (expected 1)`);
      }
    }

    // Check sequential
    for (let i = 1; i < days.length; i++) {
      if (days[i] !== days[i - 1] + 1) {
        errors.push(`Month ${key}: non-sequential days at position ${i}: ${days[i - 1]} → ${days[i]}`);
      }
    }

    if (rows[0].isLeapMonth === 1) leapMonthCount++;
  }

  return { errors, leapMonthCount, totalMonths: monthGroups.size };
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------
async function main() {
  process.stderr.write(`Downloading HKO data for ${START_YEAR}–${END_YEAR}…\n`);
  const fileTexts = await downloadAll();

  process.stderr.write('Parsing…\n');
  let allRows = [];
  let prevState = null;
  for (let y = START_YEAR; y <= END_YEAR; y++) {
    const text = fileTexts.get(y);
    const { rows, endState } = parseFile(y, text, prevState);
    allRows.push(...rows);
    prevState = endState;
  }

  // Deduplicate: consecutive year files may overlap at boundaries—shouldn't happen
  // since each file covers exactly Jan 1 – Dec 31, but just in case
  const seen = new Set();
  const uniqueRows = [];
  for (const r of allRows) {
    if (!seen.has(r.solarDate)) {
      seen.add(r.solarDate);
      uniqueRows.push(r);
    }
  }
  allRows = uniqueRows;

  process.stderr.write(`Validating ${allRows.length} rows…\n`);
  const { errors, leapMonthCount, totalMonths } = validate(allRows);

  if (errors.length > 0) {
    process.stderr.write(`\n⚠ Validation issues (${errors.length}):\n`);
    for (const e of errors.slice(0, 20)) process.stderr.write(`  - ${e}\n`);
    if (errors.length > 20) process.stderr.write(`  … and ${errors.length - 20} more\n`);
  }

  // Build CSV
  const header = 'solarDate,lunarYear,lunarMonth,lunarDay,isLeapMonth\n';
  const body = allRows.map((r) => `${r.solarDate},${r.lunarYear},${r.lunarMonth},${r.lunarDay},${r.isLeapMonth}`).join('\n') + '\n';
  const csv = header + body;

  if (cliArgs.output) {
    mkdirSync(dirname(cliArgs.output), { recursive: true });
    writeFileSync(cliArgs.output, csv, 'utf-8');
    process.stderr.write(`\nWritten to ${cliArgs.output}\n`);
  } else {
    process.stdout.write(csv);
  }

  // Summary
  const firstDate = allRows[0]?.solarDate;
  const lastDate = allRows[allRows.length - 1]?.solarDate;
  process.stderr.write(`\n=== Summary ===\n`);
  process.stderr.write(`Total rows:    ${allRows.length}\n`);
  process.stderr.write(`Date range:    ${firstDate} → ${lastDate}\n`);
  process.stderr.write(`Total months:  ${totalMonths}\n`);
  process.stderr.write(`Leap months:   ${leapMonthCount}\n`);
  process.stderr.write(`Validation:    ${errors.length === 0 ? '✓ PASS' : `✗ ${errors.length} issues`}\n`);

  if (errors.length > 0) process.exit(1);
}

main().catch((err) => {
  process.stderr.write(`\nFATAL: ${err.message}\n${err.stack}\n`);
  process.exit(1);
});
