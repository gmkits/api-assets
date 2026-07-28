#!/usr/bin/env node

import { readFileSync, readdirSync } from 'node:fs';
import { join, resolve } from 'node:path';
import { readHday } from '../packages/ts-compiler/dist/esm/index.js';

const oldRoot = resolve(process.argv[2] ?? 'target/v1-baseline-20260728/bundles');
const newRoot = resolve(process.argv[3] ?? 'data/bundles');
let checked = 0;

for (const region of readdirSync(newRoot).sort()) {
  for (const file of readdirSync(join(newRoot, region))
    .filter((name) => name.endsWith('.hday'))
    .sort()) {
    const oldData = readV1(readFileSync(join(oldRoot, region, file)));
    const newData = readHday(readFileSync(join(newRoot, region, file)));
    for (const [date, expected] of Object.entries(oldData.days)) {
      const actual = newData.days[date];
      if (JSON.stringify(actual) !== JSON.stringify(expected)) {
        throw new Error(
          `${region}/${file} ${date} changed\n`
          + `v1=${JSON.stringify(expected)}\n`
          + `v2=${JSON.stringify(actual)}`,
        );
      }
      checked++;
    }
  }
}

console.log(`✓ v1/v2 全字段一致：${checked.toLocaleString('en-US')} 个日期`);

function readV1(buffer) {
  if (buffer.toString('ascii', 0, 4) !== 'HDAY' || buffer[4] !== 1) {
    throw new Error('Expected a v1 .hday baseline');
  }
  const year = buffer.readUInt16LE(8);
  const regionLength = buffer[10];
  const regionCode = buffer.toString('utf8', 11, 11 + regionLength);
  const dayCount = buffer.readUInt16LE(28);
  const sectionCount = buffer.readUInt16LE(30);
  const sections = new Map();
  for (let index = 0; index < sectionCount; index++) {
    const entry = 32 + index * 8;
    sections.set(buffer.readUInt16LE(entry), {
      offset: buffer.readUInt32LE(entry + 2),
      length: buffer.readUInt16LE(entry + 6),
    });
  }
  const strings = readStrings(buffer, sections.get(2));
  const lists = readLists(buffer, sections.get(3));
  const dayTable = sections.get(1);
  const days = {};
  for (let index = 0; index < dayCount; index++) {
    const offset = dayTable.offset + index * 8;
    const flags = buffer.readUInt16LE(offset);
    const nameIndex = buffer.readUInt16LE(offset + 2);
    const labelIndex = buffer.readUInt16LE(offset + 4);
    days[indexToDate(year, index)] = {
      isHoliday: (flags & 1) !== 0,
      isWorkday: (flags & 2) !== 0,
      isWeekend: (flags & 4) !== 0,
      isStatutoryHoliday: (flags & 8) !== 0,
      isAdjustedWorkday: (flags & 16) !== 0,
      holidayNames: resolveNames(nameIndex, lists, strings),
      labels: resolveLabels(labelIndex, lists, strings),
    };
  }
  return { regionCode, year, days };
}

function readStrings(buffer, section) {
  let offset = section.offset;
  const count = buffer.readUInt16LE(offset);
  offset += 2;
  const values = [];
  for (let index = 0; index < count; index++) {
    const length = buffer.readUInt16LE(offset);
    offset += 2;
    values.push(buffer.toString('utf8', offset, offset + length));
    offset += length;
  }
  return values;
}

function readLists(buffer, section) {
  let offset = section.offset;
  const count = buffer.readUInt16LE(offset);
  offset += 2;
  const values = [];
  for (let index = 0; index < count; index++) {
    const pairCount = buffer.readUInt16LE(offset);
    offset += 2;
    const pairs = [];
    for (let pair = 0; pair < pairCount; pair++) {
      pairs.push([
        buffer.readUInt16LE(offset),
        buffer.readUInt16LE(offset + 2),
      ]);
      offset += 4;
    }
    values.push(pairs);
  }
  return values;
}

function resolveNames(index, lists, strings) {
  if (index === 0xffff) return {};
  const result = {};
  for (const [key, value] of lists[index]) {
    if (key !== 0xffff) (result[strings[key]] ??= []).push(strings[value]);
  }
  return result;
}

function resolveLabels(index, lists, strings) {
  if (index === 0xffff) return [];
  return lists[index]
    .filter(([key]) => key === 0xffff)
    .map(([, value]) => strings[value]);
}

function indexToDate(year, index) {
  const date = new Date(Date.UTC(year, 0, 1 + index));
  return date.toISOString().slice(0, 10);
}
