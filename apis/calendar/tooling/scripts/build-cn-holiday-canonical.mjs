#!/usr/bin/env node

/**
 * 从唯一维护的稀疏 CSV 生成编译器 canonical 输入。
 *
 * 输出位于 target/generated-data，不进入版本控制；发布资产始终可以
 * 在无网络环境下由仓库内 CSV 确定性重建。
 */

import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('../..', import.meta.url));
const input = join(root, 'assets/source/CN/holidays.csv');
const sources = JSON.parse(readFileSync(join(root, 'assets/source/CN/sources.json'), 'utf8'));
const outputDir = join(root, 'target/generated-data/canonical/CN');

const rows = readFileSync(input, 'utf8')
  .trim()
  .split(/\r?\n/)
  .slice(1)
  .map((line) => {
    const [date, status, holiday, statutory, sourceYear, confidence] = line.split(',');
    return {
      date,
      status,
      holiday,
      statutory: statutory === '1',
      sourceYear: Number(sourceYear),
      confidence,
    };
  });

const names = {
  NEW_YEAR: ['元旦', "New Year's Day"],
  SPRING_FESTIVAL: ['春节', 'Spring Festival'],
  TOMB_SWEEPING: ['清明节', 'Tomb-Sweeping Day'],
  LABOUR_DAY: ['劳动节', 'Labour Day'],
  DRAGON_BOAT: ['端午节', 'Dragon Boat Festival'],
  MID_AUTUMN: ['中秋节', 'Mid-Autumn Festival'],
  NATIONAL_DAY: ['国庆节', 'National Day'],
  VICTORY_DAY_70: ['中国人民抗日战争暨世界反法西斯战争胜利70周年纪念日', 'Victory Day 70th Anniversary'],
};

mkdirSync(outputDir, { recursive: true });
for (let year = sources.startYear; year <= sources.endYear; year++) {
  const yearRows = rows.filter((row) => row.date.startsWith(`${year}-`));
  const sourceMeta = sources.years[String(year)];
  const sourceId = `cn-holiday-notice-${year}`;
  const rules = [];

  for (const row of yearRows) {
    const [zh, en] = names[row.holiday];
    const base = {
      type: 'FIXED_DATE',
      displayNames: {
        'zh-CN': [zh],
        'en-US': [en],
      },
      labels: [row.holiday],
      sourceRefs: [sourceId],
      date: row.date,
    };
    if (row.status === 'WORK') {
      rules.push({
        ...base,
        id: `${year}-${row.date}-adjusted-workday`,
        dayKind: 'ADJUSTED_WORKDAY',
        labels: [row.holiday, 'ADJUSTED_WORKDAY'],
      });
      continue;
    }
    rules.push({
      ...base,
      id: `${year}-${row.date}-official-holiday`,
      dayKind: 'OFFICIAL_HOLIDAY',
    });
    if (row.statutory) {
      rules.push({
        ...base,
        id: `${year}-${row.date}-statutory-holiday`,
        dayKind: 'STATUTORY_HOLIDAY',
        labels: [row.holiday, 'STATUTORY'],
      });
    }
  }

  const doc = {
    meta: {
      specVersion: '2.0.0',
      bundleId: `CN-${year}`,
      regionCode: 'CN',
      parentRegionCode: null,
      year,
      validFrom: `${year}-01-01`,
      validTo: `${year}-12-31`,
      calendarSystem: 'GREGORIAN',
      timezone: 'Asia/Shanghai',
      weekendMask: ['SAT', 'SUN'],
      locales: ['zh-CN', 'en-US'],
      sourceVersion: `${year}.${sourceMeta.confidence}`,
      generatedAt: sources.generatedAt,
      generator: {
        name: 'api-assets/calendar',
        version: '2.0.0',
      },
      extensions: {
        confidence: sourceMeta.confidence,
      },
    },
    sources: [{
      id: sourceId,
      type: sourceMeta.confidence === 'GOV_NOTICE' ? 'GOV_NOTICE' : 'THIRD_PARTY_JSON',
      title: `${year} 年中国大陆节假日安排（${sourceMeta.confidence}）`,
      url: sourceMeta.papers[0],
      publishedAt: `${Math.max(1999, year - 1)}-12-01`,
    }],
    rules,
    overrides: [],
    extensions: {
      papers: sourceMeta.papers,
      confidence: sourceMeta.confidence,
    },
  };
  writeFileSync(join(outputDir, `${year}.canon.json`), `${JSON.stringify(doc, null, 2)}\n`);
}

console.log(`✓ 已从通用 CSV 生成 ${sources.startYear}-${sources.endYear} canonical 数据`);
