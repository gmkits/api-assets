/**
 * 农历模块测试。
 *
 * - 数据表完整性
 * - 闰月编解码
 * - 公历↔农历互转（已知日期 + CSV 全量）
 * - 闰月互转 / 无效闰月报错 / 小月溢出
 * - 天干地支 / 生肖
 * - 边界与异常
 * - 朔日天文估算
 */
import { describe, it } from 'node:test';
import { strict as assert } from 'node:assert';
import {readFileSync} from 'node:fs';
import {resolve, dirname} from 'node:path';
import {fileURLToPath} from 'node:url';

import {
    LUNAR_START_YEAR, LUNAR_END_YEAR,
    leapMonth, leapMonthDays, monthDays, yearDays,
    solarToLunar, solarToLunarFromStr, lunarToSolar, lunarToSolarStr,
    getTianGan, getDiZhi, getGanZhi, getShengXiao,
    getMonthName, getDayName,
    estimateNewMoonJDE, jdeToGregorian, estimateLunarNewYear,
    getSolarTerms, getSolarTerm, SOLAR_TERM_NAMES,
} from '../dist/esm/index.js';

// ─── 加载全量参照 CSV ───
const __dirname = dirname(fileURLToPath(import.meta.url));
const csvPath = resolve(__dirname, '../../../tests/lunar-golden.csv');
const csvRows = readFileSync(csvPath, 'utf-8')
    .split('\n')
    .slice(1)                     // 跳过表头
    .filter(Boolean)
    .map(line => {
        const [solarDate, lunarYear, lunarMonth, lunarDay, isLeapMonth] = line.split(',');
        const [sy, sm, sd] = solarDate.split('-').map(Number);
        return [sy, sm, sd, Number(lunarYear), Number(lunarMonth), Number(lunarDay), isLeapMonth === '1'];
    });

// ─── 数据表完整性 ───

describe('数据表完整性', () => {
    it('覆盖 1900-2100', () => {
    assert.equal(LUNAR_START_YEAR, 1900);
    assert.equal(LUNAR_END_YEAR, 2100);
  });

    it('每年天数 353-385，每月天数 29 或 30', () => {
    for (let y = LUNAR_START_YEAR; y <= LUNAR_END_YEAR; y++) {
        const yd = yearDays(y);
        assert.ok(yd >= 353 && yd <= 385, `${y}年 ${yd}天`);
      for (let m = 1; m <= 12; m++) {
          const md = monthDays(y, m);
          assert.ok(md === 29 || md === 30, `${y}-${m} ${md}天`);
      }
    }
  });
});

// ─── 闰月 ───

describe('闰月', () => {
    it('闰月月份 0-12，无闰月时天数 0，有闰月时 29 或 30', () => {
    for (let y = LUNAR_START_YEAR; y <= LUNAR_END_YEAR; y++) {
      const lm = leapMonth(y);
        assert.ok(lm >= 0 && lm <= 12, `${y}年闰月=${lm}`);
        const ld = leapMonthDays(y);
        if (lm === 0) {
            assert.equal(ld, 0, `${y}年无闰月但天数=${ld}`);
        } else {
            assert.ok(ld === 29 || ld === 30, `${y}年闰月天数=${ld}`);
      }
    }
  });

    it('已知闰月', () => {
    assert.equal(leapMonth(2025), 6);
    assert.equal(leapMonth(2023), 2);
    assert.equal(leapMonth(2024), 0);
  });
});

// ─── 闰月互转 ───

describe('闰月互转', () => {
    it('有闰月年份：闰月和非闰月各自正确互转', () => {
        // 2025 闰六月
        const leapM6Solar = lunarToSolar(2025, 6, 1, true);
        const normalM6Solar = lunarToSolar(2025, 6, 1, false);
        // 闰月和正常月应转到不同公历日期
        assert.notDeepStrictEqual(leapM6Solar, normalM6Solar);

        // 反查回来
        const backLeap = solarToLunar(...leapM6Solar);
        assert.equal(backLeap.month, 6);
        assert.equal(backLeap.isLeapMonth, true);

        const backNormal = solarToLunar(...normalM6Solar);
        assert.equal(backNormal.month, 6);
        assert.equal(backNormal.isLeapMonth, false);
    });

    it('2023 闰二月互转', () => {
        const [sy, sm, sd] = lunarToSolar(2023, 2, 15, true);
        const back = solarToLunar(sy, sm, sd);
        assert.equal(back.month, 2);
        assert.equal(back.day, 15);
        assert.equal(back.isLeapMonth, true);
    });
});

