import type { DayInfo, FestivalInfo, LunarDateInfo, SolarTermInfo } from '@holiday/spec';
import { solarToLunar } from '@holiday/lunar';

const DEFINITIONS: Record<string, FestivalInfo> = {};

add('NEW_YEAR', '元旦', "New Year's Day");
add('SPRING_FESTIVAL', '春节', 'Spring Festival');
add('LUNAR_NEW_YEARS_EVE', '除夕', "Lunar New Year's Eve");
add('LANTERN_FESTIVAL', '元宵节', 'Lantern Festival');
add('DRAGON_HEADS_RAISING', '龙抬头', 'Dragon Heads-raising Day');
add('TOMB_SWEEPING', '清明节', 'Tomb-Sweeping Day');
add('LABOUR_DAY', '劳动节', 'Labour Day');
add('DRAGON_BOAT', '端午节', 'Dragon Boat Festival');
add('QIXI', '七夕节', 'Qixi Festival');
add('GHOST_FESTIVAL', '中元节', 'Ghost Festival');
add('MID_AUTUMN', '中秋节', 'Mid-Autumn Festival');
add('DOUBLE_NINTH', '重阳节', 'Double Ninth Festival');
add('LABA', '腊八节', 'Laba Festival');
add('NATIONAL_DAY', '国庆节', 'National Day');
add('WOMENS_DAY', '妇女节', "International Women's Day");
add('ARBOR_DAY', '植树节', 'Arbor Day');
add('YOUTH_DAY', '青年节', 'Youth Day');
add('CHILDRENS_DAY', '儿童节', "Children's Day");
add('CPC_FOUNDING_DAY', '建党节', 'CPC Founding Day');
add('ARMY_DAY', '建军节', 'PLA Day');
add('VICTORY_DAY', '中国人民抗日战争胜利纪念日', 'Victory Day');
add('TEACHERS_DAY', '教师节', "Teachers' Day");
add('NATIONAL_MEMORIAL_DAY', '南京大屠杀死难者国家公祭日', 'National Memorial Day');

/**
 * 将同一天的公历、农历与节气信息组合为节日列表。
 *
 * 节日列表不代表当天放假；实际状态仍以 `isWorkday` 为准。
 */
export function resolveFestivals(
  date: string,
  extensions: DayInfo['extensions'],
): FestivalInfo[] {
  const [year, month, day] = date.split('-').map(Number);
  const result: FestivalInfo[] = [];
  const fixed = fixedFestival(month * 100 + day);
  if (fixed) result.push(DEFINITIONS[fixed]);

  const solarTerm = extensions.solarTerm as SolarTermInfo | undefined;
  if (solarTerm?.name === '清明') result.push(DEFINITIONS.TOMB_SWEEPING);

  const lunar = extensions.lunar as LunarDateInfo | undefined;
  if (lunar && !lunar.isLeapMonth) {
    const lunarCode = lunarFestival(lunar.month * 100 + lunar.day);
    if (lunarCode) result.push(DEFINITIONS[lunarCode]);
    if (lunar.month === 12 && lunar.day >= 29 && isLunarNewYearsEve(year, month, day)) {
      result.push(DEFINITIONS.LUNAR_NEW_YEARS_EVE);
    }
  }
  return result;
}

function fixedFestival(key: number): string | undefined {
  return ({
    101: 'NEW_YEAR',
    308: 'WOMENS_DAY',
    312: 'ARBOR_DAY',
    501: 'LABOUR_DAY',
    504: 'YOUTH_DAY',
    601: 'CHILDRENS_DAY',
    701: 'CPC_FOUNDING_DAY',
    801: 'ARMY_DAY',
    903: 'VICTORY_DAY',
    910: 'TEACHERS_DAY',
    1001: 'NATIONAL_DAY',
    1213: 'NATIONAL_MEMORIAL_DAY',
  } as Record<number, string>)[key];
}

function lunarFestival(key: number): string | undefined {
  return ({
    101: 'SPRING_FESTIVAL',
    115: 'LANTERN_FESTIVAL',
    202: 'DRAGON_HEADS_RAISING',
    505: 'DRAGON_BOAT',
    707: 'QIXI',
    715: 'GHOST_FESTIVAL',
    815: 'MID_AUTUMN',
    909: 'DOUBLE_NINTH',
    1208: 'LABA',
  } as Record<number, string>)[key];
}

function isLunarNewYearsEve(year: number, month: number, day: number): boolean {
  try {
    const tomorrow = new Date(Date.UTC(year, month - 1, day + 1));
    const lunar = solarToLunar(
      tomorrow.getUTCFullYear(),
      tomorrow.getUTCMonth() + 1,
      tomorrow.getUTCDate(),
    );
    return !lunar.isLeapMonth && lunar.month === 1 && lunar.day === 1;
  } catch (error) {
    if (error instanceof RangeError) return false;
    throw error;
  }
}

function add(code: string, zh: string, en: string): void {
  DEFINITIONS[code] = {
    code,
    names: {
      'zh-CN': zh,
      'en-US': en,
    },
  };
}
