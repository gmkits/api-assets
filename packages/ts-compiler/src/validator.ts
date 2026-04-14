// ============================================================
// Holiday Data Platform — Canonical Document Validator
// ============================================================

import type { CanonicalDocument, HolidayRule } from '@holiday/spec';

/** Result of validating a CanonicalDocument. */
export interface ValidationResult {
  valid: boolean;
  errors: string[];
  warnings: string[];
}

const DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

/**
 * Check if a string is a valid YYYY-MM-DD date.
 */
function isValidDate(s: string): boolean {
  if (!DATE_RE.test(s)) return false;
  const d = new Date(s + 'T00:00:00Z');
  return !isNaN(d.getTime()) && d.toISOString().startsWith(s);
}

/**
 * Check if a date falls within the given year.
 */
function isInYear(dateStr: string, year: number): boolean {
  return dateStr.startsWith(`${year}-`);
}

/**
 * Collect all dates affected by a rule.
 */
function ruleDates(rule: HolidayRule): string[] {
  if (rule.type === 'FIXED_DATE' && rule.date) {
    return [rule.date];
  }
  if (rule.type === 'DATE_RANGE' && rule.from && rule.to) {
    const dates: string[] = [];
    const start = new Date(rule.from + 'T00:00:00Z');
    const end = new Date(rule.to + 'T00:00:00Z');
    for (let d = new Date(start); d <= end; d.setUTCDate(d.getUTCDate() + 1)) {
      dates.push(d.toISOString().slice(0, 10));
    }
    return dates;
  }
  return [];
}

/**
 * Validate a CanonicalDocument for schema and semantic correctness.
 *
 * @param doc - The canonical document to validate
 * @returns A ValidationResult with errors and warnings
 *
 * @example
 * ```ts
 * const result = validate(canonicalDoc);
 * if (!result.valid) console.error(result.errors);
 * ```
 */
export function validate(doc: CanonicalDocument): ValidationResult {
  const errors: string[] = [];
  const warnings: string[] = [];

  // --- Schema validation ---
  if (!doc.meta) {
    errors.push('Missing required field: meta');
    return { valid: false, errors, warnings };
  }

  const { meta } = doc;

  if (!meta.specVersion) errors.push('meta.specVersion is required');
  if (!meta.bundleId) errors.push('meta.bundleId is required');
  if (!meta.regionCode) errors.push('meta.regionCode is required');
  if (!meta.year || typeof meta.year !== 'number') errors.push('meta.year must be a number');
  if (!meta.validFrom || !isValidDate(meta.validFrom)) errors.push('meta.validFrom must be a valid YYYY-MM-DD date');
  if (!meta.validTo || !isValidDate(meta.validTo)) errors.push('meta.validTo must be a valid YYYY-MM-DD date');
  if (!meta.calendarSystem) errors.push('meta.calendarSystem is required');
  if (!meta.timezone) errors.push('meta.timezone is required');
  if (!meta.weekendMask || !Array.isArray(meta.weekendMask)) errors.push('meta.weekendMask must be an array');
  if (!meta.locales || !Array.isArray(meta.locales)) errors.push('meta.locales must be an array');
  if (!meta.sourceVersion) errors.push('meta.sourceVersion is required');
  if (!meta.generatedAt) errors.push('meta.generatedAt is required');
  if (!meta.generator?.name) errors.push('meta.generator.name is required');

  if (!Array.isArray(doc.sources)) errors.push('sources must be an array');
  if (!Array.isArray(doc.rules)) errors.push('rules must be an array');
  if (!Array.isArray(doc.overrides)) errors.push('overrides must be an array');

  // Stop early if schema is broken
  if (errors.length > 0) {
    return { valid: false, errors, warnings };
  }

  // --- Source validation ---
  const sourceIds = new Set<string>();
  for (const source of doc.sources) {
    if (!source.id) errors.push('Source missing id');
    if (!source.type) errors.push(`Source "${source.id}" missing type`);
    if (!source.title) errors.push(`Source "${source.id}" missing title`);
    if (!source.publishedAt || !isValidDate(source.publishedAt)) {
      errors.push(`Source "${source.id}" has invalid publishedAt`);
    }
    if (sourceIds.has(source.id)) {
      errors.push(`Duplicate source id: ${source.id}`);
    }
    sourceIds.add(source.id);
  }

  // --- Rule validation ---
  const allRules = [...doc.rules, ...doc.overrides];
  const ruleIds = new Set<string>();

  for (const rule of allRules) {
    if (!rule.id) {
      errors.push('Rule missing id');
      continue;
    }
    if (ruleIds.has(rule.id)) {
      errors.push(`Duplicate rule id: ${rule.id}`);
    }
    ruleIds.add(rule.id);

    if (!rule.type) errors.push(`Rule "${rule.id}" missing type`);
    if (!rule.dayKind) errors.push(`Rule "${rule.id}" missing dayKind`);

    // Validate source references
    for (const ref of rule.sourceRefs) {
      if (!sourceIds.has(ref)) {
        warnings.push(`Rule "${rule.id}" references unknown source: ${ref}`);
      }
    }

    // Type-specific validation
    if (rule.type === 'FIXED_DATE') {
      if (!rule.date || !isValidDate(rule.date)) {
        errors.push(`Rule "${rule.id}" (FIXED_DATE) must have a valid date`);
      }
    }

    if (rule.type === 'DATE_RANGE') {
      if (!rule.from || !isValidDate(rule.from)) {
        errors.push(`Rule "${rule.id}" (DATE_RANGE) must have a valid from date`);
      }
      if (!rule.to || !isValidDate(rule.to)) {
        errors.push(`Rule "${rule.id}" (DATE_RANGE) must have a valid to date`);
      }
      if (rule.from && rule.to && rule.from > rule.to) {
        errors.push(`Rule "${rule.id}" (DATE_RANGE) from date is after to date`);
      }
    }
  }

  // --- Semantic validation ---

  // Check dates within year range (with tolerance for adjusted workdays that may be in prior year)
  for (const rule of allRules) {
    const dates = ruleDates(rule);
    for (const d of dates) {
      if (!isInYear(d, meta.year) && !isInYear(d, meta.year - 1) && !isInYear(d, meta.year + 1)) {
        warnings.push(`Rule "${rule.id}" has date ${d} outside year ${meta.year} ±1`);
      }
    }
  }

  // Check for conflicting rules on same date with incompatible day kinds
  const dateToRules = new Map<string, HolidayRule[]>();
  for (const rule of doc.rules) {
    const dates = ruleDates(rule);
    for (const d of dates) {
      const existing = dateToRules.get(d) ?? [];
      existing.push(rule);
      dateToRules.set(d, existing);
    }
  }

  for (const [date, rules] of dateToRules) {
    const hasHoliday = rules.some(r =>
      r.dayKind === 'OFFICIAL_HOLIDAY' || r.dayKind === 'STATUTORY_HOLIDAY'
    );
    const hasWorkday = rules.some(r => r.dayKind === 'ADJUSTED_WORKDAY');
    if (hasHoliday && hasWorkday) {
      errors.push(`Date ${date} has conflicting rules: both holiday and adjusted workday`);
    }
  }

  return {
    valid: errors.length === 0,
    errors,
    warnings,
  };
}
