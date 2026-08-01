import { readFileSync, writeFileSync } from 'node:fs';

const MAGIC = 'CDAT';
const HEADER_SIZE = 16;
const SECTION_ENTRY_SIZE = 12;
const FLAG_CRITICAL = 1;
const SECTION_LUNAR_YEARS = 1;
const SECTION_SOLAR_TERMS = 2;
const LUNAR_START_YEAR = 1900;
const LUNAR_END_YEAR = 2100;
const SOLAR_START_YEAR = 1901;
const SOLAR_END_YEAR = 2100;
const TERM_COUNT = 24;
const SOLAR_TERM_BASE_DAYS = [
  4, 19, 3, 18, 4, 19, 4, 19, 4, 20, 4, 20,
  6, 22, 6, 22, 6, 22, 7, 22, 6, 21, 6, 21,
];

function crc32(bytes) {
  let crc = 0xffffffff;
  for (const value of bytes) {
    crc ^= value;
    for (let bit = 0; bit < 8; bit++) {
      crc = (crc >>> 1) ^ ((crc & 1) ? 0xedb88320 : 0);
    }
  }
  return (crc ^ 0xffffffff) >>> 0;
}

function parseLunarYears(path) {
  const values = [...readFileSync(path, 'utf8').matchAll(/0x([0-9a-f]+)/gi)]
    .map((match) => Number.parseInt(match[1], 16));
  const expected = LUNAR_END_YEAR - LUNAR_START_YEAR + 1;
  if (values.length !== expected) {
    throw new Error(`Expected ${expected} lunar years, got ${values.length}`);
  }
  return values;
}

function parseSolarTerms(path) {
  const rows = readFileSync(path, 'utf8').trim().split(/\r?\n/).slice(1);
  const packed = new Array(SOLAR_END_YEAR - SOLAR_START_YEAR + 1).fill(0n);
  const seen = Array.from({ length: packed.length }, () => new Uint8Array(TERM_COUNT));
  for (const row of rows) {
    const [date, indexText] = row.split(',');
    const [year, , day] = date.split('-').map(Number);
    if (year < SOLAR_START_YEAR || year > SOLAR_END_YEAR) continue;
    const termIndex = Number(indexText);
    const offset = day - SOLAR_TERM_BASE_DAYS[termIndex];
    if (!Number.isInteger(termIndex) || termIndex < 0 || termIndex >= TERM_COUNT
        || offset < 0 || offset > 3) {
      throw new Error(`Invalid solar-term row: ${row}`);
    }
    const yearIndex = year - SOLAR_START_YEAR;
    if (seen[yearIndex][termIndex]) {
      throw new Error(`Duplicate solar term ${year}#${termIndex}`);
    }
    seen[yearIndex][termIndex] = 1;
    packed[yearIndex] |= BigInt(offset) << BigInt(termIndex * 2);
  }
  for (let yearIndex = 0; yearIndex < packed.length; yearIndex++) {
    for (let termIndex = 0; termIndex < TERM_COUNT; termIndex++) {
      if (!seen[yearIndex][termIndex]) {
        throw new Error(
          `Missing solar term ${SOLAR_START_YEAR + yearIndex}#${termIndex}`,
        );
      }
    }
  }
  return packed;
}

function buildLunarSection(values) {
  const body = Buffer.alloc(8 + values.length * 4);
  body.writeUInt16LE(LUNAR_START_YEAR, 0);
  body.writeUInt16LE(LUNAR_END_YEAR, 2);
  body.writeUInt16LE(values.length, 4);
  body.writeUInt16LE(0, 6);
  values.forEach((value, index) => body.writeUInt32LE(value, 8 + index * 4));
  return body;
}

function buildSolarSection(values) {
  const body = Buffer.alloc(8 + TERM_COUNT + values.length * 6);
  body.writeUInt16LE(SOLAR_START_YEAR, 0);
  body.writeUInt16LE(SOLAR_END_YEAR, 2);
  body.writeUInt16LE(values.length, 4);
  body.writeUInt8(TERM_COUNT, 6);
  body.writeUInt8(0, 7);
  Buffer.from(SOLAR_TERM_BASE_DAYS).copy(body, 8);
  let offset = 8 + TERM_COUNT;
  for (const value of values) {
    for (let byte = 0; byte < 6; byte++) {
      body[offset++] = Number((value >> BigInt(byte * 8)) & 0xffn);
    }
  }
  return body;
}

/** Build the deterministic cross-language calendar asset. */
export function buildCalendarAsset({ lunarSource, solarSource, output }) {
  const lunar = buildLunarSection(parseLunarYears(lunarSource));
  const solar = buildSolarSection(parseSolarTerms(solarSource));
  const sectionCount = 2;
  const lunarOffset = HEADER_SIZE + sectionCount * SECTION_ENTRY_SIZE;
  const solarOffset = lunarOffset + lunar.length;
  const crcOffset = solarOffset + solar.length;
  const data = Buffer.alloc(crcOffset + 4);

  data.write(MAGIC, 0, 4, 'ascii');
  data.writeUInt8(1, 4);
  data.writeUInt8(0, 5);
  data.writeUInt16LE(sectionCount, 6);
  data.writeUInt32LE(0, 8);
  data.writeUInt32LE(0, 12);

  const sections = [
    [SECTION_LUNAR_YEARS, lunarOffset, lunar.length],
    [SECTION_SOLAR_TERMS, solarOffset, solar.length],
  ];
  sections.forEach(([type, offset, length], index) => {
    const entry = HEADER_SIZE + index * SECTION_ENTRY_SIZE;
    data.writeUInt16LE(type, entry);
    data.writeUInt16LE(FLAG_CRITICAL, entry + 2);
    data.writeUInt32LE(offset, entry + 4);
    data.writeUInt32LE(length, entry + 8);
  });
  lunar.copy(data, lunarOffset);
  solar.copy(data, solarOffset);
  data.writeUInt32LE(crc32(data.subarray(0, crcOffset)), crcOffset);
  writeFileSync(output, data);
  return data.length;
}
