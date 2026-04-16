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

import { writeFileSync, mkdirSync } from 'node:fs';
import { dirname } from 'node:path';
import { parseArgs } from 'node:util';

import { START_YEAR, END_YEAR, downloadAll, iterateCalendarRows } from './lib/hko-calendar.mjs';

const { values: cliArgs } = parseArgs({
  options: { output: { type: 'string', short: 'o' } },
  strict: false,
});

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

function parseLunarColumn(raw) {
  const s = raw.trim();

  const leapMatch = s.match(/^閏(.+)月$/);
  if (leapMatch) {
    const inner = `${leapMatch[1]}月`;
    const month = MONTH_NAMES.get(inner);
    if (month == null) throw new Error(`Unknown leap month: "${s}"`);
    return { month, day: 1, isLeapMonth: true, isNewMonth: true };
  }

  if (s.endsWith('月')) {
    const month = MONTH_NAMES.get(s);
    if (month == null) throw new Error(`Unknown month: "${s}"`);
    return { month, day: 1, isLeapMonth: false, isNewMonth: true };
  }

  const day = DAY_NAMES.get(s);
  if (day == null) throw new Error(`Unknown day: "${s}"`);
  return { month: null, day, isLeapMonth: false, isNewMonth: false };
}

function inferInitialMonth(text) {
  for (const row of iterateCalendarRows(text)) {
    const parsed = parseLunarColumn(row.lunarRaw);
    if (!parsed.isNewMonth) continue;

    if (parsed.isLeapMonth) {
      return { month: parsed.month, isLeapMonth: false };
    }

    if (parsed.month === 1) {
      return { month: 12, isLeapMonth: false };
    }

    return { month: parsed.month - 1, isLeapMonth: false };
  }

  throw new Error('Could not determine starting month');
}

function parseFile(year, text, prevState) {
  const rows = [];
  let lunarYear = prevState?.lunarYear ?? year - 1;
  let currentMonth = prevState?.month ?? null;
  let isLeapMonth = prevState?.isLeapMonth ?? false;
  let seenZhengYue = prevState?.seenZhengYue ?? false;

  if (currentMonth == null) {
    const init = inferInitialMonth(text);
    currentMonth = init.month;
    isLeapMonth = init.isLeapMonth;
  }

  for (const row of iterateCalendarRows(text)) {
    const parsed = parseLunarColumn(row.lunarRaw);

    if (parsed.isNewMonth) {
      currentMonth = parsed.month;
      isLeapMonth = parsed.isLeapMonth;

      if (parsed.month === 1 && !parsed.isLeapMonth && !seenZhengYue) {
        lunarYear = year;
        seenZhengYue = true;
      }
    }

    rows.push({
      solarDate: row.solarDate,
      lunarYear,
      lunarMonth: parsed.isNewMonth ? parsed.month : currentMonth,
      lunarDay: parsed.day,
      isLeapMonth: (parsed.isNewMonth ? parsed.isLeapMonth : isLeapMonth) ? 1 : 0,
    });
  }

  return {
    rows,
    endState: { lunarYear, month: currentMonth, isLeapMonth, seenZhengYue: false },
  };
}

function validate(allRows) {
  const errors = [];
  let leapMonthCount = 0;

  const monthGroups = new Map();
  for (const row of allRows) {
    const key = `${row.lunarYear}-${row.lunarMonth}-${row.isLeapMonth}`;
    if (!monthGroups.has(key)) monthGroups.set(key, []);
    monthGroups.get(key).push(row);
  }

  const keys = [...monthGroups.keys()];
  const firstKey = keys[0];
  const lastKey = keys[keys.length - 1];

  for (const [key, rows] of monthGroups) {
    const days = rows.map((row) => row.lunarDay);
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

    for (let index = 1; index < days.length; index++) {
      if (days[index] !== days[index - 1] + 1) {
        errors.push(`Month ${key}: non-sequential days at position ${index}: ${days[index - 1]} → ${days[index]}`);
      }
    }

    if (rows[0].isLeapMonth === 1) leapMonthCount++;
  }

  return { errors, leapMonthCount, totalMonths: monthGroups.size };
}

async function main() {
  process.stderr.write(`Downloading HKO data for ${START_YEAR}–${END_YEAR}…\n`);
  const fileTexts = await downloadAll();

  process.stderr.write('Parsing…\n');
  let allRows = [];
  let prevState = null;

  for (let year = START_YEAR; year <= END_YEAR; year++) {
    const text = fileTexts.get(year);
    const { rows, endState } = parseFile(year, text, prevState);
    allRows.push(...rows);
    prevState = endState;
  }

  const seen = new Set();
  const uniqueRows = [];
  for (const row of allRows) {
    if (seen.has(row.solarDate)) continue;
    seen.add(row.solarDate);
    uniqueRows.push(row);
  }
  allRows = uniqueRows;

  process.stderr.write(`Validating ${allRows.length} rows…\n`);
  const { errors, leapMonthCount, totalMonths } = validate(allRows);

  if (errors.length > 0) {
    process.stderr.write(`\n⚠ Validation issues (${errors.length}):\n`);
    for (const error of errors.slice(0, 20)) {
      process.stderr.write(`  - ${error}\n`);
    }
    if (errors.length > 20) {
      process.stderr.write(`  … and ${errors.length - 20} more\n`);
    }
  }

  const header = 'solarDate,lunarYear,lunarMonth,lunarDay,isLeapMonth\n';
  const body = allRows
    .map((row) => `${row.solarDate},${row.lunarYear},${row.lunarMonth},${row.lunarDay},${row.isLeapMonth}`)
    .join('\n');
  const csv = `${header}${body}\n`;

  if (cliArgs.output) {
    mkdirSync(dirname(cliArgs.output), { recursive: true });
    writeFileSync(cliArgs.output, csv, 'utf-8');
    process.stderr.write(`\nWritten to ${cliArgs.output}\n`);
  } else {
    process.stdout.write(csv);
  }

  const firstDate = allRows[0]?.solarDate;
  const lastDate = allRows[allRows.length - 1]?.solarDate;
  process.stderr.write('\n=== Summary ===\n');
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
