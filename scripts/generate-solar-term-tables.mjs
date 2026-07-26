#!/usr/bin/env node

import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT_DIR = join(__dirname, '..');
const CSV_PATH = join(ROOT_DIR, 'tests', 'solar-terms.csv');
const TS_OUTPUT = join(ROOT_DIR, 'packages', 'ts-core', 'src', 'generated', 'solar-term-data.ts');
const JAVA_OUTPUT = join(
  ROOT_DIR,
  'java',
  'holiday-core-java',
  'src',
  'main',
  'java',
  'com',
  'github',
  'gmkits',
  'holiday',
  'core',
  'SolarTermTableData.java',
);

const START_YEAR = 1901;
const END_YEAR = 2100;
const EXPECTED_TERM_COUNT = 24;
const MONTH_OFFSETS = [0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334];
const LEAP_MONTH_OFFSETS = [0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335];

// HKO 原始数据为繁体，此映射表用于生成简体名称
const TRAD_TO_SIMP = {
  '驚蟄': '惊蛰',
  '穀雨': '谷雨',
  '小滿': '小满',
  '芒種': '芒种',
  '處暑': '处暑',
};

function toSimplified(name) {
  return TRAD_TO_SIMP[name] || name;
}

function isLeapYear(year) {
  return (year % 4 === 0 && year % 100 !== 0) || year % 400 === 0;
}

function dayOfYear(year, month, day) {
  const offsets = isLeapYear(year) ? LEAP_MONTH_OFFSETS : MONTH_OFFSETS;
  return offsets[month - 1] + day - 1;
}

function parseCsv() {
  const text = readFileSync(CSV_PATH, 'utf-8').trim();
  const lines = text.split(/\r?\n/);
  const header = lines.shift();

  if (header !== 'solarDate,solarTermIndex,solarTermName') {
    throw new Error('Unexpected CSV header: ' + header);
  }

  return lines.map((line, lineIndex) => {
    const parts = line.split(',');
    if (parts.length !== 3) {
      throw new Error('Invalid CSV row at line ' + (lineIndex + 2) + ': ' + line);
    }

    const solarDate = parts[0];
    const solarTermIndexText = parts[1];
    const solarTermName = parts[2];
    const dateParts = solarDate.split('-');
    const year = Number(dateParts[0]);
    const month = Number(dateParts[1]);
    const day = Number(dateParts[2]);
    const solarTermIndex = Number(solarTermIndexText);

    if (!Number.isInteger(year) || !Number.isInteger(month) || !Number.isInteger(day)) {
      throw new Error('Invalid solar date at line ' + (lineIndex + 2) + ': ' + solarDate);
    }
    if (!Number.isInteger(solarTermIndex)) {
      throw new Error('Invalid solarTermIndex at line ' + (lineIndex + 2) + ': ' + solarTermIndexText);
    }

    return {
      solarDate,
      year,
      month,
      day,
      solarTermIndex,
      solarTermName,
    };
  });
}

function validateAndTransform(rows) {
  const rowsByYear = new Map();

  for (const row of rows) {
    const yearRows = rowsByYear.get(row.year) || [];
    yearRows.push(row);
    rowsByYear.set(row.year, yearRows);
  }

  const years = Array.from(rowsByYear.keys()).sort((left, right) => left - right);
  if (years.length !== END_YEAR - START_YEAR + 1 || years[0] !== START_YEAR || years[years.length - 1] !== END_YEAR) {
    throw new Error('Expected complete year coverage ' + START_YEAR + '-' + END_YEAR + ', got ' + years[0] + '-' + years[years.length - 1]);
  }

  const baselineRows = rowsByYear.get(START_YEAR) || [];
  if (baselineRows.length !== EXPECTED_TERM_COUNT) {
    throw new Error(String(START_YEAR) + ' should contain ' + EXPECTED_TERM_COUNT + ' solar terms, got ' + baselineRows.length);
  }

  const solarTermNames = baselineRows.map((row) => row.solarTermName);
  const yearDayIndexes = [];

  for (let year = START_YEAR; year <= END_YEAR; year++) {
    const yearRows = rowsByYear.get(year) || [];
    if (yearRows.length !== EXPECTED_TERM_COUNT) {
      throw new Error(String(year) + ' should contain ' + EXPECTED_TERM_COUNT + ' solar terms, got ' + yearRows.length);
    }

    const dayIndexes = yearRows.map((row, rowIndex) => {
      if (row.solarTermIndex !== rowIndex) {
        throw new Error(String(year) + ' row ' + rowIndex + ' has unstable solarTermIndex ' + row.solarTermIndex);
      }
      if (row.solarTermName !== solarTermNames[rowIndex]) {
        throw new Error(String(year) + ' row ' + rowIndex + ' has mismatched solarTermName ' + row.solarTermName);
      }
      return dayOfYear(row.year, row.month, row.day);
    });

    yearDayIndexes.push(dayIndexes);
  }

  return { solarTermNames, yearDayIndexes };
}

