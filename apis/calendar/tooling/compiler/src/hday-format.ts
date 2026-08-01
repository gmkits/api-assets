/**
 * `.hday` v2 shared binary contract and browser-safe decoder.
 *
 * Keeping the decoder beside the compiler prevents asset generation and audit
 * tooling from silently accepting different binary invariants.
 */

/** Current `.hday` major version. */
export const HDAY_VERSION_MAJOR = 2;

/** Current `.hday` minor version. */
export const HDAY_VERSION_MINOR = 0;

/** `.hday` file magic. */
export const HDAY_MAGIC = 'HDAY';

/** Fixed header size. */
export const HDAY_HEADER_SIZE = 32;

/** v2 section directory entry size. */
export const HDAY_SECTION_ENTRY_SIZE = 12;

/** Sparse day override record size. */
export const HDAY_DAY_OVERRIDE_SIZE = 8;

/** Name/label index sentinel. */
export const NO_INDEX = 0xffff;

/** Section directory flags. */
export const SECTION_FLAGS = {
  CRITICAL: 1 << 0,
} as const;

/** Defined v2 section types. */
export const SECTION_TYPES = {
  DAY_OVERRIDES: 0x0001,
  STRING_TABLE: 0x0002,
  NAME_LIST_TABLE: 0x0003,
  META_TABLE: 0x0004,
} as const;

/** State bits stored by a sparse day override. */
export const DAY_OVERRIDE_STATES = {
  FORCE_HOLIDAY: 1 << 0,
  FORCE_WORKDAY: 1 << 1,
  STATUTORY_HOLIDAY: 1 << 2,
  ADJUSTED_WORKDAY: 1 << 3,
} as const;

/** Stable metadata keys. Unknown keys must be ignored. */
export const META_KEYS = {
  SPEC_VERSION: 1,
  SOURCE_VERSION: 2,
  GENERATED_AT: 3,
} as const;

/** Runtime flags expanded from the sparse representation. */
export const HDAY_RUNTIME_FLAGS = {
  IS_HOLIDAY: 1 << 0,
  IS_WORKDAY: 1 << 1,
  IS_WEEKEND: 1 << 2,
  IS_STATUTORY_HOLIDAY: 1 << 3,
  IS_ADJUSTED_WORKDAY: 1 << 4,
  HAS_NAME: 1 << 5,
  HAS_LABEL: 1 << 6,
} as const;

/** Stable format error identifiers shared by callers and tests. */
export type HdayFormatErrorCode =
  | 'TOO_SMALL'
  | 'BAD_MAGIC'
  | 'UNSUPPORTED_VERSION'
  | 'BAD_CRC'
  | 'BAD_HEADER'
  | 'BAD_UTF8'
  | 'BAD_SECTION_TABLE'
  | 'UNKNOWN_CRITICAL_SECTION'
  | 'MISSING_SECTION'
  | 'BAD_SECTION'
  | 'BAD_INDEX'
  | 'BAD_DAY_OVERRIDE';

/** Error thrown when an `.hday` file violates the binary contract. */
export class HdayFormatError extends Error {
  readonly code: HdayFormatErrorCode;

  constructor(code: HdayFormatErrorCode, message: string) {
    super(message);
    this.name = 'HdayFormatError';
    this.code = code;
  }
}

/** Parsed header information. */
export interface HdayHeader {
  magic: string;
  majorVersion: number;
  minorVersion: number;
  flags: number;
  year: number;
  regionCode: string;
  calendarSystem: number;
  dayCount: number;
  sectionCount: number;
}

/** Expanded runtime day entry. */
export interface HdayDayEntry {
  flags: number;
  nameListIndex: number;
  labelListIndex: number;
}

/** Compact runtime status and annotation indexes for one complete year. */
export interface HdayDayTable {
  readonly length: number;
  readonly holidayBits: Uint32Array;
  readonly workdayBits: Uint32Array;
  readonly weekendBits: Uint32Array;
  readonly statutoryBits: Uint32Array;
  readonly adjustedBits: Uint32Array;
  /** `-1` means that the date has no holiday-name annotation. */
  readonly nameListIndexes: Int16Array;
  /** `-1` means that the date has no label annotation. */
  readonly labelListIndexes: Int16Array;
}

