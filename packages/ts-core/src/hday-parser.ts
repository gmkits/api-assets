/**
 * @holiday/core — `.hday` Binary Bundle Parser
 *
 * Parses a `.hday` binary file (supplied as an `ArrayBuffer`) into an
 * in-memory {@link HdayBundle} structure.  Uses only `DataView` for binary
 * reads so the code works identically in Node.js and browsers.
 *
 * All multi-byte integers in the `.hday` format are **little-endian**.
 *
 * @module
 */

import {
  HDAY_MAGIC,
  HDAY_HEADER_SIZE,
  HDAY_SECTION_ENTRY_SIZE,
  HDAY_DAY_ENTRY_SIZE,
  SECTION_TYPES,
} from '@holiday/spec';

// ---------------------------------------------------------------------------
// Public types
// ---------------------------------------------------------------------------

/** Parsed header information from an `.hday` file. */
export interface HdayHeader {
  /** Magic string — must be `"HDAY"`. */
  magic: string;
  /** Major format version (currently 1). */
  majorVersion: number;
  /** Minor format version (currently 0). */
  minorVersion: number;
  /** Global flags (reserved — 0 in v1.0). */
  flags: number;
  /** Calendar year, e.g. 2026. */
  year: number;
  /** Region code, e.g. `"CN"`. */
  regionCode: string;
  /** Calendar system enum code (0 = GREGORIAN). */
  calendarSystem: number;
  /** Number of day entries (365 or 366). */
  dayCount: number;
  /** Number of sections in the section table. */
  sectionCount: number;
}

/**
 * A single day entry from the DAY_TABLE section.
 *
 * @see {@link https://github.com/.../spec/bundle-format.md | Bundle Format}
 */
export interface DayEntry {
  /** Bitfield flags — see `DAY_FLAGS` constants from `@holiday/spec`. */
  flags: number;
  /** Index into `nameLists` (0xFFFF ⟹ none). */
  nameListIndex: number;
  /** Index into `nameLists` for labels (0xFFFF ⟹ none). */
  labelListIndex: number;
  /** Reserved extension index (0xFFFF ⟹ unused). */
  extIndex: number;
}

/**
 * A name-list entry — a list of (key, value) string-index pairs.
 *
 * For **holiday names** the key points to a locale string (e.g. `"zh-CN"`),
 * and the value points to the localised name.
 *
 * For **labels** the key index is `0xFFFF` and the value points to a label
 * string (e.g. `"NEW_YEAR"`).
 */
export interface NameListEntry {
  /** Ordered key/value pairs referencing string table indices. */
  pairs: Array<{ keyIndex: number; valueIndex: number }>;
}

/** Section descriptor parsed from the section table. */
interface SectionInfo {
  /** Section type code (e.g. `SECTION_TYPES.DAY_TABLE`). */
  type: number;
  /** Absolute byte offset in the file. */
  offset: number;
  /** Section body length in bytes. */
  length: number;
}

/** Complete parsed representation of an `.hday` bundle. */
export interface HdayBundle {
  /** Parsed file header. */
  header: HdayHeader;
  /** Day entries indexed 0 … dayCount − 1 (Jan 1 = 0). */
  days: DayEntry[];
  /** Deduplicated string pool. */
  strings: string[];
  /** Name-list entries (used for both names and labels). */
  nameLists: NameListEntry[];
  /** 可选 EXT_JSON 中的审计元数据。 */
  metadata?: {
    specVersion?: string;
    sourceVersion?: string;
    generatedAt?: string;
  };
}

// ---------------------------------------------------------------------------
// Internal helpers
// ---------------------------------------------------------------------------

/** Shared UTF-8 `TextDecoder` instance. */
const utf8Decoder = new TextDecoder('utf-8');

/**
 * Read a fixed-length ASCII string from a `DataView`.
 *
 * @param view   - Source DataView.
 * @param offset - Byte offset to start reading.
 * @param length - Number of bytes to read.
 * @returns The decoded ASCII string.
 */
function readAscii(view: DataView, offset: number, length: number): string {
  const bytes = new Uint8Array(view.buffer, view.byteOffset + offset, length);
  return utf8Decoder.decode(bytes);
}

// ---------------------------------------------------------------------------
// Header parsing
// ---------------------------------------------------------------------------

/**
 * Parse the 32-byte file header.
 *
 * @param view - DataView over the entire `.hday` file.
 * @returns The parsed {@link HdayHeader}.
 * @throws {Error} If the file is too small or the magic bytes are wrong.
 */