function buildTsSource(solarTermNames, yearDayIndexes) {
  const simpNames = solarTermNames.map(toSimplified);
  const simpLines = simpNames.map((name) => "  '" + name + "',").join('\n');
  const tradLines = solarTermNames.map((name) => "  '" + name + "',").join('\n');
  const yearLines = yearDayIndexes.map((dayIndexes) => '  [' + dayIndexes.join(', ') + '],').join('\n');

  return [
    '/**',
    ' * Generated from tests/solar-terms.csv by scripts/generate-solar-term-tables.mjs.',
    ' * Do not edit manually.',
    ' */',
    'export const SOLAR_TERM_START_YEAR = ' + START_YEAR + ';',
    'export const SOLAR_TERM_END_YEAR = ' + END_YEAR + ';',
    '',
    '/** 简体中文节气名（默认）。 */',
    'export const SOLAR_TERM_NAMES: readonly string[] = [',
    simpLines,
    '];',
    '',
    '/** 繁体中文节气名。 */',
    'export const SOLAR_TERM_NAMES_ZH_TW: readonly string[] = [',
    tradLines,
    '];',
    '',
    'export const SOLAR_TERM_DAY_INDEXES_BY_YEAR: ReadonlyArray<ReadonlyArray<number>> = [',
    yearLines,
    '];',
    '',
  ].join('\n');
}

function escapeJavaString(value) {
  return value.replace(/\\/g, '\\\\').replace(/\"/g, '\\\"');
}

function buildJavaSource(solarTermNames, yearDayIndexes) {
  const simpNames = solarTermNames.map(toSimplified);
  const simpLines = simpNames.map((name) => '        \"' + escapeJavaString(name) + '\",').join('\n');
  const tradLines = solarTermNames.map((name) => '        \"' + escapeJavaString(name) + '\",').join('\n');
  const yearLines = yearDayIndexes.map((dayIndexes) => '        {' + dayIndexes.join(', ') + '},').join('\n');

  return [
    'package com.github.gmkits.holiday.core;',
    '',
    '/**',
    ' * Generated from tests/solar-terms.csv by scripts/generate-solar-term-tables.mjs.',
    ' * Do not edit manually.',
    ' */',
    'final class SolarTermTableData {',
    '',
    '    /** 简体中文节气名（默认）。 */',
    '    static final String[] SOLAR_TERM_NAMES = {',
    simpLines,
    '    };',
    '',
    '    /** 繁体中文节气名。 */',
    '    static final String[] SOLAR_TERM_NAMES_ZH_TW = {',
    tradLines,
    '    };',
    '',
    '    static final int[][] SOLAR_TERM_DAY_INDEXES_BY_YEAR = {',
    yearLines,
    '    };',
    '',
    '    private SolarTermTableData() {',
    '    }',
    '}',
    '',
  ].join('\n');
}

function writeFile(path, content) {
  mkdirSync(dirname(path), { recursive: true });
  writeFileSync(path, content, 'utf-8');
}

function main() {
  const rows = parseCsv();
  const result = validateAndTransform(rows);

  writeFile(TS_OUTPUT, buildTsSource(result.solarTermNames, result.yearDayIndexes));
  writeFile(JAVA_OUTPUT, buildJavaSource(result.solarTermNames, result.yearDayIndexes));

  process.stdout.write('Generated ' + TS_OUTPUT + '\n');
  process.stdout.write('Generated ' + JAVA_OUTPUT + '\n');
}

main();
