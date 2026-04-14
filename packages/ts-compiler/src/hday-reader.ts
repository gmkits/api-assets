// ============================================================
// Holiday Data Platform — .hday Binary Reader
// ============================================================

import type {
  MaterializedYearData,
  MaterializedDay,
  CommonMeta,
  CalendarSystem,
  MultiLangNames,
} from '@holiday/spec';
import {
  DAY_FLAGS,
  SECTION_TYPES,
  HDAY_MAGIC,
  HDAY_HEADER_SIZE,
  HDAY_SECTION_ENTRY_SIZE,
  HDAY_DAY_ENTRY_SIZE,
  NO_INDEX,
  CALENDAR_SYSTEM_CODES,
} from '@holiday/spec';
import { crc32 } from './hday-compiler.js';
import { indexToDate } from './materializer.js';

/** Parsed section table entry. */
interface SectionEntry {
  type: number;
  offset: number;
}

/** Reverse lookup for calendar system codes. */
const CODE_TO_CALENDAR: Record<number, CalendarSystem> = {};
for (const [key, value] of Object.entries(CALENDAR_SYSTEM_CODES)) {
  CODE_TO_CALENDAR[value] = key as CalendarSystem;
}

/**
 * Parse a string table from the buffer at the given offset.
 * Format: [count:u16] [len:u16 bytes...]*
 */
function parseStringTable(buf: Buffer, offset: number): string[] {
  const count = buf.readUInt16LE(offset);
  let pos = offset + 2;
  const strings: string[] = [];

  for (let i = 0; i < count; i++) {
    const len = buf.readUInt16LE(pos);
    pos += 2;
    strings.push(buf.subarray(pos, pos + len).toString('utf-8'));
    pos += len;
  }

  return strings;
}

/**
 * A key-value pair from the name list table.
 */
interface NamePairParsed {
  keyIndex: number;
  valueIndex: number;
}

/**
 * Parse a name list table from the buffer at the given offset.
 * Format: [count:u16] [pairCount:u16 [keyIdx:u16 valueIdx:u16]*]*
 */
function parseNameListTable(buf: Buffer, offset: number): NamePairParsed[][] {
  const count = buf.readUInt16LE(offset);
  let pos = offset + 2;
  const lists: NamePairParsed[][] = [];

  for (let i = 0; i < count; i++) {
    const pairCount = buf.readUInt16LE(pos);
    pos += 2;
    const pairs: NamePairParsed[] = [];
    for (let j = 0; j < pairCount; j++) {
      const keyIndex = buf.readUInt16LE(pos);
      const valueIndex = buf.readUInt16LE(pos + 2);
      pairs.push({ keyIndex, valueIndex });
      pos += 4;
    }
    lists.push(pairs);
  }

  return lists;
}

/**
 * Read and parse an .hday binary file back into MaterializedYearData.
 *
 * Validates the CRC32 checksum to ensure data integrity.
 *
 * @param buf - The .hday buffer to parse
 * @returns The reconstructed MaterializedYearData
 * @throws Error if the file is invalid or the checksum doesn't match
 *
 * @example
 * ```ts
 * const buf = fs.readFileSync('CN-2025.hday');
 * const yearData = readHday(buf);
 * ```
 */