/** Resolved multilingual names retained after the string pool is discarded. */
export type HdayResolvedNames = Readonly<Record<string, ReadonlyArray<string>>>;

/** Shared name-list representation. */
export interface HdayNameListEntry {
  pairs: ReadonlyArray<Readonly<{ keyIndex: number; valueIndex: number }>>;
}

/** Parsed `.hday` bundle. */
export interface ParsedHdayBundle {
  header: Readonly<HdayHeader>;
  days: Readonly<HdayDayTable>;
  names: ReadonlyArray<HdayResolvedNames>;
  labels: ReadonlyArray<ReadonlyArray<string>>;
  metadata: Readonly<{
    specVersion?: string;
    sourceVersion?: string;
    generatedAt?: string;
  }>;
}

interface SectionInfo {
  type: number;
  flags: number;
  offset: number;
  length: number;
}

interface Utf8Decoder {
  decode(input?: ArrayBufferView): string;
}

declare const TextDecoder: {
  new(label?: string, options?: { fatal?: boolean }): Utf8Decoder;
};

const KNOWN_SECTION_FLAGS = SECTION_FLAGS.CRITICAL;
const KNOWN_OVERRIDE_STATES =
  DAY_OVERRIDE_STATES.FORCE_HOLIDAY
  | DAY_OVERRIDE_STATES.FORCE_WORKDAY
  | DAY_OVERRIDE_STATES.STATUTORY_HOLIDAY
  | DAY_OVERRIDE_STATES.ADJUSTED_WORKDAY;
const KNOWN_SECTION_TYPES = new Set<number>(Object.values(SECTION_TYPES));
const utf8Decoder = new TextDecoder('utf-8', { fatal: true });
const CRC32_TABLE = new Uint32Array(256);
for (let value = 0; value < CRC32_TABLE.length; value++) {
  let crc = value;
  for (let bit = 0; bit < 8; bit++) {
    crc = (crc >>> 1) ^ ((crc & 1) !== 0 ? 0xedb88320 : 0);
  }
  CRC32_TABLE[value] = crc >>> 0;
}

/** Standard CRC32 (polynomial `0xEDB88320`). */
export function crc32(bytes: Uint8Array): number {
  let crc = 0xffffffff;
  for (let i = 0; i < bytes.length; i++) {
    crc = (crc >>> 8) ^ CRC32_TABLE[(crc ^ bytes[i]) & 0xff];
  }
  return (crc ^ 0xffffffff) >>> 0;
}

function fail(code: HdayFormatErrorCode, message: string): never {
  throw new HdayFormatError(code, message);
}

function isLeapYear(year: number): boolean {
  return year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0);
}

function decodeUtf8(
  view: DataView,
  offset: number,
  length: number,
  code: HdayFormatErrorCode,
): string {
  try {
    return utf8Decoder.decode(
      new Uint8Array(view.buffer, view.byteOffset + offset, length),
    );
  } catch {
    return fail(code, `UTF-8 数据无效，offset=${offset}, length=${length}`);
  }
}

function ensureRange(
  start: number,
  length: number,
  limit: number,
  code: HdayFormatErrorCode,
  label: string,
): void {
  if (!Number.isSafeInteger(start) || !Number.isSafeInteger(length)
      || start < 0 || length < 0 || start > limit - length) {
    fail(code, `${label} 越界：offset=${start}, length=${length}, limit=${limit}`);
  }
}