// ─── 错误处理 ───

describe('错误处理', () => {
    it('无闰月年份传 isLeapMonth=true 报错', () => {
        assert.throws(() => lunarToSolar(2024, 6, 1, true), RangeError);
        assert.throws(() => lunarToSolar(2024, 1, 1, true), RangeError);
    });

    it('有闰月但指定错误月份 isLeapMonth=true 报错', () => {
        // 2025 闰六月，传闰三月应报错
        assert.throws(() => lunarToSolar(2025, 3, 1, true), RangeError);
    });

    it('29 天小月传 day=30 报错', () => {
        // 找到一个 29 天月
        for (let m = 1; m <= 12; m++) {
            if (monthDays(2025, m) === 29) {
                assert.throws(() => lunarToSolar(2025, m, 30), RangeError);
                break;
            }
        }
    });

    it('超出年份范围', () => {
        assert.throws(() => solarToLunar(1899, 1, 1), RangeError);
        assert.throws(() => lunarToSolar(1899, 1, 1), RangeError);
        assert.throws(() => yearDays(1899), RangeError);
        assert.throws(() => yearDays(2101), RangeError);
  });

    it('无效月份/日期', () => {
        assert.throws(() => monthDays(2025, 0), RangeError);
        assert.throws(() => monthDays(2025, 13), RangeError);
        assert.throws(() => lunarToSolar(2025, 0, 1), RangeError);
        assert.throws(() => lunarToSolar(2025, 1, 0), RangeError);
        assert.throws(() => lunarToSolar(2025, 1, 31), RangeError);
  });

    it('无效日期格式', () => {
        assert.throws(() => solarToLunarFromStr('invalid'), Error);
        assert.throws(() => solarToLunarFromStr('2025/01/29'), Error);
    });
});

// ─── 已知日期验证 ───

describe('已知日期', () => {
    const cases = [
        // [公历Y,M,D, 农历Y,M,D, isLeap, 干支年, 生肖]
        [2025, 1, 29, 2025, 1, 1, false, '乙巳年', '蛇'],
        [2024, 2, 10, 2024, 1, 1, false, '甲辰年', '龙'],
        [2023, 1, 22, 2023, 1, 1, false, '癸卯年', '兔'],
        [1900, 1, 31, 1900, 1, 1, false, '庚子年', '鼠'],
        [2025, 10, 6, 2025, 8, 15, false, '乙巳年', '蛇'], // 中秋
    ];

    for (const [sy, sm, sd, ly, lm, ld, leap, ganZhi, sx] of cases) {
        it(`${sy}-${sm}-${sd} → ${ganZhi}${leap ? '闰' : ''}${lm}月${ld}日`, () => {
            const info = solarToLunar(sy, sm, sd);
            assert.equal(info.year, ly);
            assert.equal(info.month, lm);
            assert.equal(info.day, ld);
            assert.equal(info.isLeapMonth, leap);
            assert.equal(info.ganZhiYear, ganZhi);
            assert.equal(info.shengXiao, sx);
        });
    }

    it('solarToLunarFromStr', () => {
    const info = solarToLunarFromStr('2025-01-29');
    assert.equal(info.year, 2025);
    assert.equal(info.month, 1);
    assert.equal(info.day, 1);
  });

    it('lunarToSolarStr', () => {
        assert.equal(lunarToSolarStr(2025, 1, 1), '2025-01-29');
  });
});

// ─── 边界日期 ───

describe('边界日期', () => {
    it('1901-01-01 = 农历 1900 年十一月十一（HKO 首行）', () => {
        const info = solarToLunar(1901, 1, 1);
        assert.equal(info.year, 1900);
        assert.equal(info.month, 11);
        assert.equal(info.day, 11);
  });

    it('2100 年末附近仍可转换', () => {
        // 取 CSV 最后几行的日期验证
        const lastRow = csvRows[csvRows.length - 1];
        const [sy, sm, sd, ly, lm, ld, leap] = lastRow;
        const info = solarToLunar(sy, sm, sd);
        assert.equal(info.year, ly);
        assert.equal(info.month, lm);
        assert.equal(info.day, ld);
        assert.equal(info.isLeapMonth, leap);
  });
});

// ─── CSV 全量验证 ───