function parseHeader(view: DataView): HdayHeader {
  if (view.byteLength < HDAY_HEADER_SIZE) {
    throw new Error(
        `.hday 文件无效：至少需要 ${HDAY_HEADER_SIZE} 字节，实际 ${view.byteLength} 字节`,
    );
  }

  const magic = readAscii(view, 0x00, 4);
  if (magic !== HDAY_MAGIC) {
    throw new Error(
        `.hday 魔数无效：期望 "${HDAY_MAGIC}"，实际 "${magic}"`,
    );
  }

  const majorVersion = view.getUint8(0x04);
  const minorVersion = view.getUint8(0x05);
  const flags = view.getUint16(0x06, true);
  const year = view.getUint16(0x08, true);
  const regionCodeLen = view.getUint8(0x0a);
  const regionCode = readAscii(view, 0x0b, regionCodeLen);
  const calendarSystem = view.getUint8(0x1b);
  const dayCount = view.getUint16(0x1c, true);
  const sectionCount = view.getUint16(0x1e, true);

  return {
    magic,
    majorVersion,
    minorVersion,
    flags,
    year,
    regionCode,
    calendarSystem,
    dayCount,
    sectionCount,
  };
}

// ---------------------------------------------------------------------------
// Section table parsing
// ---------------------------------------------------------------------------

/**
 * Parse the section table that immediately follows the header.
 *
 * @param view         - DataView over the entire file.
 * @param sectionCount - Number of section entries to read.
 * @returns Array of {@link SectionInfo} descriptors.
 */
function parseSectionTable(
  view: DataView,
  sectionCount: number,
): SectionInfo[] {
  const sections: SectionInfo[] = [];
  let offset = HDAY_HEADER_SIZE; // section table starts right after header

  for (let i = 0; i < sectionCount; i++) {
    const type = view.getUint16(offset, true);
    const sectionOffset = view.getUint32(offset + 2, true);
    const length = view.getUint16(offset + 6, true);
    sections.push({ type, offset: sectionOffset, length });
    offset += HDAY_SECTION_ENTRY_SIZE;
  }

  return sections;
}

/**
 * Find a section by its type code.
 *
 * @param sections - Parsed section descriptors.
 * @param type     - Section type to look for.
 * @returns The matching {@link SectionInfo}, or `undefined` if absent.
 */
function findSection(
  sections: SectionInfo[],
  type: number,
): SectionInfo | undefined {
  return sections.find((s) => s.type === type);
}

// ---------------------------------------------------------------------------
// DAY_TABLE parsing
// ---------------------------------------------------------------------------

/**
 * Parse the DAY_TABLE section.
 *
 * @param view     - DataView over the entire file.
 * @param section  - Section descriptor for DAY_TABLE.
 * @param dayCount - Expected number of day entries (from the header).
 * @returns Array of {@link DayEntry} items.
 */
function parseDayTable(
  view: DataView,
  section: SectionInfo,
  dayCount: number,
): DayEntry[] {
  const days: DayEntry[] = [];
  let offset = section.offset;

  for (let i = 0; i < dayCount; i++) {
    const flags = view.getUint16(offset, true);
    const nameListIndex = view.getUint16(offset + 2, true);
    const labelListIndex = view.getUint16(offset + 4, true);
    const extIndex = view.getUint16(offset + 6, true);
    days.push({ flags, nameListIndex, labelListIndex, extIndex });
    offset += HDAY_DAY_ENTRY_SIZE;
  }

  return days;
}

// ---------------------------------------------------------------------------
// STRING_TABLE parsing
// ---------------------------------------------------------------------------

/**
 * Parse the STRING_TABLE section.
 *
 * Layout: `u16 stringCount` followed by `stringCount` entries, each being
 * `u16 length` + `length` UTF-8 bytes.
 *
 * @param view    - DataView over the entire file.
 * @param section - Section descriptor for STRING_TABLE.
 * @returns Array of decoded strings, indexed from 0.
 */
function parseStringTable(view: DataView, section: SectionInfo): string[] {
  let offset = section.offset;
  const stringCount = view.getUint16(offset, true);
  offset += 2;

  const strings: string[] = [];
  for (let i = 0; i < stringCount; i++) {
    const len = view.getUint16(offset, true);
    offset += 2;
    const bytes = new Uint8Array(
      view.buffer,
      view.byteOffset + offset,
      len,
    );
    strings.push(utf8Decoder.decode(bytes));
    offset += len;
  }

  return strings;
}