function parseHeader(view: DataView): HdayHeader {
  ensureRange(0, HDAY_HEADER_SIZE + 4, view.byteLength, 'TOO_SMALL', '.hday 文件');
  const magic = decodeUtf8(view, 0, 4, 'BAD_MAGIC');
  if (magic !== HDAY_MAGIC) {
    fail('BAD_MAGIC', `.hday 魔数无效：期望 ${HDAY_MAGIC}，实际 ${magic}`);
  }
  const majorVersion = view.getUint8(4);
  const minorVersion = view.getUint8(5);
  if (majorVersion !== HDAY_VERSION_MAJOR) {
    fail(
      'UNSUPPORTED_VERSION',
      `不支持的 .hday 主版本号：${majorVersion}，期望 ${HDAY_VERSION_MAJOR}`,
    );
  }
  const year = view.getUint16(8, true);
  if (year === 0) {
    fail('BAD_HEADER', '年份必须大于 0');
  }
  const regionCodeLength = view.getUint8(10);
  if (regionCodeLength === 0 || regionCodeLength > 16) {
    fail('BAD_HEADER', `地区代码长度无效：${regionCodeLength}`);
  }
  const regionCode = decodeUtf8(view, 11, regionCodeLength, 'BAD_UTF8');
  for (let offset = 11 + regionCodeLength; offset < 27; offset++) {
    if (view.getUint8(offset) !== 0) {
      fail('BAD_HEADER', '地区代码未使用字节必须填 0');
    }
  }
  const calendarSystem = view.getUint8(27);
  if (calendarSystem > 1) {
    fail('BAD_HEADER', `历法编码无效：${calendarSystem}`);
  }
  const dayCount = view.getUint16(28, true);
  const expectedDays = isLeapYear(year) ? 366 : 365;
  if (dayCount !== expectedDays) {
    fail('BAD_HEADER', `${year} 年应有 ${expectedDays} 天，文件声明 ${dayCount} 天`);
  }
  const sectionCount = view.getUint16(30, true);
  if (sectionCount === 0) {
    fail('BAD_SECTION_TABLE', 'sectionCount 不能为 0');
  }
  return {
    magic,
    majorVersion,
    minorVersion,
    flags: view.getUint16(6, true),
    year,
    regionCode,
    calendarSystem,
    dayCount,
    sectionCount,
  };
}

function parseSections(
  view: DataView,
  sectionCount: number,
  crcOffset: number,
): SectionInfo[] {
  const tableLength = sectionCount * HDAY_SECTION_ENTRY_SIZE;
  const dataStart = HDAY_HEADER_SIZE + tableLength;
  ensureRange(
    HDAY_HEADER_SIZE,
    tableLength,
    crcOffset,
    'BAD_SECTION_TABLE',
    'section directory',
  );

  const sections: SectionInfo[] = [];
  const types = new Set<number>();
  for (let i = 0; i < sectionCount; i++) {
    const entry = HDAY_HEADER_SIZE + i * HDAY_SECTION_ENTRY_SIZE;
    const section: SectionInfo = {
      type: view.getUint16(entry, true),
      flags: view.getUint16(entry + 2, true),
      offset: view.getUint32(entry + 4, true),
      length: view.getUint32(entry + 8, true),
    };
    if ((section.flags & ~KNOWN_SECTION_FLAGS) !== 0) {
      fail('BAD_SECTION_TABLE', `section ${section.type} 含未知 flags`);
    }
    if (types.has(section.type)) {
      fail('BAD_SECTION_TABLE', `section ${section.type} 重复`);
    }
    types.add(section.type);
    ensureRange(
      section.offset,
      section.length,
      crcOffset,
      'BAD_SECTION_TABLE',
      `section ${section.type}`,
    );
    if (section.offset < dataStart) {
      fail('BAD_SECTION_TABLE', `section ${section.type} 与 header/directory 重叠`);
    }
    if (!KNOWN_SECTION_TYPES.has(section.type)
        && (section.flags & SECTION_FLAGS.CRITICAL) !== 0) {
      fail('UNKNOWN_CRITICAL_SECTION', `无法读取关键 section ${section.type}`);
    }
    sections.push(section);
  }

  const ordered = [...sections].sort((a, b) => a.offset - b.offset);
  for (let i = 1; i < ordered.length; i++) {
    const previous = ordered[i - 1];
    if (previous.offset + previous.length > ordered[i].offset) {
      fail('BAD_SECTION_TABLE', `section ${previous.type} 与 ${ordered[i].type} 重叠`);
    }
  }
  return sections;
}

function requiredSection(sections: SectionInfo[], type: number): SectionInfo {
  const section = sections.find((item) => item.type === type);
  if (!section) {
    fail('MISSING_SECTION', `.hday 缺少必需 section ${type}`);
  }
  return section;
}

