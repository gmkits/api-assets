import type { DayInfo, Manifest } from '@holiday/spec';

type Rec = Record<string, unknown>;

export interface HolidayApiClientOptions {
  baseUrl: string;
  defaultRegion?: string;
  fetchFn?: typeof fetch;
}

export class HolidayApiClient {
  private readonly baseUrl: string;
  private readonly defaultRegion: string;
  private readonly fetchFn: typeof fetch;

  constructor(opts: HolidayApiClientOptions) {
    this.baseUrl = opts.baseUrl.replace(/\/+$/, '');
    this.defaultRegion = opts.defaultRegion ?? 'CN';
    this.fetchFn = opts.fetchFn ?? globalThis.fetch.bind(globalThis);
  }

  async getDayInfo(date: string, regionCode?: string): Promise<DayInfo> {
    const r = regionCode ?? this.defaultRegion;
    return normalizeDayInfo(await this.json(`/api/v1/day?date=${enc(date)}&regionCode=${enc(r)}`));
  }

  async getRange(from: string, to: string, regionCode?: string): Promise<DayInfo[]> {
    const r = regionCode ?? this.defaultRegion;
    return normalizeDayInfoArray(await this.json(`/api/v1/range?from=${enc(from)}&to=${enc(to)}&regionCode=${enc(r)}`));
  }

  async getYear(year: number, regionCode?: string): Promise<DayInfo[]> {
    const r = regionCode ?? this.defaultRegion;
    return normalizeDayInfoArray(await this.json(`/api/v1/year?year=${enc(String(year))}&regionCode=${enc(r)}`));
  }

  async getManifest(): Promise<Manifest> {
    return this.json('/api/v1/manifest') as Promise<Manifest>;
  }

  async getRegions(): Promise<string[]> {
    const data = await this.json('/api/v1/regions');
    return arr(data, 'regions', (v): v is string => typeof v === 'string');
  }

  async downloadBundle(region: string, year: number): Promise<ArrayBuffer> {
    const res = await this.fetchFn(`${this.baseUrl}/api/v1/bundle/${enc(region)}/${enc(String(year))}`);
    if (!res.ok) throw apiErr(res);
    return res.arrayBuffer();
  }

  private async json(path: string): Promise<unknown> {
    const res = await this.fetchFn(`${this.baseUrl}${path}`);
    if (!res.ok) throw apiErr(res);
    return res.json();
  }
}

// --- 归一化工具 ---

const enc = encodeURIComponent;
const apiErr = (r: Response) => new Error(`API 请求失败: ${r.status} ${r.statusText}`);

function str(rec: Rec, f: string): string {
  const v = rec[f];
  if (typeof v !== 'string') throw fieldErr(f);
  return v;
}

function num(rec: Rec, f: string): number {
  const v = rec[f];
  if (typeof v !== 'number') throw fieldErr(f);
  return v;
}

function bool(rec: Rec, cur: string, legacy: string): boolean {
  const v = rec[cur];
  if (typeof v === 'boolean') return v;
  const lv = rec[legacy];
  if (typeof lv === 'boolean') return lv;
  throw fieldErr(`${cur}/${legacy}`);
}

function arr<T>(val: unknown, name: string, guard: (v: unknown) => v is T): T[] {
  if (!Array.isArray(val) || val.some((v) => !guard(v))) throw fieldErr(name);
  return val;
}

function rec(val: unknown, name: string): Rec {
  if (!val || typeof val !== 'object' || Array.isArray(val)) throw fieldErr(name);
  return val as Rec;
}

function fieldErr(f: string) {
  return new Error(`API 返回的 ${f} 格式错误`);
}

function normalizeDayInfoArray(value: unknown): DayInfo[] {
  if (!Array.isArray(value)) throw fieldErr('DayInfo[]');
  return value.map(normalizeDayInfo);
}

function normalizeDayInfo(value: unknown): DayInfo {
  const r = rec(value, 'DayInfo');
  return {
    date: str(r, 'date'),
    regionCode: str(r, 'regionCode'),
    calendarSystem: readCalSys(r),
    isHoliday: bool(r, 'isHoliday', 'holiday'),
    isWorkday: bool(r, 'isWorkday', 'workday'),
    isWeekend: bool(r, 'isWeekend', 'weekend'),
    isStatutoryHoliday: bool(r, 'isStatutoryHoliday', 'statutoryHoliday'),
    isAdjustedWorkday: bool(r, 'isAdjustedWorkday', 'adjustedWorkday'),
    holidayNames: readNames(r),
    labels: arr(r.labels, 'labels', (v): v is string => typeof v === 'string'),
    sourceVersion: str(r, 'sourceVersion'),
    extensions: normalizeExt(r.extensions),
  };
}

function readCalSys(r: Rec): DayInfo['calendarSystem'] {
  const v = r.calendarSystem;
  if (v === 'GREGORIAN' || v === 'CHINESE_LUNAR') return v;
  throw fieldErr('calendarSystem');
}

function readNames(r: Rec): DayInfo['holidayNames'] {
  const v = rec(r.holidayNames, 'holidayNames');
  const out: DayInfo['holidayNames'] = {};
  for (const [k, names] of Object.entries(v)) {
    out[k] = arr(names, `holidayNames.${k}`, (x): x is string => typeof x === 'string');
  }
  return out;
}

function normalizeExt(value: unknown): Rec {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return {};
  const r = value as Rec;
  const out: Rec = { ...r };
  if (r.lunar !== undefined) {
    const l = rec(r.lunar, 'extensions.lunar');
    out.lunar = {
      year: num(l, 'year'), month: num(l, 'month'), day: num(l, 'day'),
      isLeapMonth: bool(l, 'isLeapMonth', 'leapMonth'),
      ganZhiYear: str(l, 'ganZhiYear'), shengXiao: str(l, 'shengXiao'),
      monthName: str(l, 'monthName'), dayName: str(l, 'dayName'),
    };
  }
  return out;
}
