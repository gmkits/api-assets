/**
 * 农历模块单元测试。
 *
 * 覆盖：
 * - 数据表完整性（201 年）
 * - 闰月解码
 * - 公历→农历转换（已知日期）
 * - 农历→公历转换
 * - 往返一致性（solar→lunar→solar）
 * - 天干地支 / 生肖
 * - 边界情况
 * - 每年天数合理性
 */
import { describe, it } from 'node:test';
import { strict as assert } from 'node:assert';

import {
  LUNAR_START_YEAR,
  LUNAR_END_YEAR,
  leapMonth,
  leapMonthDays,
  monthDays,
  yearDays,
  solarToLunar,
  solarToLunarFromStr,
  lunarToSolar,
  lunarToSolarStr,
  getTianGan,
  getDiZhi,
  getGanZhi,
  getShengXiao,
  getMonthName,
  getDayName,
} from '../dist/esm/index.js';

describe('数据表完整性', () => {
  it('应覆盖 1900-2100（201 年）', () => {
    assert.equal(LUNAR_START_YEAR, 1900);
    assert.equal(LUNAR_END_YEAR, 2100);
  });

  it('每年总天数应在 353-385 之间', () => {
    for (let y = LUNAR_START_YEAR; y <= LUNAR_END_YEAR; y++) {
      const days = yearDays(y);
      assert.ok(days >= 353 && days <= 385, `${y} 年天数 ${days} 超出合理范围 [353, 385]`);
    }
  });

  it('每月天数应为 29 或 30', () => {
    for (let y = LUNAR_START_YEAR; y <= LUNAR_END_YEAR; y++) {
      for (let m = 1; m <= 12; m++) {
        const days = monthDays(y, m);
        assert.ok(days === 29 || days === 30, `${y}-${m} 月天数 ${days} 不是 29 或 30`);
      }
    }
  });
});

describe('闰月', () => {
  it('闰月月份应在 0-12 之间', () => {
    for (let y = LUNAR_START_YEAR; y <= LUNAR_END_YEAR; y++) {
      const lm = leapMonth(y);
      assert.ok(lm >= 0 && lm <= 12, `${y} 年闰月 ${lm} 超出范围`);
    }
  });

  it('无闰月时闰月天数应为 0', () => {
    for (let y = LUNAR_START_YEAR; y <= LUNAR_END_YEAR; y++) {
      if (leapMonth(y) === 0) {
        assert.equal(leapMonthDays(y), 0, `${y} 年无闰月但闰月天数不为 0`);
      }
    }
  });

  it('有闰月时闰月天数应为 29 或 30', () => {
    for (let y = LUNAR_START_YEAR; y <= LUNAR_END_YEAR; y++) {
      if (leapMonth(y) > 0) {
        const days = leapMonthDays(y);
        assert.ok(days === 29 || days === 30, `${y} 年闰月天数 ${days} 不是 29 或 30`);
      }
    }
  });

  it('2025 年应有闰六月', () => {
    assert.equal(leapMonth(2025), 6);
  });

  it('2023 年应有闰二月', () => {
    assert.equal(leapMonth(2023), 2);
  });

  it('2024 年无闰月', () => {
    assert.equal(leapMonth(2024), 0);
  });
});

describe('公历→农历（已知日期验证）', () => {
  it('2025-01-29 应为乙巳年正月初一', () => {
    const info = solarToLunar(2025, 1, 29);
    assert.equal(info.year, 2025);
    assert.equal(info.month, 1);
    assert.equal(info.day, 1);
    assert.equal(info.isLeapMonth, false);
    assert.equal(info.ganZhiYear, '乙巳年');
    assert.equal(info.shengXiao, '蛇');
    assert.equal(info.monthName, '正月');
    assert.equal(info.dayName, '初一');
  });

  it('2024-02-10 应为甲辰年正月初一（龙年春节）', () => {
    const info = solarToLunar(2024, 2, 10);
    assert.equal(info.year, 2024);
    assert.equal(info.month, 1);
    assert.equal(info.day, 1);
    assert.equal(info.shengXiao, '龙');
  });

  it('2023-01-22 应为癸卯年正月初一（兔年春节）', () => {
    const info = solarToLunar(2023, 1, 22);
    assert.equal(info.year, 2023);
    assert.equal(info.month, 1);
    assert.equal(info.day, 1);
    assert.equal(info.shengXiao, '兔');
  });

  it('solarToLunarFromStr 便捷方法', () => {
    const info = solarToLunarFromStr('2025-01-29');
    assert.equal(info.year, 2025);
    assert.equal(info.month, 1);
    assert.equal(info.day, 1);
  });

  it('2025-09-22 应为正月十五后的中秋', () => {
    // 2025 中秋: 农历八月十五 → 公历 10月6日
    const info = solarToLunar(2025, 10, 6);
    assert.equal(info.month, 8);
    assert.equal(info.day, 15);
  });
});

