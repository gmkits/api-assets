#!/usr/bin/env node

import { readFileSync, readdirSync } from 'node:fs';
import { basename, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('..', import.meta.url));
const rawDir = join(root, 'data/raw/CN');
const materializedDir = join(root, 'data/materialized/CN');
const errors = [];
const expectedStatutoryDates = new Map([
  [2025, new Set([
    '2025-01-01',
    '2025-01-28', '2025-01-29', '2025-01-30', '2025-01-31',
    '2025-04-04',
    '2025-05-01', '2025-05-02',
    '2025-05-31',
    '2025-10-01', '2025-10-02', '2025-10-03',
    '2025-10-06',
  ])],
  [2026, new Set([
    '2026-01-01',
    '2026-02-16', '2026-02-17', '2026-02-18', '2026-02-19',
    '2026-04-05',
    '2026-05-01', '2026-05-02',
    '2026-06-19',
    '2026-09-25',
    '2026-10-01', '2026-10-02', '2026-10-03',
  ])],
]);

for (const file of readdirSync(rawDir).filter((name) => name.endsWith('.source.json')).sort()) {
  const raw = JSON.parse(readFileSync(join(rawDir, file), 'utf8'));
  const materializedPath = join(materializedDir, `${raw.year}.year.json`);
  const materialized = JSON.parse(readFileSync(materializedPath, 'utf8'));
  const yearPrefix = `${raw.year}-`;
  const expectedHolidays = new Set(raw.holidays
    .flatMap((holiday) => holiday.holidayDates)
    .filter((date) => date.startsWith(yearPrefix)));
  const expectedAdjusted = new Set(raw.holidays
    .flatMap((holiday) => holiday.adjustedWorkdays)
    .filter((date) => date.startsWith(yearPrefix)));
  const actualHolidays = new Set();
  const actualAdjusted = new Set();
  const actualStatutory = new Set();

  for (const [date, day] of Object.entries(materialized.days)) {
    if (day.isHoliday) actualHolidays.add(date);
    if (day.isAdjustedWorkday) actualAdjusted.add(date);
    if (day.isStatutoryHoliday) actualStatutory.add(date);
  }

  compareSets(file, 'holiday', expectedHolidays, actualHolidays);
  compareSets(file, 'adjusted workday', expectedAdjusted, actualAdjusted);
  const expectedStatutory = expectedStatutoryDates.get(raw.year);
  if (expectedStatutory) {
    compareSets(file, 'statutory holiday', expectedStatutory, actualStatutory);
  }
}

if (errors.length > 0) {
  for (const error of errors) console.error(`✗ ${error}`);
  process.exit(1);
}

console.log('✓ Government sources match materialized holiday, adjusted-workday, and statutory dates');

function compareSets(file, label, expected, actual) {
  for (const date of expected) {
    if (!actual.has(date)) errors.push(`${basename(file)}: missing ${label} ${date}`);
  }
  for (const date of actual) {
    if (!expected.has(date)) errors.push(`${basename(file)}: unexpected ${label} ${date}`);
  }
}
