// ============================================================
// Holiday Data Platform — .hday Binary Compiler
// ============================================================

import type { MaterializedYearData, MultiLangNames } from './spec.js';
import {
  DAY_OVERRIDE_STATES,
  HDAY_DAY_OVERRIDE_SIZE,
  HDAY_VERSION_MAJOR,
  HDAY_VERSION_MINOR,
  META_KEYS,
  SECTION_FLAGS,
  SECTION_TYPES,
  CALENDAR_SYSTEM_CODES,
  HDAY_MAGIC,
  HDAY_HEADER_SIZE,
  HDAY_SECTION_ENTRY_SIZE,
  NO_INDEX,
  crc32,
} from './spec.js';
import { getDaysInYear, indexToDate } from './materializer.js';

export { crc32 };

// --- String Table Builder ---

class StringTableBuilder {
  private strings: string[] = [];
  private indexMap = new Map<string, number>();

  /** Add a string and return its index. */
  add(s: string): number {
    const existing = this.indexMap.get(s);
    if (existing !== undefined) return existing;
    const idx = this.strings.length;
    this.strings.push(s);
    this.indexMap.set(s, idx);
    return idx;
  }

  /** Get the index for a string, or NO_INDEX if not present. */
  indexOf(s: string): number {
    return this.indexMap.get(s) ?? NO_INDEX;
  }

  /** Serialize the string table to a buffer: [count:u16] [len:u16 bytes...]* */
  serialize(): Buffer {
    const parts: Buffer[] = [];
    const countBuf = Buffer.alloc(2);
    countBuf.writeUInt16LE(this.strings.length, 0);
    parts.push(countBuf);

    for (const s of this.strings) {
      const encoded = Buffer.from(s, 'utf-8');
      const lenBuf = Buffer.alloc(2);
      lenBuf.writeUInt16LE(encoded.length, 0);
      parts.push(lenBuf);
      parts.push(encoded);
    }

    return Buffer.concat(parts);
  }

  get count(): number {
    return this.strings.length;
  }
}

// --- Name List Table Builder ---

/** A key-value pair of string indices (keyIndex, valueIndex) */
interface NamePair {
  keyIndex: number;
  valueIndex: number;
}

class NameListTableBuilder {
  private lists: NamePair[][] = [];
  private indexMap = new Map<string, number>();

  /** Add a name list (array of key-value pairs) and return the name-list index. */
  add(pairs: NamePair[]): number {
    const key = pairs.map(p => `${p.keyIndex}:${p.valueIndex}`).join(',');
    const existing = this.indexMap.get(key);
    if (existing !== undefined) return existing;
    const idx = this.lists.length;
    this.lists.push(pairs);
    this.indexMap.set(key, idx);
    return idx;
  }

  /** Serialize: [count:u16] [pairCount:u16 [keyIdx:u16 valueIdx:u16]*]* */
  serialize(): Buffer {
    const parts: Buffer[] = [];
    const countBuf = Buffer.alloc(2);
    countBuf.writeUInt16LE(this.lists.length, 0);
    parts.push(countBuf);

    for (const list of this.lists) {
      const lenBuf = Buffer.alloc(2);
      lenBuf.writeUInt16LE(list.length, 0);
      parts.push(lenBuf);
      for (const pair of list) {
        const pairBuf = Buffer.alloc(4);
        pairBuf.writeUInt16LE(pair.keyIndex, 0);
        pairBuf.writeUInt16LE(pair.valueIndex, 2);
        parts.push(pairBuf);
      }
    }

    return Buffer.concat(parts);
  }
}

/**
 * Convert MultiLangNames into key-value pairs of string indices.
 * Each pair is (localeStringIndex, nameStringIndex).
 */
function buildNamePairs(
  names: MultiLangNames,
  strTable: StringTableBuilder,
): NamePair[] {
  const pairs: NamePair[] = [];
  const locales = Object.keys(names).sort();
  for (const locale of locales) {
    const keyIndex = strTable.add(locale);
    for (const name of names[locale]) {
      const valueIndex = strTable.add(name);
      pairs.push({ keyIndex, valueIndex });
    }
  }
  return pairs;
}

/**
 * Convert labels into key-value pairs where key is 0xFFFF (no locale).
 */
