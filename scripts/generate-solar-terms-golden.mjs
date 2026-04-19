#!/usr/bin/env node
/**
 * 从 hungtcs/traditional-chinese-calendar-database（基于香港天文台 HKO 数据）
 * 下载 1901-2100 年二十四节气日期，生成：
 *   1. Golden CSV 文件 (tests/solar-terms-golden.csv)
 *   2. 可直接嵌入 TS/Java 的压缩数据表
 *
 * 数据来源: https://github.com/hungtcs/traditional-chinese-calendar-database
 * 原始数据来源: 香港天文台 (HKO) / 紫金山天文台天文年历
 *
 * Usage:
 *   node scripts/generate-solar-terms-golden.mjs
 *   node scripts/generate-solar-terms-golden.mjs --output tests/solar-terms-golden.csv
 */

import { writeFileSync, mkdirSync, existsSync, readFileSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { parseArgs } from 'node:util';

const __dirname = dirname(fileURLToPath(import.meta.url));
const CACHE_DIR = join(__dirname, '..', '.solar-terms-cache');

const START_YEAR = 1901;
const END_YEAR = 2100;
const CONCURRENCY = 10;
const MAX_RETRIES = 3;
const RETRY_DELAY_MS = 1000;

// ─── 繁体→简体映射 ───
const TRAD_TO_SIMP = {
  '驚蟄': '惊蛰',
  '穀雨': '谷雨',
  '小滿': '小满',
  '芒種': '芒种',
  '處暑': '处暑',
};

// ─── 24 节气按一年时间顺序排列 ───
const TERM_ORDER = [
  '小寒', '大寒', '立春', '雨水', '惊蛰', '春分',
  '清明', '谷雨', '立夏', '小满', '芒种', '夏至',
  '小暑', '大暑', '立秋', '处暑', '白露', '秋分',
  '寒露', '霜降', '立冬', '小雪', '大雪', '冬至',
];

// ─── CLI ───
const { values: cliArgs } = parseArgs({
  options: { output: { type: 'string', short: 'o' } },
  strict: false,
});

// ─── Download helpers ───

function githubUrl(year) {
  return `https://raw.githubusercontent.com/hungtcs/traditional-chinese-calendar-database/master/database/json/${year}.json`;
}

function cachePath(year) {
  return join(CACHE_DIR, `${year}.json`);
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
      const res = await fetch(githubUrl(year));
      if (!res.ok) throw new Error(`HTTP ${res.status} for year ${year}`);
      const text = await res.text();
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

// ─── Parse solar terms from year JSON ───

function parseSolarTerms(year, jsonText) {
  const data = JSON.parse(jsonText);
  const terms = [];

  for (const entry of data) {
    if (!entry.solarTerm) continue;
    let name = entry.solarTerm;
    // 繁体 → 简体
    if (TRAD_TO_SIMP[name]) name = TRAD_TO_SIMP[name];

    const g = entry.gregorian;
    terms.push({
      year: g.year,
      month: g.month,
      day: g.date,
      name,
    });
  }

  // Verify 24 terms
  if (terms.length !== 24) {
    throw new Error(`Year ${year}: expected 24 solar terms, got ${terms.length}`);
  }

  // Sort by date
  terms.sort((a, b) => {
    if (a.month !== b.month) return a.month - b.month;
    return a.day - b.day;
  });

  // Verify names match expected order
  for (let i = 0; i < 24; i++) {
    if (terms[i].name !== TERM_ORDER[i]) {
      throw new Error(
        `Year ${year}: term ${i} expected "${TERM_ORDER[i]}" but got "${terms[i].name}" (date: ${terms[i].month}-${terms[i].day})`,
      );
    }
  }

  return terms;
}

// ─── Generate compact data ───

/**
 * 每年 24 个节气的月份是固定的（每个节气总是落在特定月份），
 * 因此只需存储 day-of-month。
 *
 * 编码方式：3 个 40-bit 整数 per year（每 8 个 term 打包一个整数，
 * 每个 term 用 5 bit 存储 day，范围 1-31）。
 *
 * 但 JS 安全整数只有 53 bit，40 bit 完全安全。
 *
 * 实际方案：用 24 字符字符串表示一年的 24 天值。
 * 编码：day 1-9 → '1'-'9'，day 10-31 → 'a'-'v'
 */
function dayToChar(day) {
  if (day >= 1 && day <= 9) return String(day);
  if (day >= 10 && day <= 31) return String.fromCharCode(97 + day - 10); // 'a'=10, 'b'=11, ...
  throw new Error(`Invalid day: ${day}`);
}

function generateCompactData(allTermsByYear) {
  const lines = [];
  for (let y = START_YEAR; y <= END_YEAR; y++) {
    const terms = allTermsByYear.get(y);
    const encoded = terms.map((t) => dayToChar(t.day)).join('');
    lines.push(encoded);
  }
  return lines;
}

// ─── Main ───

async function main() {
  process.stderr.write(`Downloading solar terms data for ${START_YEAR}–${END_YEAR}…\n`);
  const fileTexts = await downloadAll();

  process.stderr.write('Parsing solar terms…\n');
  const allTermsByYear = new Map();
  const csvRows = [];

  for (let y = START_YEAR; y <= END_YEAR; y++) {
    const text = fileTexts.get(y);
    const terms = parseSolarTerms(y, text);
    allTermsByYear.set(y, terms);
    for (let i = 0; i < terms.length; i++) {
      const t = terms[i];
      csvRows.push({
        year: t.year,
        termIndex: i,
        termName: t.name,
        month: t.month,
        day: t.day,
      });
    }
  }

  // Generate CSV
  const header = 'year,termIndex,termName,month,day\n';
  const body = csvRows.map((r) => `${r.year},${r.termIndex},${r.termName},${r.month},${r.day}`).join('\n') + '\n';
  const csv = header + body;

  const defaultOutput = join(__dirname, '..', 'tests', 'solar-terms-golden.csv');
  const outputPath = cliArgs.output || defaultOutput;
  mkdirSync(dirname(outputPath), { recursive: true });
  writeFileSync(outputPath, csv, 'utf-8');
  process.stderr.write(`\n✓ CSV written to ${outputPath} (${csvRows.length} rows)\n`);

  // Generate compact data
  const compactLines = generateCompactData(allTermsByYear);

  // Output compact data as TS constant
  const tsDataPath = join(__dirname, '..', '.solar-terms-compact.txt');
  const tsContent = compactLines.map((line, i) => {
    const y = START_YEAR + i;
    return `  '${line}', // ${y}`;
  }).join('\n');
  writeFileSync(tsDataPath, `// SOLAR_TERM_DAYS: 24-char string per year (${START_YEAR}-${END_YEAR})\n// Each char encodes day-of-month: '1'-'9' = 1-9, 'a'-'v' = 10-31\nconst SOLAR_TERM_DAYS: string[] = [\n${tsContent}\n];\n`, 'utf-8');
  process.stderr.write(`✓ Compact data written to ${tsDataPath}\n`);

  // Summary
  process.stderr.write(`\n=== Summary ===\n`);
  process.stderr.write(`Years:      ${START_YEAR}–${END_YEAR} (${END_YEAR - START_YEAR + 1} years)\n`);
  process.stderr.write(`CSV rows:   ${csvRows.length}\n`);
  process.stderr.write(`Compact:    ${compactLines.length} × 24 chars = ${compactLines.reduce((s, l) => s + l.length, 0)} chars total\n`);
}

main().catch((err) => {
  process.stderr.write(`\nFATAL: ${err.message}\n${err.stack}\n`);
  process.exit(1);
});