describe('CSV 全量验证（73,000+ 行）', () => {
    it('solarToLunar 对每一行均正确', () => {
        let checked = 0;
        for (const [sy, sm, sd, ly, lm, ld, leap] of csvRows) {
            const info = solarToLunar(sy, sm, sd);
            assert.equal(info.year, ly, `${sy}-${sm}-${sd} year`);
            assert.equal(info.month, lm, `${sy}-${sm}-${sd} month`);
            assert.equal(info.day, ld, `${sy}-${sm}-${sd} day`);
            assert.equal(info.isLeapMonth, leap, `${sy}-${sm}-${sd} leap`);
            checked++;
    }
        assert.ok(checked > 73000, `仅验证了 ${checked} 行`);
  });

    it('lunarToSolar 对每一行均正确（round-trip）', () => {
        let checked = 0;
        for (const [sy, sm, sd, ly, lm, ld, leap] of csvRows) {
            const [ry, rm, rd] = lunarToSolar(ly, lm, ld, leap);
            assert.equal(ry, sy, `lunar(${ly},${lm},${ld},${leap}) year`);
            assert.equal(rm, sm, `lunar(${ly},${lm},${ld},${leap}) month`);
            assert.equal(rd, sd, `lunar(${ly},${lm},${ld},${leap}) day`);
            checked++;
    }
        assert.ok(checked > 73000, `仅验证了 ${checked} 行`);
  });
});

// ─── 天干地支 / 生肖 ───

describe('天干地支', () => {
    it('甲子年循环', () => {
    assert.equal(getGanZhi(1984), '甲子');
    assert.equal(getShengXiao(1984), '鼠');
        assert.equal(getGanZhi(1984 + 60), '甲子');
        assert.equal(getGanZhi(1984 + 120), '甲子');
  });

    it('2025 乙巳蛇', () => {
    assert.equal(getTianGan(2025), '乙');
    assert.equal(getDiZhi(2025), '巳');
    assert.equal(getGanZhi(2025), '乙巳');
    assert.equal(getShengXiao(2025), '蛇');
  });
});

// ─── 月份/日期名称 ───

describe('名称', () => {
    it('月份名', () => {
    assert.equal(getMonthName(1, false), '正月');
    assert.equal(getMonthName(12, false), '腊月');
    assert.equal(getMonthName(4, true), '闰四月');
  });

    it('日期名', () => {
    assert.equal(getDayName(1), '初一');
    assert.equal(getDayName(15), '十五');
    assert.equal(getDayName(30), '三十');
  });
});

// ─── 朔日天文估算 ───

describe('朔日估算', () => {
    it('k=0 JDE ≈ 2451550', () => {
    const jde = estimateNewMoonJDE(0);
        assert.ok(jde > 2451549 && jde < 2451552);
  });

    it('JDE→公历：2451545.0 = 2000-01-01', () => {
    const [y, m, d] = jdeToGregorian(2451545.0);
        assert.deepStrictEqual([y, m, d], [2000, 1, 1]);
  });

    it('春节估算精度 ≤2 天', () => {
        const known = [
      [2020, 1, 25], [2021, 2, 12], [2022, 2, 1], [2023, 1, 22],
      [2024, 2, 10], [2025, 1, 29], [2026, 2, 17], [2027, 2, 6],
      [2028, 1, 26], [2029, 2, 13], [2030, 2, 3],
    ];
        for (const [yr, em, ed] of known) {
      const [ey, rm, rd] = estimateLunarNewYear(yr);
            assert.equal(ey, yr);
            const diff = Math.abs(Date.UTC(yr, em - 1, ed) - Date.UTC(ey, rm - 1, rd)) / 86400000;
            assert.ok(diff <= 2, `${yr}年偏差${diff}天`);
    }
  });

    it('相邻朔日间隔 29.2-29.9 天', () => {
    for (let k = -100; k < 100; k++) {
        const interval = estimateNewMoonJDE(k + 1) - estimateNewMoonJDE(k);
        assert.ok(interval >= 29.2 && interval <= 29.9, `k=${k} 间隔${interval.toFixed(4)}`);
    }
  });
});

// ─── 二十四节气 ───

// 加载节气全量参照 CSV
const solarTermsCsvPath = resolve(__dirname, '../../../tests/solar-terms-golden.csv');
const solarTermsCsvRows = readFileSync(solarTermsCsvPath, 'utf-8')
    .split('\n')
    .slice(1)                     // 跳过表头
    .filter(Boolean)
    .map(line => {
        const [year, termIndex, termName, month, day] = line.split(',');
        return [Number(year), Number(termIndex), termName, Number(month), Number(day)];
    });

