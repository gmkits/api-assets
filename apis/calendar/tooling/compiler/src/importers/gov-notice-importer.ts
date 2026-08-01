// ============================================================
// Holiday Data Platform — Gov Notice Importer
// ============================================================

import type {
  CanonicalDocument,
  SourceRecord,
  HolidayRule,
  CommonMeta,
  DayKind,
  HolidayLabel,
  MultiLangNames,
} from '../spec.js';

// --- Raw source format matching data/raw/CN/*.source.json ---

/** A single holiday entry in the raw government notice JSON. */
export interface RawHolidayEntry {
  name: string;
  nameEn: string;
  holidayDates: string[];
  adjustedWorkdays: string[];
  /** Optional statutory dates; if absent, inferred from the first day of holidayDates. */
  statutoryDates?: string[];
}

/** Raw government notice JSON format. */
export interface RawGovNotice {
  sourceType: 'GOV_NOTICE';
  title: string;
  url: string;
  publishedAt: string;
  regionCode: string;
  year: number;
  holidays: RawHolidayEntry[];
}

// Map Chinese holiday names to standard labels
const NAME_TO_LABEL: Record<string, HolidayLabel> = {
  '元旦': 'NEW_YEAR',
  '春节': 'SPRING_FESTIVAL',
  '清明节': 'TOMB_SWEEPING',
  '劳动节': 'LABOUR_DAY',
  '端午节': 'DRAGON_BOAT',
  '中秋节': 'MID_AUTUMN',
  '国庆节': 'NATIONAL_DAY',
};

/**
 * Resolve holiday labels from a Chinese holiday name.
 * Handles compound names like "中秋节+国庆节".
 */
function resolveLabels(chineseName: string): string[] {
  const labels: string[] = [];
  for (const [key, label] of Object.entries(NAME_TO_LABEL)) {
    if (chineseName.includes(key)) {
      labels.push(label);
    }
  }
  return labels;
}

/**
 * Generate a deterministic rule ID.
 */
function ruleId(region: string, year: number, holidayName: string, suffix: string, index?: number): string {
  const base = `${region}-${year}-${holidayName}-${suffix}`;
  return index !== undefined ? `${base}-${index}` : base;
}

/**
 * Import a raw government notice JSON into a CanonicalDocument.
 *
 * @param raw - The raw government notice data
 * @returns A fully populated CanonicalDocument
 *
 * @example
 * ```ts
 * const raw = JSON.parse(fs.readFileSync('2025-gov-notice.source.json', 'utf-8'));
 * const doc = importGovNotice(raw);
 * ```
 */
export function importGovNotice(raw: RawGovNotice): CanonicalDocument {
  const sourceId = `gov-notice-${raw.regionCode}-${raw.year}`;

  const source: SourceRecord = {
    id: sourceId,
    type: 'GOV_NOTICE',
    title: raw.title,
    url: raw.url,
    publishedAt: raw.publishedAt,
  };

  const rules: HolidayRule[] = [];
  const now = new Date().toISOString();

  for (const holiday of raw.holidays) {
    const labels = resolveLabels(holiday.name);
    const displayNames: MultiLangNames = {
      zh: [holiday.name],
      en: [holiday.nameEn],
    };

    // Statutory dates: use provided or infer first day of holidayDates
    const statutoryDates = holiday.statutoryDates ?? (
      holiday.holidayDates.length > 0 ? [holiday.holidayDates[0]] : []
    );

    // Holiday date range rule (OFFICIAL_HOLIDAY)
    if (holiday.holidayDates.length > 0) {
      const sorted = [...holiday.holidayDates].sort();
      if (sorted.length === 1) {
        rules.push({
          id: ruleId(raw.regionCode, raw.year, holiday.nameEn, 'holiday'),
          type: 'FIXED_DATE',
          dayKind: 'OFFICIAL_HOLIDAY',
          displayNames,
          labels: [...labels],
          sourceRefs: [sourceId],
          date: sorted[0],
        });
      } else {
        rules.push({
          id: ruleId(raw.regionCode, raw.year, holiday.nameEn, 'holiday'),
          type: 'DATE_RANGE',
          dayKind: 'OFFICIAL_HOLIDAY',
          displayNames,
          labels: [...labels],
          sourceRefs: [sourceId],
          from: sorted[0],
          to: sorted[sorted.length - 1],
        });
      }
    }

    // Statutory date rules (STATUTORY_HOLIDAY)
    for (let i = 0; i < statutoryDates.length; i++) {
      rules.push({
        id: ruleId(raw.regionCode, raw.year, holiday.nameEn, 'statutory', i),
        type: 'FIXED_DATE',
        dayKind: 'STATUTORY_HOLIDAY',
        displayNames,
        labels: [...labels, 'STATUTORY'],
        sourceRefs: [sourceId],
        date: statutoryDates[i],
      });
    }

    // Adjusted workday rules (ADJUSTED_WORKDAY)
    for (let i = 0; i < holiday.adjustedWorkdays.length; i++) {
      rules.push({
        id: ruleId(raw.regionCode, raw.year, holiday.nameEn, 'adjusted', i),
        type: 'FIXED_DATE',
        dayKind: 'ADJUSTED_WORKDAY',
        displayNames,
        labels: ['ADJUSTED_WORKDAY'],
        sourceRefs: [sourceId],
        date: holiday.adjustedWorkdays[i],
      });
    }
  }

  const meta: CommonMeta = {
    specVersion: '1.0.0',
    bundleId: `${raw.regionCode}-${raw.year}`,
    regionCode: raw.regionCode,
    parentRegionCode: null,
    year: raw.year,
    validFrom: `${raw.year}-01-01`,
    validTo: `${raw.year}-12-31`,
    calendarSystem: 'GREGORIAN',
    timezone: 'Asia/Shanghai',
    weekendMask: ['SAT', 'SUN'],
    locales: ['zh', 'en'],
    sourceVersion: raw.publishedAt.replace(/-/g, '.'),
    generatedAt: now,
    generator: {
      name: '@api-assets/calendar-compiler',
      version: '1.0.0',
    },
    extensions: {},
  };

  return {
    meta,
    sources: [source],
    rules,
    overrides: [],
    extensions: {},
  };
}