export function readHday(buf: Buffer): MaterializedYearData {
  // --- Validate magic ---
  const magic = buf.subarray(0, 4).toString('ascii');
  if (magic !== HDAY_MAGIC) {
    throw new Error(`Invalid .hday file: expected magic "${HDAY_MAGIC}", got "${magic}"`);
  }

  // --- Parse header (32 bytes per spec) ---
  const majorVersion = buf.readUInt8(4);
  const minorVersion = buf.readUInt8(5);
  if (majorVersion !== 1) {
    throw new Error(`Unsupported .hday major version: ${majorVersion}`);
  }

  // const flags = buf.readUInt16LE(6); // reserved
  const year = buf.readUInt16LE(8);
  const regionCodeLen = buf.readUInt8(10);
  const regionCode = buf.subarray(11, 11 + regionCodeLen).toString('utf-8');
  const calSystemCode = buf.readUInt8(27);
  const calendarSystem = CODE_TO_CALENDAR[calSystemCode] ?? 'GREGORIAN';
  const totalDays = buf.readUInt16LE(28);
  const numSections = buf.readUInt16LE(30);
  const sectionTableOffset = HDAY_HEADER_SIZE;

  // --- Verify CRC32 ---
  const crcOffset = buf.length - 4;
  const storedCrc = buf.readUInt32LE(crcOffset);
  const computedCrc = crc32(buf.subarray(0, crcOffset));
  if (storedCrc !== computedCrc) {
    throw new Error(
      `CRC32 mismatch: stored=0x${storedCrc.toString(16)}, computed=0x${computedCrc.toString(16)}`
    );
  }

  // --- Parse section table ---
  const sections: SectionEntry[] = [];
  for (let i = 0; i < numSections; i++) {
    const entryOffset = sectionTableOffset + i * HDAY_SECTION_ENTRY_SIZE;
    sections.push({
      type: buf.readUInt16LE(entryOffset),
      offset: buf.readUInt32LE(entryOffset + 2),
    });
  }

  const dayTableSection = sections.find(s => s.type === SECTION_TYPES.DAY_TABLE);
  const strTableSection = sections.find(s => s.type === SECTION_TYPES.STRING_TABLE);
  const nameListSection = sections.find(s => s.type === SECTION_TYPES.NAME_LIST_TABLE);

  if (!dayTableSection || !strTableSection || !nameListSection) {
    throw new Error('Missing required sections in .hday file');
  }

  // --- Parse string table ---
  const strings = parseStringTable(buf, strTableSection.offset);

  // --- Parse name list table ---
  const nameLists = parseNameListTable(buf, nameListSection.offset);

  /** Resolve a name list index to MultiLangNames (locale → names[]) */
  function resolveNames(idx: number): MultiLangNames {
    if (idx === NO_INDEX || idx >= nameLists.length) return {};
    const pairs = nameLists[idx];
    const result: MultiLangNames = {};
    for (const pair of pairs) {
      const locale = strings[pair.keyIndex] ?? 'default';
      const name = strings[pair.valueIndex] ?? '';
      if (!result[locale]) result[locale] = [];
      result[locale].push(name);
    }
    return result;
  }

  /** Resolve a label list index to string[] (labels have keyIndex=0xFFFF) */
  function resolveLabels(idx: number): string[] {
    if (idx === NO_INDEX || idx >= nameLists.length) return [];
    return nameLists[idx].map(pair => strings[pair.valueIndex] ?? '');
  }

  // --- Parse day table ---
  const days: Record<string, MaterializedDay> = {};
  for (let i = 0; i < totalDays; i++) {
    const offset = dayTableSection.offset + i * HDAY_DAY_ENTRY_SIZE;
    const flags = buf.readUInt16LE(offset);
    const nameListIdx = buf.readUInt16LE(offset + 2);
    const labelListIdx = buf.readUInt16LE(offset + 4);

    const dateStr = indexToDate(year, i);
    const holidayNames = resolveNames(nameListIdx);
    const labels = resolveLabels(labelListIdx);

    days[dateStr] = {
      isHoliday: !!(flags & DAY_FLAGS.IS_HOLIDAY),
      isWorkday: !!(flags & DAY_FLAGS.IS_WORKDAY),
      isWeekend: !!(flags & DAY_FLAGS.IS_WEEKEND),
      isStatutoryHoliday: !!(flags & DAY_FLAGS.IS_STATUTORY_HOLIDAY),
      isAdjustedWorkday: !!(flags & DAY_FLAGS.IS_ADJUSTED_WORKDAY),
      holidayNames,
      labels,
    };
  }

  // Build a minimal CommonMeta from the header info
  const meta: CommonMeta = {
    specVersion: '1.0.0',
    bundleId: `${regionCode}-${year}`,
    regionCode,
    parentRegionCode: null,
    year,
    validFrom: `${year}-01-01`,
    validTo: `${year}-12-31`,
    calendarSystem,
    timezone: '',
    weekendMask: ['SAT', 'SUN'],
    locales: [],
    sourceVersion: '',
    generatedAt: '',
    generator: { name: '', version: '' },
    extensions: {},
  };

  return { meta, days };
}