describe('农历→公历', () => {
  it('2025年正月初一 → 2025-01-29', () => {
    const [y, m, d] = lunarToSolar(2025, 1, 1);
    assert.equal(y, 2025);
    assert.equal(m, 1);
    assert.equal(d, 29);
  });

  it('2024年正月初一 → 2024-02-10', () => {
    const [y, m, d] = lunarToSolar(2024, 1, 1);
    assert.equal(y, 2024);
    assert.equal(m, 2);
    assert.equal(d, 10);
  });

  it('lunarToSolarStr 便捷方法', () => {
    const str = lunarToSolarStr(2025, 1, 1);
    assert.equal(str, '2025-01-29');
  });
});

describe('往返一致性', () => {
  it('solar→lunar→solar 应还原（2000-2050 每年正月初一）', () => {
    for (let year = 2000; year <= 2050; year++) {
      const lunar = solarToLunar(year, 2, 1); // 约在春节附近
      const [sy, sm, sd] = lunarToSolar(lunar.year, lunar.month, lunar.day, lunar.isLeapMonth);
      assert.equal(sy, year, `${year} 年 2月1日往返失败`);
      assert.equal(sm, 2);
      assert.equal(sd, 1);
    }
  });

  it('lunar→solar→lunar 应还原（2020-2030 每年正月初一到十五）', () => {
    for (let year = 2020; year <= 2030; year++) {
      for (let day = 1; day <= 15; day++) {
        const [sy, sm, sd] = lunarToSolar(year, 1, day);
        const back = solarToLunar(sy, sm, sd);
        assert.equal(back.year, year, `lunar ${year}-1-${day} 往返年份不一致`);
        assert.equal(back.month, 1, `lunar ${year}-1-${day} 往返月份不一致`);
        assert.equal(back.day, day, `lunar ${year}-1-${day} 往返日期不一致`);
      }
    }
  });
});

describe('天干地支 / 生肖', () => {
  it('甲子年循环：1984 = 甲子', () => {
    assert.equal(getGanZhi(1984), '甲子');
    assert.equal(getShengXiao(1984), '鼠');
  });

  it('2025 = 乙巳（蛇）', () => {
    assert.equal(getTianGan(2025), '乙');
    assert.equal(getDiZhi(2025), '巳');
    assert.equal(getGanZhi(2025), '乙巳');
    assert.equal(getShengXiao(2025), '蛇');
  });

  it('2024 = 甲辰（龙）', () => {
    assert.equal(getGanZhi(2024), '甲辰');
    assert.equal(getShengXiao(2024), '龙');
  });

  it('60 年一个完整周期', () => {
    const base = getGanZhi(1984); // 甲子
    assert.equal(getGanZhi(1984 + 60), base);
    assert.equal(getGanZhi(1984 + 120), base);
  });
});

describe('月份和日期名称', () => {
  it('正月到腊月', () => {
    assert.equal(getMonthName(1, false), '正月');
    assert.equal(getMonthName(12, false), '腊月');
    assert.equal(getMonthName(4, true), '闰四月');
  });

  it('初一到三十', () => {
    assert.equal(getDayName(1), '初一');
    assert.equal(getDayName(15), '十五');
    assert.equal(getDayName(30), '三十');
  });
});

describe('边界情况', () => {
  it('超出年份范围应抛出 RangeError', () => {
    assert.throws(() => solarToLunar(1899, 1, 1), RangeError);
    assert.throws(() => lunarToSolar(1899, 1, 1), RangeError);
    assert.throws(() => yearDays(1899), RangeError);
    assert.throws(() => yearDays(2101), RangeError);
  });

  it('无效月份应抛出 RangeError', () => {
    assert.throws(() => monthDays(2025, 0), RangeError);
    assert.throws(() => monthDays(2025, 13), RangeError);
    assert.throws(() => lunarToSolar(2025, 0, 1), RangeError);
  });

  it('无效日期应抛出 RangeError', () => {
    assert.throws(() => lunarToSolar(2025, 1, 0), RangeError);
    assert.throws(() => lunarToSolar(2025, 1, 31), RangeError);
  });

  it('无效日期格式应抛出 Error', () => {
    assert.throws(() => solarToLunarFromStr('invalid'), Error);
    assert.throws(() => solarToLunarFromStr('2025/01/29'), Error);
  });

  it('1900-01-31 应为农历 1900 年正月初一', () => {
    const info = solarToLunar(1900, 1, 31);
    assert.equal(info.year, 1900);
    assert.equal(info.month, 1);
    assert.equal(info.day, 1);
  });
});
