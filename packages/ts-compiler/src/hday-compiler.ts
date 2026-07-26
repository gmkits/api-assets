// ============================================================
// Holiday Data Platform — .hday Binary Compiler
// ============================================================

import type { MaterializedYearData, MultiLangNames } from '@holiday/spec';
import {
  DAY_FLAGS,
  SECTION_TYPES,
  CALENDAR_SYSTEM_CODES,
  HDAY_MAGIC,
  HDAY_HEADER_SIZE,
  HDAY_SECTION_ENTRY_SIZE,
  HDAY_DAY_ENTRY_SIZE,
  NO_INDEX,
} from '@holiday/spec';
import { getDaysInYear, indexToDate } from './materializer.js';

// --- CRC32 ---

/** Standard CRC32 lookup table (polynomial 0xEDB88320). */
const CRC32_TABLE = new Uint32Array(256);
for (let i = 0; i < 256; i++) {
  let c = i;
  for (let j = 0; j < 8; j++) {
    c = c & 1 ? 0xEDB88320 ^ (c >>> 1) : c >>> 1;
  }
  CRC32_TABLE[i] = c;
}

/**
 * Compute CRC32 checksum for a buffer.
 *
 * @param buf - The data to checksum
 * @returns The CRC32 value as an unsigned 32-bit integer
 */