function buildLabelPairs(
  labels: string[],
  strTable: StringTableBuilder,
): NamePair[] {
  return labels.map(label => ({
    keyIndex: NO_INDEX,
    valueIndex: strTable.add(label),
  }));
}

/**
 * Compile MaterializedYearData into an .hday binary buffer.
 *
 * The v2 format stores only dates that differ from the normal weekday/weekend
 * calendar or carry annotations. This keeps each year independently
 * replaceable without paying eight bytes for every ordinary date.
 *
 * @param data - The materialized year data to compile
 * @returns A Buffer containing the .hday binary
 *
 * @example
 * ```ts
 * const hdayBuffer = compile(materializedData);
 * fs.writeFileSync('CN-2025.hday', hdayBuffer);
 * ```
 */
export function compile(data: MaterializedYearData): Buffer {
  const { meta, days } = data;
  const year = meta.year;
  const totalDays = getDaysInYear(year);
  const numSections = 4;
  const regionBuf = Buffer.from(meta.regionCode, 'utf8');
  if (regionBuf.length === 0 || regionBuf.length > 16) {
    throw new RangeError(
      `regionCode 的 UTF-8 长度必须为 1-16 字节，实际 ${regionBuf.length}`,
    );
  }

  const strTable = new StringTableBuilder();
  const nameListTable = new NameListTableBuilder();

  interface DayOverride {
    dayIndex: number;
    state: number;
    nameListIdx: number;
    labelListIdx: number;
  }
  const overrides: DayOverride[] = [];

  for (let i = 0; i < totalDays; i++) {
    const dateStr = indexToDate(year, i);
    const day = days[dateStr];

    if (!day) {
      continue;
    }

    let nameListIdx = NO_INDEX;
    const namePairs = buildNamePairs(day.holidayNames, strTable);
    if (namePairs.length > 0) {
      nameListIdx = nameListTable.add(namePairs);
    }

    let labelListIdx = NO_INDEX;
    if (day.labels.length > 0) {
      const labelPairs = buildLabelPairs(day.labels, strTable);
      labelListIdx = nameListTable.add(labelPairs);
    }

    const dayOfWeek = new Date(`${dateStr}T00:00:00Z`).getUTCDay();
    const defaultWeekend = dayOfWeek === 0 || dayOfWeek === 6;
    const differsFromDefault =
      day.isHoliday !== defaultWeekend
      || day.isWorkday === defaultWeekend
      || day.isStatutoryHoliday
      || day.isAdjustedWorkday
      || nameListIdx !== NO_INDEX
      || labelListIdx !== NO_INDEX;
    if (!differsFromDefault) continue;

    if (day.isHoliday === day.isWorkday) {
      throw new Error(`${dateStr} 的 isHoliday/isWorkday 必须互斥且恰有一个为 true`);
    }
    if (day.isStatutoryHoliday && !day.isHoliday) {
      throw new Error(`${dateStr} 的法定节假日必须同时是休息日`);
    }
    if (day.isAdjustedWorkday && !day.isWorkday) {
      throw new Error(`${dateStr} 的调休补班必须同时是工作日`);
    }

    let state = day.isHoliday
      ? DAY_OVERRIDE_STATES.FORCE_HOLIDAY
      : DAY_OVERRIDE_STATES.FORCE_WORKDAY;
    if (day.isStatutoryHoliday) {
      state |= DAY_OVERRIDE_STATES.STATUTORY_HOLIDAY;
    }
    if (day.isAdjustedWorkday) {
      state |= DAY_OVERRIDE_STATES.ADJUSTED_WORKDAY;
    }
    overrides.push({ dayIndex: i, state, nameListIdx, labelListIdx });
  }

  if (overrides.length > NO_INDEX) {
    throw new RangeError(`单年 override 数量超过 u16：${overrides.length}`);
  }
  const dayOverridesBuf = Buffer.alloc(2 + overrides.length * HDAY_DAY_OVERRIDE_SIZE);
  dayOverridesBuf.writeUInt16LE(overrides.length, 0);
  for (let i = 0; i < overrides.length; i++) {
    const entry = overrides[i];
    const offset = 2 + i * HDAY_DAY_OVERRIDE_SIZE;
    dayOverridesBuf.writeUInt16LE(entry.dayIndex, offset);
    dayOverridesBuf.writeUInt8(entry.state, offset + 2);
    dayOverridesBuf.writeUInt8(0, offset + 3);
    dayOverridesBuf.writeUInt16LE(entry.nameListIdx, offset + 4);
    dayOverridesBuf.writeUInt16LE(entry.labelListIdx, offset + 6);
  }

  const metaPairs = [
    [META_KEYS.SPEC_VERSION, strTable.add(meta.specVersion)],
    [META_KEYS.SOURCE_VERSION, strTable.add(meta.sourceVersion)],
    [META_KEYS.GENERATED_AT, strTable.add(meta.generatedAt)],
  ] as const;
  const strTableBuf = strTable.serialize();
  const nameListTableBuf = nameListTable.serialize();
  const metaTableBuf = Buffer.alloc(2 + metaPairs.length * 4);
  metaTableBuf.writeUInt16LE(metaPairs.length, 0);
  for (let i = 0; i < metaPairs.length; i++) {
    metaTableBuf.writeUInt16LE(metaPairs[i][0], 2 + i * 4);
    metaTableBuf.writeUInt16LE(metaPairs[i][1], 4 + i * 4);
  }

  const sectionTableOffset = HDAY_HEADER_SIZE;
  const sectionTableSize = numSections * HDAY_SECTION_ENTRY_SIZE;
  const dataStart = sectionTableOffset + sectionTableSize;
  const dayOverridesOffset = dataStart;
  const strTableOffset = dayOverridesOffset + dayOverridesBuf.length;
  const nameListTableOffset = strTableOffset + strTableBuf.length;
  const metaTableOffset = nameListTableOffset + nameListTableBuf.length;
  const crcOffset = metaTableOffset + metaTableBuf.length;
  const totalSize = crcOffset + 4;

  const buf = Buffer.alloc(totalSize);
  buf.write(HDAY_MAGIC, 0, 4, 'ascii');
  buf.writeUInt8(HDAY_VERSION_MAJOR, 4);
  buf.writeUInt8(HDAY_VERSION_MINOR, 5);
  buf.writeUInt16LE(0, 6);
  buf.writeUInt16LE(year, 8);
  buf.writeUInt8(regionBuf.length, 10);
  regionBuf.copy(buf, 11);
  buf.writeUInt8(CALENDAR_SYSTEM_CODES[meta.calendarSystem] ?? 0, 27);
  buf.writeUInt16LE(totalDays, 28);
  buf.writeUInt16LE(numSections, 30);

  const sections = [
    {
      type: SECTION_TYPES.DAY_OVERRIDES,
      flags: SECTION_FLAGS.CRITICAL,
      offset: dayOverridesOffset,
      length: dayOverridesBuf.length,
    },
    {
      type: SECTION_TYPES.STRING_TABLE,
      flags: SECTION_FLAGS.CRITICAL,
      offset: strTableOffset,
      length: strTableBuf.length,
    },
    {
      type: SECTION_TYPES.NAME_LIST_TABLE,
      flags: SECTION_FLAGS.CRITICAL,
      offset: nameListTableOffset,
      length: nameListTableBuf.length,
    },
    {
      type: SECTION_TYPES.META_TABLE,
      flags: 0,
      offset: metaTableOffset,
      length: metaTableBuf.length,
    },
  ];
  for (let i = 0; i < sections.length; i++) {
    const entryOffset = sectionTableOffset + i * HDAY_SECTION_ENTRY_SIZE;
    const section = sections[i];
    buf.writeUInt16LE(section.type, entryOffset);
    buf.writeUInt16LE(section.flags, entryOffset + 2);
    buf.writeUInt32LE(section.offset, entryOffset + 4);
    buf.writeUInt32LE(section.length, entryOffset + 8);
  }

  dayOverridesBuf.copy(buf, dayOverridesOffset);
  strTableBuf.copy(buf, strTableOffset);
  nameListTableBuf.copy(buf, nameListTableOffset);
  metaTableBuf.copy(buf, metaTableOffset);

  const checksum = crc32(buf.subarray(0, crcOffset));
  buf.writeUInt32LE(checksum, crcOffset);

  return buf;
}
