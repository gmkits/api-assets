#!/usr/bin/env node

/**
 * 同步并交叉核验中国大陆节假日安排。
 *
 * 运行时数据本身不依赖网络；本脚本仅在维护者更新年度数据时使用。
 * 2004 年以后以 chinese-days 的逐日表为基础，2007 年以后再与
 * holiday-cn 所引用的国务院通知逐日交叉核验。2000-2003 使用
 * holiday-calendar 的历史快照，并在元数据中明确标注来源等级。
 */

import { createHash } from 'node:crypto';
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { solarToLunar } from '../packages/ts-lunar/dist/esm/index.js';

const root = fileURLToPath(new URL('..', import.meta.url));
const outputDir = join(root, 'data/source/CN');
const outputCsv = join(outputDir, 'holidays.csv');
const outputSources = join(outputDir, 'sources.json');

const START_YEAR = 2000;
const END_YEAR = 2026;
const CHINESE_DAYS_VERSION = '1.5.9';
const HOLIDAY_CALENDAR_COMMIT = '6384dcbfc7da04d5a8beb0f093170aec02370689';
const HOLIDAY_CN_COMMIT = 'e110da903058bcd0d4010c4690ccdf56f7033b49';

const archivedPapers = {
  2000: [
    'https://www.gov.cn/gongbao/shuju/1999/gwyb199933.pdf',
  ],
  2001: [
    'https://zh.wikisource.org/wiki/国务院办公厅关于2001年春节、“五一”、“十一”放假安排的通知',
  ],
  2002: [
    'https://zh.wikisource.org/wiki/国务院办公厅关于2002年部分节假日休息安排的通知',
  ],
  2003: [
    'https://zh.wikisource.org/wiki/国务院办公厅关于2003年部分节假日休息安排的通知',
  ],
  2004: [
    'https://zh.wikisource.org/wiki/国务院办公厅关于2004年部分节假日安排的通知',
  ],
  2005: [
    'https://zh.wikisource.org/wiki/国务院办公厅关于2005年部分节假日安排的通知',
  ],
  2006: [
    'https://www.gov.cn/jrzg/2005-12/22/content_133837.htm',
  ],
};

const solarTerms = new Map(
  readFileSync(join(root, 'data/date-assets/calendar/solar-terms.csv'), 'utf8')
    .trim()
    .split(/\r?\n/)
    .slice(1)
    .map((line) => {
      const [date, , name] = line.split(',');
      return [date, name];
    }),
);

const rows = [];
const sourceYears = {};
const providerDigests = {
  chineseDays: [],
  holidayCalendar: [],
  holidayCn: [],
};

const baseByYear = new Map(await Promise.all(
  Array.from({ length: END_YEAR - START_YEAR + 1 }, (_, index) => START_YEAR + index)
    .map(async (year) => [
      year,
      year < 2004 ? await loadHolidayCalendar(year) : await loadChineseDays(year),
    ]),
));
const officialByYear = new Map(await Promise.all(
  Array.from({ length: END_YEAR - 2007 + 1 }, (_, index) => 2007 + index)
    .map(async (year) => [year, await loadHolidayCnCalendarYear(year)]),
));

for (let year = START_YEAR; year <= END_YEAR; year++) {
  const records = baseByYear.get(year);

  let papers = archivedPapers[year] ?? [];
  let confidence = year === 2000 ? 'RECONSTRUCTED' : 'ARCHIVED_NOTICE';

  if (year >= 2007) {
    const official = officialByYear.get(year);
    compareOverrides(year, records, official);
    papers = official.papers;
    confidence = 'GOV_NOTICE';
  }

  sourceYears[String(year)] = {
    confidence,
    papers: [...new Set(papers)],
  };

  for (const record of records) {
    rows.push({
      ...record,
      statutory: isStatutory(record.date, record.holiday),
      sourceYear: year,
      confidence,
    });
  }
}

rows.sort((a, b) => a.date.localeCompare(b.date) || a.status.localeCompare(b.status));
assertUniqueDates(rows);

mkdirSync(outputDir, { recursive: true });
writeFileSync(
  outputCsv,
  [
    'date,status,holiday,statutory,sourceYear,confidence',
    ...rows.map((row) => [
      row.date,
      row.status,
      row.holiday,
      row.statutory ? '1' : '0',
      row.sourceYear,
      row.confidence,
    ].join(',')),
    '',
  ].join('\n'),
);