function parseStrings(view: DataView, section: SectionInfo): string[] {
  const end = section.offset + section.length;
  ensureRange(section.offset, 2, end, 'BAD_SECTION', 'STRING_TABLE count');
  let offset = section.offset;
  const count = view.getUint16(offset, true);
  offset += 2;
  const strings: string[] = [];
  for (let i = 0; i < count; i++) {
    ensureRange(offset, 2, end, 'BAD_SECTION', 'STRING_TABLE length');
    const length = view.getUint16(offset, true);
    offset += 2;
    ensureRange(offset, length, end, 'BAD_SECTION', 'STRING_TABLE value');
    strings.push(decodeUtf8(view, offset, length, 'BAD_UTF8'));
    offset += length;
  }
  if (offset !== end) {
    fail('BAD_SECTION', 'STRING_TABLE 含未消费字节');
  }
  return strings;
}

function parseNameLists(
  view: DataView,
  section: SectionInfo,
  stringCount: number,
): HdayNameListEntry[] {
  const end = section.offset + section.length;
  ensureRange(section.offset, 2, end, 'BAD_SECTION', 'NAME_LIST_TABLE count');
  let offset = section.offset;
  const count = view.getUint16(offset, true);
  offset += 2;
  const lists: HdayNameListEntry[] = [];
  for (let i = 0; i < count; i++) {
    ensureRange(offset, 2, end, 'BAD_SECTION', 'NAME_LIST_TABLE pair count');
    const pairCount = view.getUint16(offset, true);
    offset += 2;
    ensureRange(offset, pairCount * 4, end, 'BAD_SECTION', 'NAME_LIST_TABLE pairs');
    const pairs: Array<Readonly<{ keyIndex: number; valueIndex: number }>> = [];
    for (let pair = 0; pair < pairCount; pair++) {
      const keyIndex = view.getUint16(offset, true);
      const valueIndex = view.getUint16(offset + 2, true);
      if (keyIndex !== NO_INDEX && keyIndex >= stringCount) {
        fail('BAD_INDEX', `名称 keyStringIndex 越界：${keyIndex}`);
      }
      if (valueIndex >= stringCount) {
        fail('BAD_INDEX', `名称 valueStringIndex 越界：${valueIndex}`);
      }
      pairs.push(Object.freeze({ keyIndex, valueIndex }));
      offset += 4;
    }
    lists.push(Object.freeze({ pairs: Object.freeze(pairs) }));
  }
  if (offset !== end) {
    fail('BAD_SECTION', 'NAME_LIST_TABLE 含未消费字节');
  }
  return lists;
}

function firstDayOfWeek(year: number): number {
  const first = new Date(0);
  first.setUTCFullYear(year, 0, 1);
  first.setUTCHours(0, 0, 0, 0);
  return first.getUTCDay();
}

function defaultDayFlags(firstWeekday: number, dayIndex: number): number {
  const dayOfWeek = (firstWeekday + dayIndex) % 7;
  return dayOfWeek === 0 || dayOfWeek === 6
    ? HDAY_RUNTIME_FLAGS.IS_HOLIDAY | HDAY_RUNTIME_FLAGS.IS_WEEKEND
    : HDAY_RUNTIME_FLAGS.IS_WORKDAY;
}

