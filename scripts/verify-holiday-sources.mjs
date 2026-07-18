#!/usr/bin/env node

import { readFileSync, readdirSync } from 'node:fs';
import { basename, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('..', import.meta.url));
const rawDir = join(root, 'data/raw/CN');
const materializedDir = join(root, 'data/materialized/CN');
const errors = [];

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

  for (const [date, day] of Object.entries(materialized.days)) {
    if (day.isHoliday) actualHolidays.add(date);
    if (day.isAdjustedWorkday) actualAdjusted.add(date);
  }

  compareSets(file, 'holiday', expectedHolidays, actualHolidays);
  compareSets(file, 'adjusted workday', expectedAdjusted, actualAdjusted);
}

if (errors.length > 0) {
  for (const error of errors) console.error(`✗ ${error}`);
  process.exit(1);
}

console.log('✓ Raw government notices match all materialized holiday and adjusted-workday dates');

function compareSets(file, label, expected, actual) {
  for (const date of expected) {
    if (!actual.has(date)) errors.push(`${basename(file)}: missing ${label} ${date}`);
  }
  for (const date of actual) {
    if (!expected.has(date)) errors.push(`${basename(file)}: unexpected ${label} ${date}`);
  }
}