// ---------------------------------------------------------------------------
// NAME_LIST_TABLE parsing
// ---------------------------------------------------------------------------

/**
 * Parse the NAME_LIST_TABLE section.
 *
 * Layout: `u16 listCount` followed by `listCount` entries. Each entry starts
 * with `u16 pairCount`, then `pairCount` × 4-byte pairs (key u16, value u16).
 *
 * @param view    - DataView over the entire file.
 * @param section - Section descriptor for NAME_LIST_TABLE.
 * @returns Array of {@link NameListEntry} items.
 */
function parseNameListTable(
  view: DataView,
  section: SectionInfo,
): NameListEntry[] {
  let offset = section.offset;
  const listCount = view.getUint16(offset, true);
  offset += 2;

  const lists: NameListEntry[] = [];
  for (let i = 0; i < listCount; i++) {
    const pairCount = view.getUint16(offset, true);
    offset += 2;

    const pairs: NameListEntry['pairs'] = [];
    for (let p = 0; p < pairCount; p++) {
      const keyIndex = view.getUint16(offset, true);
      const valueIndex = view.getUint16(offset + 2, true);
      pairs.push({ keyIndex, valueIndex });
      offset += 4;
    }
    lists.push({ pairs });
  }

  return lists;
}

// ---------------------------------------------------------------------------
// Public API
// ---------------------------------------------------------------------------

/**
 * Parse a `.hday` binary bundle from an `ArrayBuffer`.
 *
 * The function validates the magic bytes and major version, then decodes the
 * section table, day table, string table, and name-list table.
 *
 * @param data - Raw bytes of the `.hday` file.
 * @returns A fully-parsed {@link HdayBundle}.
 * @throws {Error} On invalid magic, unsupported major version, or missing
 *   required sections (DAY_TABLE, STRING_TABLE, NAME_LIST_TABLE).
 *
 * @example
 * ```ts
 * const buf = await fs.promises.readFile('CN/2026.hday');
 * const bundle = parseHdayBundle(buf.buffer);
 * console.log(bundle.header.year); // 2026
 * ```
 */
export function parseHdayBundle(data: ArrayBuffer): HdayBundle {
  const view = new DataView(data);

  // 1. Parse & validate header
  const header = parseHeader(view);
  if (header.majorVersion !== 1) {
    throw new Error(
        `不支持的 .hday 主版本号: ${header.majorVersion}（期望 1）`,
    );
  }

  // 2. Parse section table
  const sections = parseSectionTable(view, header.sectionCount);

  // 3. Locate required sections
  const daySection = findSection(sections, SECTION_TYPES.DAY_TABLE);
  if (!daySection) {
      throw new Error('.hday 文件缺少必需的 DAY_TABLE 段');
  }

  const stringSection = findSection(sections, SECTION_TYPES.STRING_TABLE);
  if (!stringSection) {
      throw new Error('.hday 文件缺少必需的 STRING_TABLE 段');
  }

  const nameListSection = findSection(sections, SECTION_TYPES.NAME_LIST_TABLE);
  if (!nameListSection) {
      throw new Error('.hday 文件缺少必需的 NAME_LIST_TABLE 段');
  }

  // 4. Parse each section
  const days = parseDayTable(view, daySection, header.dayCount);
  const strings = parseStringTable(view, stringSection);
  const nameLists = parseNameListTable(view, nameListSection);
  const extSection = findSection(sections, SECTION_TYPES.EXT_JSON);
  let metadata: HdayBundle['metadata'] = {};
  if (extSection) {
    const length = view.getUint32(extSection.offset, true);
    const bytes = new Uint8Array(
      view.buffer,
      view.byteOffset + extSection.offset + 4,
      length,
    );
    metadata = JSON.parse(utf8Decoder.decode(bytes)) as HdayBundle['metadata'];
  }

  // Freeze the bundle to prevent accidental mutation by consumers.
  // Caches share parsed bundles across queries, so mutating any field
  // would corrupt unrelated callers.  Object.freeze is shallow + cheap;
  // the inner arrays only contain primitives or already-frozen records.
  return Object.freeze({
    header: Object.freeze(header),
    days: Object.freeze(days),
    strings: Object.freeze(strings),
    nameLists: Object.freeze(nameLists),
    metadata: Object.freeze(metadata),
  }) as HdayBundle;
}
