import type { SolarTermInfo } from '@holiday/spec';
import { getSolarTerm } from '@holiday/lunar';

type ChineseLocale = 'zh-CN' | 'zh-TW';

const SOLAR_TERM_NAMES_ZH_TW: readonly string[] = [
  '小寒', '大寒', '立春', '雨水', '驚蟄', '春分',
  '清明', '穀雨', '立夏', '小滿', '芒種', '夏至',
  '小暑', '大暑', '立秋', '處暑', '白露', '秋分',
  '寒露', '霜降', '立冬', '小雪', '大雪', '冬至',
];
const SOLAR_TERM_NAMES_ZH_CN: readonly string[] = [
  '小寒', '大寒', '立春', '雨水', '惊蛰', '春分',
  '清明', '谷雨', '立夏', '小满', '芒种', '夏至',
  '小暑', '大暑', '立秋', '处暑', '白露', '秋分',
  '寒露', '霜降', '立冬', '小雪', '大雪', '冬至',
];

const INFOS = new Map<ChineseLocale, ReadonlyArray<SolarTermInfo>>();

function infos(locale: ChineseLocale): ReadonlyArray<SolarTermInfo> {
  let values = INFOS.get(locale);
  if (!values) {
    const names = locale === 'zh-TW'
      ? SOLAR_TERM_NAMES_ZH_TW
      : SOLAR_TERM_NAMES_ZH_CN;
    values = names.map((name, index) => Object.freeze({ index, name }));
    INFOS.set(locale, values);
  }
  return values;
}

/** Decode only the two possible terms in the requested month. */
export function lookupSolarTerm(
  year: number,
  dayIndex: number,
  locale: ChineseLocale = 'zh-CN',
): SolarTermInfo | null {
  if (year < 1901 || year > 2100) return null;
  const date = new Date(Date.UTC(year, 0, dayIndex + 1));
  if (date.getUTCFullYear() !== year) return null;
  const month = date.getUTCMonth() + 1;
  const day = date.getUTCDate();
  const name = getSolarTerm(year, month, day);
  if (!name) return null;
  const first = (month - 1) * 2;
  const index = SOLAR_TERM_NAMES_ZH_CN[first] === name ? first : first + 1;
  return infos(locale)[index];
}