const sources = {
  formatVersion: 1,
  region: 'CN',
  startYear: START_YEAR,
  endYear: END_YEAR,
  generatedAt: new Date().toISOString(),
  providers: {
    chineseDays: {
      version: CHINESE_DAYS_VERSION,
      license: 'MIT',
      url: 'https://github.com/vsme/chinese-days',
      sha256: aggregateDigest(providerDigests.chineseDays),
    },
    holidayCalendar: {
      commit: HOLIDAY_CALENDAR_COMMIT,
      license: 'MIT',
      url: 'https://github.com/cg-zhou/holiday-calendar',
      sha256: aggregateDigest(providerDigests.holidayCalendar),
    },
    holidayCn: {
      commit: HOLIDAY_CN_COMMIT,
      license: 'MIT',
      url: 'https://github.com/NateScarlet/holiday-cn',
      sha256: aggregateDigest(providerDigests.holidayCn),
    },
  },
  years: sourceYears,
};
writeFileSync(outputSources, `${JSON.stringify(sources, null, 2)}\n`);

console.log(`✓ ${rows.length} 条节假日/调休记录已同步：${START_YEAR}-${END_YEAR}`);
console.log(`✓ 通用 CSV：${outputCsv}`);

async function loadChineseDays(year) {
  const url = `https://cdn.jsdelivr.net/npm/chinese-days@${CHINESE_DAYS_VERSION}/dist/years/${year}.json`;
  const { json, bytes } = await fetchJson(url);
  providerDigests.chineseDays.push(digest(bytes));

  const result = [];
  for (const [date, name] of Object.entries(json.holidays)) {
    if (date.startsWith(`${year}-`)) {
      result.push({ date, status: 'OFF', holiday: holidayCode(name) });
    }
  }
  for (const [date, name] of Object.entries(json.workdays)) {
    if (date.startsWith(`${year}-`)) {
      result.push({ date, status: 'WORK', holiday: holidayCode(name) });
    }
  }
  return result;
}

async function loadHolidayCalendar(year) {
  const url = `https://cdn.jsdelivr.net/gh/cg-zhou/holiday-calendar@${HOLIDAY_CALENDAR_COMMIT}/data/CN/${year}.json`;
  const { json, bytes } = await fetchJson(url);
  providerDigests.holidayCalendar.push(digest(bytes));

  return json.dates
    .filter((entry) => entry.date.startsWith(`${year}-`))
    .map((entry) => ({
      date: entry.date,
      status: entry.type === 'transfer_workday' ? 'WORK' : 'OFF',
      holiday: holidayCode(entry.name_cn ?? entry.name),
    }));
}

async function loadHolidayCnCalendarYear(year) {
  const result = [];
  const papers = [];
  for (const noticeYear of [year, year + 1]) {
    const url = `https://cdn.jsdelivr.net/gh/NateScarlet/holiday-cn@${HOLIDAY_CN_COMMIT}/${noticeYear}.json`;
    const response = await fetchWithTimeout(url);
    if (response.status === 404) continue;
    if (!response.ok) throw new Error(`下载失败 ${url}: HTTP ${response.status}`);
    const bytes = new Uint8Array(await response.arrayBuffer());
    providerDigests.holidayCn.push(digest(bytes));
    const json = JSON.parse(new TextDecoder().decode(bytes));
    papers.push(...json.papers);
    for (const entry of json.days) {
      if (!entry.date.startsWith(`${year}-`)) continue;
      result.push({
        date: entry.date,
        status: entry.isOffDay ? 'OFF' : 'WORK',
        holiday: holidayCode(entry.name),
      });
    }
  }
  return { records: result, papers };
}

function compareOverrides(year, candidate, official) {
  const candidateMap = overrideMap(candidate);
  const officialMap = overrideMap(official.records);
  const dates = new Set([...candidateMap.keys(), ...officialMap.keys()]);
  const errors = [];
  for (const date of dates) {
    if (candidateMap.get(date) !== officialMap.get(date)) {
      errors.push(`${date}: ${candidateMap.get(date) ?? '-'} / ${officialMap.get(date) ?? '-'}`);
    }
  }
  if (errors.length > 0) {
    throw new Error(`${year} 与国务院通知数据不一致：\n${errors.join('\n')}`);
  }
}

