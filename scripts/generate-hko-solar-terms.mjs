#!/usr/bin/env node
/**
 * 从香港天文台 (HKO) 下载公历-农历对照表，生成节气 CSV 基线。
 *
 * 数据来源: https://www.hko.gov.hk/tc/gts/time/calendar/text/files/T{YEAR}c.txt
 * 年份范围: 1901–2100 (共 200 个文件)
 *
 * Usage:
 *   node scripts/generate-hko-solar-terms.mjs
 *   node scripts/generate-hko-solar-terms.mjs --output path/to/out.csv
 *   node scripts/generate-hko-solar-terms.mjs --java-output path/to/java.csv
 */

import { writeFileSync, mkdirSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { parseArgs } from 'node:util';

import { START_YEAR, END_YEAR, downloadAll, iterateCalendarRows } from './lib/hko-calendar.mjs';

const __dirname = dirname(fileURLToPath(import.meta.url));
const DEFAULT_OUTPUT = join(__dirname, '..', 'tests', 'solar-terms.csv');
const DEFAULT_JAVA_OUTPUT = join(__dirname, '..', 'java', 'holiday-core-java', 'src', 'test', 'resources', 'solar-terms.csv');
const EXPECTED_TERMS_PER_YEAR = 24;

const { values: cliArgs } = parseArgs({
  options: {
    output: { type: 'string', short: 'o' },
    'java-output': { type: 'string' },
  },
  strict: false,
});

function collectSolarTerms(fileTexts) {
  const rowsByYear = new Map();

  for (let year = START_YEAR; year <= END_YEAR; year++) {
    const yearRows = [];
    for (const row of iterateCalendarRows(fileTexts.get(year))) {
      if (!row.solarTermName) continue;
      yearRows.push({
        solarDate: row.solarDate,
        solarTermName: row.solarTermName,
      });
    }
    rowsByYear.set(year, yearRows);
  }

  return rowsByYear;
}

function validate(rowsByYear) {
  const errors = [];
  const baselineYear = START_YEAR;
  const baselineRows = rowsByYear.get(baselineYear) ?? [];
  const baselineNames = baselineRows.map((row) => row.solarTermName);
  const baselineKey = baselineNames.join('|');
  const baselineIndexByName = new Map(baselineNames.map((name, index) => [name, index]));
  const flattenedRows = [];
  const countsByYear = [];

  if (baselineNames.length !== EXPECTED_TERMS_PER_YEAR) {
    errors.push(`${baselineYear}: expected ${EXPECTED_TERMS_PER_YEAR} solar terms, got ${baselineNames.length}`);
  }

  if (new Set(baselineNames).size !== baselineNames.length) {
    errors.push(`${baselineYear}: solar term sequence contains duplicates`);
  }

  let previousGlobalDate = null;

  for (let year = START_YEAR; year <= END_YEAR; year++) {
    const yearRows = rowsByYear.get(year) ?? [];
    countsByYear.push({ year, count: yearRows.length });

    if (yearRows.length !== EXPECTED_TERMS_PER_YEAR) {
      errors.push(`${year}: expected ${EXPECTED_TERMS_PER_YEAR} solar terms, got ${yearRows.length}`);
    }

    const yearNames = yearRows.map((row) => row.solarTermName);
    if (baselineKey && yearNames.join('|') !== baselineKey) {
      errors.push(`${year}: solar term sequence mismatch`);
    }

    for (let index = 0; index < yearRows.length; index++) {
      const row = yearRows[index];
      const expectedIndex = baselineIndexByName.get(row.solarTermName);

      if (expectedIndex == null) {
        errors.push(`${year}: unknown solar term "${row.solarTermName}"`);
      }

      if (!row.solarDate.startsWith(`${year}-`)) {
        errors.push(`${year}: solar date ${row.solarDate} is outside the calendar year`);
      }

      if (index > 0 && row.solarDate <= yearRows[index - 1].solarDate) {
        errors.push(`${year}: solar dates are not strictly increasing`);
      }

      if (previousGlobalDate && row.solarDate <= previousGlobalDate) {
        errors.push(`Global order error: ${row.solarDate} is not after ${previousGlobalDate}`);
      }
      previousGlobalDate = row.solarDate;

      flattenedRows.push({
        solarDate: row.solarDate,
        solarTermIndex: expectedIndex ?? -1,
        solarTermName: row.solarTermName,
      });
    }
  }

  return { errors, baselineNames, countsByYear, flattenedRows };
}

function buildCsv(rows) {
  const header = 'solarDate,solarTermIndex,solarTermName\n';
  const body = rows.map((row) => `${row.solarDate},${row.solarTermIndex},${row.solarTermName}`).join('\n');
  return `${header}${body}\n`;
}

function writeOutputs(csv) {
  const outputPaths = new Set([
    cliArgs.output ?? DEFAULT_OUTPUT,
    cliArgs['java-output'] ?? DEFAULT_JAVA_OUTPUT,
  ]);

  for (const outputPath of outputPaths) {
    mkdirSync(dirname(outputPath), { recursive: true });
    writeFileSync(outputPath, csv, 'utf-8');
    process.stderr.write(`Written to ${outputPath}\n`);
  }
}

async function main() {
  process.stderr.write(`Downloading HKO data for ${START_YEAR}–${END_YEAR}…\n`);
  const fileTexts = await downloadAll();

  process.stderr.write('Parsing solar terms…\n');
  const rowsByYear = collectSolarTerms(fileTexts);
  const { errors, baselineNames, countsByYear, flattenedRows } = validate(rowsByYear);

  if (errors.length > 0) {
    process.stderr.write(`\n⚠ Validation issues (${errors.length}):\n`);
    for (const error of errors.slice(0, 20)) {
      process.stderr.write(`  - ${error}\n`);
    }
    if (errors.length > 20) {
      process.stderr.write(`  … and ${errors.length - 20} more\n`);
    }
    process.exit(1);
  }

  const csv = buildCsv(flattenedRows);
  writeOutputs(csv);

  const lineCount = flattenedRows.length + 1;
  const firstDate = flattenedRows[0]?.solarDate;
  const lastDate = flattenedRows[flattenedRows.length - 1]?.solarDate;
  const invalidYears = countsByYear.filter((item) => item.count !== EXPECTED_TERMS_PER_YEAR).length;

  process.stderr.write('\n=== Summary ===\n');
  process.stderr.write(`Total rows:          ${flattenedRows.length}\n`);
  process.stderr.write(`CSV line count:      ${lineCount}\n`);
  process.stderr.write(`Date range:          ${firstDate} → ${lastDate}\n`);
  process.stderr.write(`Sequence:            ${baselineNames.join(' → ')}\n`);
  process.stderr.write(`Years with 24 terms: ${countsByYear.length - invalidYears}/${countsByYear.length}\n`);
  process.stderr.write('Validation:          ✓ PASS\n');
}

main().catch((err) => {
  process.stderr.write(`\nFATAL: ${err.message}\n${err.stack}\n`);
  process.exit(1);
});
