#!/usr/bin/env node

/**
 * 验证通用 CSV、编译结果和来源元数据的一致性。
 */

import { readFileSync, readdirSync } from 'node:fs';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('../..', import.meta.url));
const source = join(root, 'assets/source/CN');
const generated = join(root, 'target/generated-data/materialized/CN');
const sources = JSON.parse(readFileSync(join(source, 'sources.json'), 'utf8'));
const errors = [];

const rows = readFileSync(join(source, 'holidays.csv'), 'utf8')
  .trim()
  .split(/\r?\n/)
  .slice(1)
  .map((line) => {
    const [date, status, holiday, statutory] = line.split(',');
    return { date, status, holiday, statutory: statutory === '1' };
  });

for (let year = sources.startYear; year <= sources.endYear; year++) {
  const file = join(generated, `${year}.year.json`);
  const materialized = JSON.parse(readFileSync(file, 'utf8'));
  const expected = rows.filter((row) => row.date.startsWith(`${year}-`));
  const expectedDates = new Set(expected.map((row) => row.date));

  for (const row of expected) {
    const actual = materialized.days[row.date];
    if (!actual) {
      errors.push(`${row.date}: 编译结果缺失`);
      continue;
    }
    if (row.status === 'OFF' && (!actual.isHoliday || actual.isWorkday)) {
      errors.push(`${row.date}: 应为休息日`);
    }
    if (row.status === 'WORK' && (!actual.isWorkday || !actual.isAdjustedWorkday)) {
      errors.push(`${row.date}: 应为调休工作日`);
    }
    if (actual.isStatutoryHoliday !== row.statutory) {
      errors.push(`${row.date}: 法定假日标记不一致`);
    }
    if (!actual.labels.includes(row.holiday)) {
      errors.push(`${row.date}: 缺少节日标签 ${row.holiday}`);
    }
  }

  for (const [date, day] of Object.entries(materialized.days)) {
    if ((day.isAdjustedWorkday || day.holidayNames['zh-CN']?.length) && !expectedDates.has(date)) {
      errors.push(`${date}: 出现 CSV 未声明的节假日安排`);
    }
  }
}

const bundleYears = readdirSync(join(root, 'assets/runtime/holidays/bundles/CN'))
  .filter((name) => name.endsWith('.hday'))
  .map((name) => Number(name.slice(0, -5)))
  .sort((a, b) => a - b);
const expectedYears = Array.from(
  { length: sources.endYear - sources.startYear + 1 },
  (_, index) => sources.startYear + index,
);
if (bundleYears.join(',') !== expectedYears.join(',')) {
  errors.push(`bundle 年份不连续：${bundleYears.join(',')}`);
}

if (errors.length > 0) {
  for (const error of errors) console.error(`✗ ${error}`);
  process.exit(1);
}

console.log(`✓ ${sources.startYear}-${sources.endYear} 节假日 CSV、来源和 bundle 完全一致`);