describe('二十四节气', () => {
  it('SOLAR_TERM_NAMES 应有 24 个名称', () => {
    assert.equal(SOLAR_TERM_NAMES.length, 24);
  });

  it('getSolarTerms 应返回 24 个节气', () => {
    const terms = getSolarTerms(2025);
    assert.equal(terms.length, 24);
    for (const term of terms) {
      assert.ok(term.name, '节气应有名称');
      assert.ok(term.date[0] === 2025, `节气年份应为 2025，实际 ${term.date[0]}: ${term.name}`);
      assert.ok(term.date[1] >= 1 && term.date[1] <= 12, '月份应在 1-12');
      assert.ok(term.date[2] >= 1 && term.date[2] <= 31, '日期应在 1-31');
    }
  });

  it('2025 年已知节气日期精确匹配', () => {
    // 2025 年已知节气（来源：香港天文台 / 紫金山天文台数据）
    const known = [
      ['小寒', 1, 5],
      ['大寒', 1, 20],
      ['立春', 2, 3],
      ['雨水', 2, 18],
      ['惊蛰', 3, 5],
      ['春分', 3, 20],
      ['清明', 4, 4],
      ['谷雨', 4, 20],
      ['立夏', 5, 5],
      ['小满', 5, 21],
      ['芒种', 6, 5],
      ['夏至', 6, 21],
      ['小暑', 7, 7],
      ['大暑', 7, 22],
      ['立秋', 8, 7],
      ['处暑', 8, 23],
      ['白露', 9, 7],
      ['秋分', 9, 23],
      ['寒露', 10, 8],
      ['霜降', 10, 23],
      ['立冬', 11, 7],
      ['小雪', 11, 22],
      ['大雪', 12, 7],
      ['冬至', 12, 21],
    ];
    const terms = getSolarTerms(2025);
    for (const [name, expectedMonth, expectedDay] of known) {
      const term = terms.find(t => t.name === name);
      assert.ok(term, `应找到节气: ${name}`);
      assert.equal(term.date[1], expectedMonth, `${name} 月份不匹配`);
      assert.equal(term.date[2], expectedDay, `${name} 日期不匹配（期望 ${expectedMonth}-${expectedDay}，实际 ${term.date[1]}-${term.date[2]}）`);
    }
  });

  it('getSolarTerm 应在节气日返回名称', () => {
    const terms = getSolarTerms(2025);
    const firstTerm = terms[0];
    const result = getSolarTerm(firstTerm.date[0], firstTerm.date[1], firstTerm.date[2]);
    assert.equal(result, firstTerm.name);
  });

  it('getSolarTerm 应在非节气日返回 null', () => {
    // 2025-01-01 通常不是节气日
    const result = getSolarTerm(2025, 1, 1);
    assert.equal(result, null);
  });

  it('节气按时间顺序排列', () => {
    const terms = getSolarTerms(2025);
    for (let i = 1; i < terms.length; i++) {
      const prev = new Date(terms[i - 1].date[0], terms[i - 1].date[1] - 1, terms[i - 1].date[2]);
      const curr = new Date(terms[i].date[0], terms[i].date[1] - 1, terms[i].date[2]);
      assert.ok(curr >= prev, `节气顺序错误: ${terms[i - 1].name} (${prev.toISOString()}) 应早于 ${terms[i].name} (${curr.toISOString()})`);
    }
  });

  it('CSV 全量验证（4,824 行，1900-2100 全部 24 节气）', () => {
    let checked = 0;
    // 按年分组验证
    let currentYear = null;
    let currentTerms = null;
    for (const [year, termIndex, termName, month, day] of solarTermsCsvRows) {
      if (year !== currentYear) {
        currentYear = year;
        currentTerms = getSolarTerms(year);
      }
      const term = currentTerms[termIndex];
      assert.equal(term.name, termName, `${year} term ${termIndex} 名称不匹配`);
      assert.equal(term.date[1], month, `${year} ${termName} 月份不匹配`);
      assert.equal(term.date[2], day, `${year} ${termName} 日期不匹配（期望 ${month}-${day}，实际 ${term.date[1]}-${term.date[2]}）`);
      checked++;
    }
    assert.ok(checked >= 4824, `仅验证了 ${checked} 行，期望 ≥4824`);
  });
});