function parseDayOverrides(
  view: DataView,
  section: SectionInfo,
  header: HdayHeader,
  nameListCount: number,
): HdayDayTable {
  const expectedBaseLength = 2;
  ensureRange(section.offset, expectedBaseLength, section.offset + section.length,
    'BAD_SECTION', 'DAY_OVERRIDES count');
  const count = view.getUint16(section.offset, true);
  if (section.length !== 2 + count * HDAY_DAY_OVERRIDE_SIZE) {
    fail('BAD_SECTION', `DAY_OVERRIDES 长度与记录数不匹配：${section.length}`);
  }

  const firstWeekday = firstDayOfWeek(header.year);
  if (nameListCount > 0x7fff) {
    fail('BAD_INDEX', `名称列表过多，无法放入 Int16Array：${nameListCount}`);
  }
  const wordCount = Math.ceil(header.dayCount / 32);
  const holidayBits = new Uint32Array(wordCount);
  const workdayBits = new Uint32Array(wordCount);
  const weekendBits = new Uint32Array(wordCount);
  const statutoryBits = new Uint32Array(wordCount);
  const adjustedBits = new Uint32Array(wordCount);
  const nameListIndexes = new Int16Array(header.dayCount);
  const labelListIndexes = new Int16Array(header.dayCount);
  nameListIndexes.fill(-1);
  labelListIndexes.fill(-1);
  for (let index = 0; index < header.dayCount; index++) {
    const flags = defaultDayFlags(firstWeekday, index);
    setBit(
      (flags & HDAY_RUNTIME_FLAGS.IS_HOLIDAY) !== 0
        ? holidayBits
        : workdayBits,
      index,
    );
    if ((flags & HDAY_RUNTIME_FLAGS.IS_WEEKEND) !== 0) {
      setBit(weekendBits, index);
    }
  }
  let offset = section.offset + 2;
  let previousDay = -1;
  for (let i = 0; i < count; i++) {
    const dayIndex = view.getUint16(offset, true);
    const state = view.getUint8(offset + 2);
    const reserved = view.getUint8(offset + 3);
    const nameListIndex = view.getUint16(offset + 4, true);
    const labelListIndex = view.getUint16(offset + 6, true);
    offset += HDAY_DAY_OVERRIDE_SIZE;

    if (dayIndex >= header.dayCount || dayIndex <= previousDay) {
      fail('BAD_DAY_OVERRIDE', `dayIndex 必须按升序唯一且小于 dayCount：${dayIndex}`);
    }
    previousDay = dayIndex;
    if (reserved !== 0 || (state & ~KNOWN_OVERRIDE_STATES) !== 0) {
      fail('BAD_DAY_OVERRIDE', `dayIndex ${dayIndex} 含未知状态或保留值`);
    }
    const forceHoliday = (state & DAY_OVERRIDE_STATES.FORCE_HOLIDAY) !== 0;
    const forceWorkday = (state & DAY_OVERRIDE_STATES.FORCE_WORKDAY) !== 0;
    const statutory = (state & DAY_OVERRIDE_STATES.STATUTORY_HOLIDAY) !== 0;
    const adjusted = (state & DAY_OVERRIDE_STATES.ADJUSTED_WORKDAY) !== 0;
    if (forceHoliday === forceWorkday || (statutory && !forceHoliday)
        || (adjusted && !forceWorkday)) {
      fail('BAD_DAY_OVERRIDE', `dayIndex ${dayIndex} 状态组合无效`);
    }
    if (nameListIndex !== NO_INDEX && nameListIndex >= nameListCount) {
      fail('BAD_INDEX', `dayIndex ${dayIndex} 名称列表索引越界`);
    }
    if (labelListIndex !== NO_INDEX && labelListIndex >= nameListCount) {
      fail('BAD_INDEX', `dayIndex ${dayIndex} 标签列表索引越界`);
    }

    clearBit(holidayBits, dayIndex);
    clearBit(workdayBits, dayIndex);
    setBit(forceHoliday ? holidayBits : workdayBits, dayIndex);
    if (statutory) setBit(statutoryBits, dayIndex);
    if (adjusted) setBit(adjustedBits, dayIndex);
    nameListIndexes[dayIndex] =
      nameListIndex === NO_INDEX ? -1 : nameListIndex;
    labelListIndexes[dayIndex] =
      labelListIndex === NO_INDEX ? -1 : labelListIndex;
  }
  return {
    length: header.dayCount,
    holidayBits,
    workdayBits,
    weekendBits,
    statutoryBits,
    adjustedBits,
    nameListIndexes,
    labelListIndexes,
  };
}

function setBit(words: Uint32Array, index: number): void {
  words[index >>> 5] |= 1 << (index & 31);
}

function clearBit(words: Uint32Array, index: number): void {
  words[index >>> 5] &= ~(1 << (index & 31));
}