function overrideMap(records) {
  const result = new Map();
  for (const record of records) {
    const weekend = isWeekend(record.date);
    if ((record.status === 'OFF' && !weekend) || (record.status === 'WORK' && weekend)) {
      result.set(record.date, record.status);
    }
  }
  return result;
}

function isStatutory(date, holiday) {
  const year = Number(date.slice(0, 4));
  const month = Number(date.slice(5, 7));
  const day = Number(date.slice(8, 10));
  if (holiday === 'NEW_YEAR') return month === 1 && day === 1;
  if (holiday === 'LABOUR_DAY') {
    return month === 5 && day >= 1 && day <= (year <= 2007 ? 3 : year >= 2025 ? 2 : 1);
  }
  if (holiday === 'NATIONAL_DAY') return month === 10 && day >= 1 && day <= 3;
  if (holiday === 'TOMB_SWEEPING') return solarTerms.get(date) === '清明';

  const lunar = solarToLunar(year, month, day);
  if (holiday === 'DRAGON_BOAT') return !lunar.isLeapMonth && lunar.month === 5 && lunar.day === 5;
  if (holiday === 'MID_AUTUMN') return !lunar.isLeapMonth && lunar.month === 8 && lunar.day === 15;
  if (holiday !== 'SPRING_FESTIVAL') return false;

  const isNewYear = !lunar.isLeapMonth && lunar.month === 1;
  const isEve = !lunar.isLeapMonth && lunar.month === 12
    && isLastLunarDay(year, month, day);
  if (year >= 2025) return isEve || (isNewYear && lunar.day <= 3);
  if (year >= 2008 && year <= 2013) return isEve || (isNewYear && lunar.day <= 2);
  return isNewYear && lunar.day <= 3;
}

function isLastLunarDay(year, month, day) {
  const tomorrow = new Date(Date.UTC(year, month - 1, day));
  tomorrow.setUTCDate(tomorrow.getUTCDate() + 1);
  const next = solarToLunar(
    tomorrow.getUTCFullYear(),
    tomorrow.getUTCMonth() + 1,
    tomorrow.getUTCDate(),
  );
  return !next.isLeapMonth && next.month === 1 && next.day === 1;
}

function holidayCode(name) {
  const value = String(name);
  if (value.includes('元旦')) return 'NEW_YEAR';
  if (value.includes('春节')) return 'SPRING_FESTIVAL';
  if (value.includes('清明')) return 'TOMB_SWEEPING';
  if (value.includes('劳动') || value.includes('五一') || value.includes('“五一”')) return 'LABOUR_DAY';
  if (value.includes('端午')) return 'DRAGON_BOAT';
  if (value.includes('中秋')) return 'MID_AUTUMN';
  if (value.includes('国庆') || value.includes('十一') || value.includes('“十一”')) return 'NATIONAL_DAY';
  if (value.includes('Anti-Fascist') || value.includes('反法西斯')) return 'VICTORY_DAY_70';
  throw new Error(`无法识别节日名称：${name}`);
}

function isWeekend(date) {
  const weekday = new Date(`${date}T00:00:00Z`).getUTCDay();
  return weekday === 0 || weekday === 6;
}

function assertUniqueDates(records) {
  const seen = new Map();
  for (const record of records) {
    const previous = seen.get(record.date);
    if (previous) {
      throw new Error(`日期重复：${record.date} (${previous.holiday}/${record.holiday})`);
    }
    seen.set(record.date, record);
  }
}

async function fetchJson(url) {
  const response = await fetchWithTimeout(url);
  if (!response.ok) throw new Error(`下载失败 ${url}: HTTP ${response.status}`);
  const bytes = new Uint8Array(await response.arrayBuffer());
  return {
    bytes,
    json: JSON.parse(new TextDecoder().decode(bytes)),
  };
}

async function fetchWithTimeout(url) {
  let lastError;
  for (let attempt = 1; attempt <= 3; attempt++) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 20_000);
    try {
      const response = await fetch(url, { signal: controller.signal });
      if (response.ok || response.status === 404) return response;
      lastError = new Error(`HTTP ${response.status}`);
    } catch (error) {
      lastError = error;
    } finally {
      clearTimeout(timeout);
    }
  }
  throw new Error(`下载失败 ${url}: ${lastError?.message ?? 'unknown error'}`);
}

function digest(bytes) {
  return createHash('sha256').update(bytes).digest('hex');
}

function aggregateDigest(digests) {
  return createHash('sha256').update([...digests].sort().join('\n')).digest('hex');
}
