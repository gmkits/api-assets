import { describe, it } from 'node:test';
import assert from 'node:assert/strict';

import { HolidayApiClient } from '../dist/esm/index.js';

function createJsonResponse(payload) {
  return {
    ok: true,
    status: 200,
    statusText: 'OK',
    async json() {
      return payload;
    },
  };
}

function createBinaryResponse(bytes) {
  return {
    ok: true,
    status: 200,
    statusText: 'OK',
    async arrayBuffer() {
      return bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength);
    },
  };
}

describe('HolidayApiClient', () => {
  it('应使用 regionCode 参数并归一化 DayInfo / lunar 字段', async () => {
    const urls = [];
    const client = new HolidayApiClient({
      baseUrl: 'https://api.example.com/',
      fetchFn: async (url) => {
        urls.push(String(url));
        return createJsonResponse({
          date: '2025-01-01',
          regionCode: 'CN',
          calendarSystem: 'GREGORIAN',
          holiday: true,
          workday: false,
          weekend: false,
          statutoryHoliday: true,
          adjustedWorkday: false,
          holidayNames: {
            'zh-CN': ['元旦'],
          },
          labels: ['NEW_YEAR', 'STATUTORY'],
          sourceVersion: '2025.01.01',
          extensions: {
            lunar: {
              year: 2024,
              month: 12,
              day: 2,
              leapMonth: false,
              ganZhiYear: '甲辰年',
              shengXiao: '龙',
              monthName: '腊月',
              dayName: '初二',
            },
            solarTerm: {
              index: 0,
              name: '小寒',
            },
          },
        });
      },
    });

    const info = await client.getDayInfo('2025-01-01');

    assert.equal(
      urls[0],
      'https://api.example.com/api/v1/day?date=2025-01-01&regionCode=CN',
    );
    assert.equal(info.isHoliday, true);
    assert.equal(info.isWorkday, false);
    assert.equal(info.isStatutoryHoliday, true);
    assert.deepEqual(info.extensions.lunar, {
      year: 2024,
      month: 12,
      day: 2,
      isLeapMonth: false,
      ganZhiYear: '甲辰年',
      shengXiao: '龙',
      monthName: '腊月',
      dayName: '初二',
    });
    assert.deepEqual(info.extensions.solarTerm, {
      index: 0,
      name: '小寒',
    });
  });

  it('应归一化区间和整年查询返回的数组', async () => {
    const urls = [];
    const payload = [
      {
        date: '2025-01-02',
        regionCode: 'CN',
        calendarSystem: 'GREGORIAN',
        holiday: false,
        workday: true,
        weekend: false,
        statutoryHoliday: false,
        adjustedWorkday: false,
        holidayNames: {},
        labels: [],
        sourceVersion: '2025.01.01',
        extensions: {},
      },
    ];
    const client = new HolidayApiClient({
      baseUrl: 'https://api.example.com',
      fetchFn: async (url) => {
        urls.push(String(url));
        return createJsonResponse(payload);
      },
    });

    const range = await client.getRange('2025-01-01', '2025-01-02', 'CN');
    const year = await client.getYear(2025, 'CN');

    assert.equal(
      urls[0],
      'https://api.example.com/api/v1/range?from=2025-01-01&to=2025-01-02&regionCode=CN',
    );
    assert.equal(
      urls[1],
      'https://api.example.com/api/v1/year?year=2025&regionCode=CN',
    );
    assert.equal(range[0].isWorkday, true);
    assert.equal(year[0].isHoliday, false);
  });

  it('应使用路径参数下载 bundle', async () => {
    const urls = [];
    const bytes = new Uint8Array([0x48, 0x44, 0x41, 0x59]);
    const client = new HolidayApiClient({
      baseUrl: 'https://api.example.com',
      fetchFn: async (url) => {
        urls.push(String(url));
        return createBinaryResponse(bytes);
      },
    });

    const bundle = await client.downloadBundle('CN', 2025);

    assert.equal(urls[0], 'https://api.example.com/api/v1/bundle/CN/2025');
    assert.deepEqual(Array.from(new Uint8Array(bundle)), Array.from(bytes));
  });
});