function resolveAnnotations(
  lists: HdayNameListEntry[],
  strings: string[],
): {
  names: HdayResolvedNames[];
  labels: ReadonlyArray<string>[];
} {
  const names: HdayResolvedNames[] = [];
  const labels: ReadonlyArray<string>[] = [];
  for (const list of lists) {
    const resolvedNames: Record<string, string[]> = {};
    const resolvedLabels: string[] = [];
    for (const pair of list.pairs) {
      if (pair.keyIndex === NO_INDEX) {
        resolvedLabels.push(strings[pair.valueIndex]);
      } else {
        const locale = strings[pair.keyIndex];
        (resolvedNames[locale] ??= []).push(strings[pair.valueIndex]);
      }
    }
    for (const values of Object.values(resolvedNames)) Object.freeze(values);
    names.push(Object.freeze(resolvedNames));
    labels.push(Object.freeze(resolvedLabels));
  }
  return { names, labels };
}

function parseMetadata(
  view: DataView,
  section: SectionInfo | undefined,
  strings: string[],
): ParsedHdayBundle['metadata'] {
  if (!section) return {};
  const end = section.offset + section.length;
  ensureRange(section.offset, 2, end, 'BAD_SECTION', 'META_TABLE count');
  const count = view.getUint16(section.offset, true);
  if (section.length !== 2 + count * 4) {
    fail('BAD_SECTION', 'META_TABLE 长度与记录数不匹配');
  }
  let offset = section.offset + 2;
  const metadata: {
    specVersion?: string;
    sourceVersion?: string;
    generatedAt?: string;
  } = {};
  for (let i = 0; i < count; i++) {
    const key = view.getUint16(offset, true);
    const valueIndex = view.getUint16(offset + 2, true);
    offset += 4;
    if (valueIndex >= strings.length) {
      fail('BAD_INDEX', `META_TABLE 字符串索引越界：${valueIndex}`);
    }
    if (key === META_KEYS.SPEC_VERSION) metadata.specVersion = strings[valueIndex];
    if (key === META_KEYS.SOURCE_VERSION) metadata.sourceVersion = strings[valueIndex];
    if (key === META_KEYS.GENERATED_AT) metadata.generatedAt = strings[valueIndex];
  }
  return metadata;
}

/** Parse and strictly validate a v2 `.hday` bundle. */
export function parseHdayBundle(data: ArrayBuffer): ParsedHdayBundle {
  const view = new DataView(data);
  const header = parseHeader(view);
  const crcOffset = view.byteLength - 4;
  const storedCrc = view.getUint32(crcOffset, true);
  const actualCrc = crc32(
    new Uint8Array(view.buffer, view.byteOffset, crcOffset),
  );
  if (storedCrc !== actualCrc) {
    fail(
      'BAD_CRC',
      `CRC32 校验失败：存储值=0x${storedCrc.toString(16)}，计算值=0x${actualCrc.toString(16)}`,
    );
  }

  const sections = parseSections(view, header.sectionCount, crcOffset);
  const daySection = requiredSection(sections, SECTION_TYPES.DAY_OVERRIDES);
  const stringSection = requiredSection(sections, SECTION_TYPES.STRING_TABLE);
  const nameListSection = requiredSection(sections, SECTION_TYPES.NAME_LIST_TABLE);
  for (const section of [daySection, stringSection, nameListSection]) {
    if ((section.flags & SECTION_FLAGS.CRITICAL) === 0) {
      fail('BAD_SECTION_TABLE', `必需 section ${section.type} 必须标记 CRITICAL`);
    }
  }

  const strings = parseStrings(view, stringSection);
  const nameLists = parseNameLists(view, nameListSection, strings.length);
  const days = parseDayOverrides(view, daySection, header, nameLists.length);
  const metadata = parseMetadata(
    view,
    sections.find((section) => section.type === SECTION_TYPES.META_TABLE),
    strings,
  );
  const annotations = resolveAnnotations(nameLists, strings);

  return Object.freeze({
    header: Object.freeze(header),
    days: Object.freeze(days),
    names: Object.freeze(annotations.names),
    labels: Object.freeze(annotations.labels),
    metadata: Object.freeze(metadata),
  });
}