export function crc32(buf: Buffer): number {
  let crc = 0xFFFFFFFF;
  for (let i = 0; i < buf.length; i++) {
    crc = CRC32_TABLE[(crc ^ buf[i]) & 0xFF] ^ (crc >>> 8);
  }
  return (crc ^ 0xFFFFFFFF) >>> 0;
}

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
 * Binary layout:
 * 1. Header (32 bytes)
 * 2. Section table (3 entries × 8 bytes = 24 bytes)
 * 3. DAY_TABLE section
 * 4. STRING_TABLE section
 * 5. NAME_LIST_TABLE section
 * 6. CRC32 checksum (4 bytes)
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

  const strTable = new StringTableBuilder();
  const nameListTable = new NameListTableBuilder();

  // Pre-process all days to build string table and name list table
  interface DayEntry {
    flags: number;
    nameListIdx: number;
    labelListIdx: number;
  }
  const dayEntries: DayEntry[] = [];

  for (let i = 0; i < totalDays; i++) {
    const dateStr = indexToDate(year, i);
    const day = days[dateStr];

    if (!day) {
      dayEntries.push({ flags: DAY_FLAGS.IS_WORKDAY, nameListIdx: NO_INDEX, labelListIdx: NO_INDEX });
      continue;
    }

    let flags = 0;
    if (day.isHoliday) flags |= DAY_FLAGS.IS_HOLIDAY;
    if (day.isWorkday) flags |= DAY_FLAGS.IS_WORKDAY;
    if (day.isWeekend) flags |= DAY_FLAGS.IS_WEEKEND;
    if (day.isStatutoryHoliday) flags |= DAY_FLAGS.IS_STATUTORY_HOLIDAY;
    if (day.isAdjustedWorkday) flags |= DAY_FLAGS.IS_ADJUSTED_WORKDAY;

    // Build name list
    let nameListIdx = NO_INDEX;
    const namePairs = buildNamePairs(day.holidayNames, strTable);
    if (namePairs.length > 0) {
      flags |= DAY_FLAGS.HAS_NAME;
      nameListIdx = nameListTable.add(namePairs);
    }

    // Build label list
    let labelListIdx = NO_INDEX;
    if (day.labels.length > 0) {
      flags |= DAY_FLAGS.HAS_LABEL;
      const labelPairs = buildLabelPairs(day.labels, strTable);
      labelListIdx = nameListTable.add(labelPairs);
    }

    dayEntries.push({ flags, nameListIdx, labelListIdx });
  }

  // Serialize sections
  const dayTableBuf = Buffer.alloc(totalDays * HDAY_DAY_ENTRY_SIZE);
  for (let i = 0; i < totalDays; i++) {
    const entry = dayEntries[i];
    const offset = i * HDAY_DAY_ENTRY_SIZE;
    dayTableBuf.writeUInt16LE(entry.flags, offset);        // bytes 0-1: flags
    dayTableBuf.writeUInt16LE(entry.nameListIdx, offset + 2); // bytes 2-3: name list index
    dayTableBuf.writeUInt16LE(entry.labelListIdx, offset + 4); // bytes 4-5: label list index
    dayTableBuf.writeUInt16LE(0, offset + 6);                  // bytes 6-7: reserved
  }

  const strTableBuf = strTable.serialize();
  const nameListTableBuf = nameListTable.serialize();
  const extJson = Buffer.from(JSON.stringify({
    specVersion: meta.specVersion,
    sourceVersion: meta.sourceVersion,
    generatedAt: meta.generatedAt,
  }), 'utf8');
  const extJsonBuf = Buffer.alloc(4 + extJson.length);
  extJsonBuf.writeUInt32LE(extJson.length, 0);
  extJson.copy(extJsonBuf, 4);

  // Calculate offsets
  const sectionTableOffset = HDAY_HEADER_SIZE;
  const sectionTableSize = numSections * HDAY_SECTION_ENTRY_SIZE;
  const dataStart = sectionTableOffset + sectionTableSize;

  const dayTableOffset = dataStart;
  const strTableOffset = dayTableOffset + dayTableBuf.length;
  const nameListTableOffset = strTableOffset + strTableBuf.length;
  const extJsonOffset = nameListTableOffset + nameListTableBuf.length;
  const crcOffset = extJsonOffset + extJsonBuf.length;
  const totalSize = crcOffset + 4; // +4 for CRC32

  // Build the complete buffer
  const buf = Buffer.alloc(totalSize);

  // --- Header (32 bytes) ---
  // Bytes 0-3: Magic "HDAY"
  buf.write(HDAY_MAGIC, 0, 4, 'ascii');
  // Byte 4: Major version
  buf.writeUInt8(1, 4);
  // Byte 5: Minor version
  buf.writeUInt8(0, 5);
  // Bytes 6-7: Flags (reserved)
  buf.writeUInt16LE(0, 6);
  // Bytes 8-9: Year
  buf.writeUInt16LE(year, 8);
  // Byte 10: Region code length
  const regionBuf = Buffer.from(meta.regionCode, 'utf-8');
  buf.writeUInt8(regionBuf.length, 10);
  // Bytes 11-26: Region code (16 bytes, zero-padded)
  regionBuf.copy(buf, 11, 0, Math.min(regionBuf.length, 16));
  // Byte 27: Calendar system
  buf.writeUInt8(CALENDAR_SYSTEM_CODES[meta.calendarSystem] ?? 0, 27);
  // Bytes 28-29: Day count
  buf.writeUInt16LE(totalDays, 28);
  // Bytes 30-31: Section count
  buf.writeUInt16LE(numSections, 30);

  // --- Section Table (type:u16 + offset:u32 + length:u16 = 8B per entry) ---
  // Entry 0: DAY_TABLE
  buf.writeUInt16LE(SECTION_TYPES.DAY_TABLE, sectionTableOffset);
  buf.writeUInt32LE(dayTableOffset, sectionTableOffset + 2);
  buf.writeUInt16LE(dayTableBuf.length, sectionTableOffset + 6);

  // Entry 1: STRING_TABLE
  buf.writeUInt16LE(SECTION_TYPES.STRING_TABLE, sectionTableOffset + 8);
  buf.writeUInt32LE(strTableOffset, sectionTableOffset + 10);
  buf.writeUInt16LE(strTableBuf.length, sectionTableOffset + 14);

  // Entry 2: NAME_LIST_TABLE
  buf.writeUInt16LE(SECTION_TYPES.NAME_LIST_TABLE, sectionTableOffset + 16);
  buf.writeUInt32LE(nameListTableOffset, sectionTableOffset + 18);
  buf.writeUInt16LE(nameListTableBuf.length, sectionTableOffset + 22);

  // Entry 3: EXT_JSON（仅存放重建与审计需要的最小元数据）
  buf.writeUInt16LE(SECTION_TYPES.EXT_JSON, sectionTableOffset + 24);
  buf.writeUInt32LE(extJsonOffset, sectionTableOffset + 26);
  buf.writeUInt16LE(extJsonBuf.length, sectionTableOffset + 30);

  // --- Data sections ---
  dayTableBuf.copy(buf, dayTableOffset);
  strTableBuf.copy(buf, strTableOffset);
  nameListTableBuf.copy(buf, nameListTableOffset);
  extJsonBuf.copy(buf, extJsonOffset);

  // --- CRC32 over everything before the CRC field ---
  const checksum = crc32(buf.subarray(0, crcOffset));
  buf.writeUInt32LE(checksum, crcOffset);

  return buf;
}
