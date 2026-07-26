/**
 * @holiday/core —— Holiday Service
 *
 * 作为 SDK 的主入口，负责 bundle 加载、解析缓存、并发去重和查询编排。
 */

import type { DayInfo } from '@holiday/spec';

import type { HdayBundle } from './hday-parser.js';
import { parseHdayBundle } from './hday-parser.js';
import { LRUCache } from './lru-cache.js';
import {
  countBundleWorkdays,
  dayOfYear,
  findBundleStatutoryHoliday,
  parseDate,
  queryDay,
  queryRange,
  queryYear,
} from './query-engine.js';

/**
 * 创建 `HolidayService` 时可传入的参数。
 */
export interface HolidayServiceOptions {
  /** 默认地区代码，例如 `CN`。 */
  defaultRegion?: string;
  /**
   * 数据目录。
   *
   * - Node.js 下可为文件系统路径
   * - 浏览器下可为静态资源基础 URL
   */
  dataPath?: string;
  /** 预加载 bundle，key 形如 `CN-2026`。 */
  preloadedBundles?: Map<string, ArrayBuffer>;
  /** 已解析 bundle 的 LRU 缓存上限。 */
  maxCacheSize?: number;
}

/**
 * 对外查询接口。
 */
export interface HolidayService {
  getDayInfo(date: string, regionCode?: string): Promise<DayInfo | null>;
  isHoliday(date: string, regionCode?: string): Promise<boolean>;
  isWorkday(date: string, regionCode?: string): Promise<boolean>;
  getRange(from: string, to: string, regionCode?: string): Promise<DayInfo[]>;
  getYear(year: number, regionCode?: string): Promise<DayInfo[]>;
  /** 查询指定月的所有日期信息。 */
  getMonth(year: number, month: number, regionCode?: string): Promise<DayInfo[]>;
  /** 统计闭区间内的工作日天数。 */
  countWorkdays(from: string, to: string, regionCode?: string): Promise<number>;
  /** 从指定日期（含）起查找下一个假期。 */
  getNextHoliday(from: string, regionCode?: string): Promise<DayInfo | null>;
}

const DEFAULT_CACHE_SIZE = 32;

function cacheKey(region: string, year: number): string {
  return `${region}-${year}`;
}

class HolidayServiceImpl implements HolidayService {
  private readonly defaultRegion: string;
  private readonly dataPath: string | undefined;
  private readonly preloaded: Map<string, ArrayBuffer>;
  private readonly cache: LRUCache<string, HdayBundle>;
  private readonly loadingBundles: Map<string, Promise<HdayBundle>>;

  constructor(options: HolidayServiceOptions = {}) {
    this.defaultRegion = options.defaultRegion ?? 'CN';
    this.dataPath = options.dataPath;
    this.preloaded = options.preloadedBundles ?? new Map();
    this.cache = new LRUCache<string, HdayBundle>(
      options.maxCacheSize ?? DEFAULT_CACHE_SIZE,
    );
    this.loadingBundles = new Map();
  }

  /**
   * 读取并解析指定地区/年份的 bundle。
   */
  private async getBundle(region: string, year: number): Promise<HdayBundle> {
    const key = cacheKey(region, year);
    const cached = this.cache.get(key);
    if (cached) {
      return cached;
    }

    const inflight = this.loadingBundles.get(key);
    if (inflight) {
      return inflight;
    }

    const loading = this.loadBundle(region, year)
      .then((bundle) => {
        this.cache.set(key, bundle);
        return bundle;
      })
      .finally(() => {
        this.loadingBundles.delete(key);
      });

    this.loadingBundles.set(key, loading);
    return loading;
  }

  private async loadBundle(region: string, year: number): Promise<HdayBundle> {
    const key = cacheKey(region, year);

    const preloaded = this.preloaded.get(key);
    if (preloaded) {
      return parseHdayBundle(preloaded);
    }

    if (!this.dataPath) {
      throw new Error(
          `${key} 的数据包不可用：没有预加载数据且未配置 dataPath`,
      );
    }

    return parseHdayBundle(await this.loadFromDataPath(region, year));
  }

  /**
   * 从文件系统或 HTTP 读取原始 `.hday` 字节。
   */
  private async loadFromDataPath(
    region: string,
    year: number,
  ): Promise<ArrayBuffer> {
    const filePath = `${this.dataPath}/${region}/${year}.hday`;

    try {
      const fsModule = 'fs';
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const fs: any = await import(/* webpackIgnore: true */ fsModule);
      const nodeBuffer: {
        buffer: ArrayBuffer;
        byteOffset: number;
        byteLength: number;
      } = await fs.promises.readFile(filePath);
      return nodeBuffer.buffer.slice(
        nodeBuffer.byteOffset,
        nodeBuffer.byteOffset + nodeBuffer.byteLength,
      );
    } catch {
      // Node.js 文件系统不可用或文件不存在时退回 fetch。
    }

    if (typeof fetch === 'function') {
      const response = await fetch(filePath);
      if (!response.ok) {
        throw new Error(
            `获取数据包失败 ${filePath}: HTTP ${response.status}`,
        );
      }
      return response.arrayBuffer();
    }

    throw new Error(
        `无法加载数据包 ${filePath}：fs 和 fetch 均不可用`,
    );
  }

  async getDayInfo(
    date: string,
    regionCode?: string,
  ): Promise<DayInfo | null> {
    const region = regionCode ?? this.defaultRegion;
    const [year] = parseDate(date);
    const bundle = await this.getBundle(region, year);
    return queryDay(bundle, date);
  }

  async isHoliday(date: string, regionCode?: string): Promise<boolean> {
    const info = await this.getDayInfo(date, regionCode);
    return info?.isHoliday ?? false;
  }

  async isWorkday(date: string, regionCode?: string): Promise<boolean> {
    const info = await this.getDayInfo(date, regionCode);
    return info?.isWorkday ?? false;
  }

  async getRange(
    from: string,
    to: string,
    regionCode?: string,
  ): Promise<DayInfo[]> {
    const region = regionCode ?? this.defaultRegion;
    const [fromYear, fromMonth, fromDay] = parseDate(from);
    const [toYear, toMonth, toDay] = parseDate(to);

    if (from > to) {
      return [];
    }

    const results: DayInfo[] = [];
    for (let year = fromYear; year <= toYear; year++) {
      const bundle = await this.getBundle(region, year);
      const startIndex =
        year === fromYear ? dayOfYear(fromYear, fromMonth, fromDay) : 0;
      const endIndex =
        year === toYear
          ? dayOfYear(toYear, toMonth, toDay)
          : bundle.days.length - 1;
      results.push(...queryRange(bundle, startIndex, endIndex));
    }

    return results;
  }

  async getYear(year: number, regionCode?: string): Promise<DayInfo[]> {
    const region = regionCode ?? this.defaultRegion;
    const bundle = await this.getBundle(region, year);
    return queryYear(bundle);
  }

  async getMonth(
    year: number,
    month: number,
    regionCode?: string,
  ): Promise<DayInfo[]> {
    const region = regionCode ?? this.defaultRegion;
    const bundle = await this.getBundle(region, year);

    // 计算月份的首日和末日 dayIndex
    const daysInMonth = new Date(year, month, 0).getDate();
    const startIndex = dayOfYear(year, month, 1);
    const endIndex = dayOfYear(year, month, daysInMonth);

    return queryRange(bundle, startIndex, endIndex);
  }

  async countWorkdays(
    from: string,
    to: string,
    regionCode?: string,
  ): Promise<number> {
    const region = regionCode ?? this.defaultRegion;
    const [fromYear, fromMonth, fromDay] = parseDate(from);
    const [toYear, toMonth, toDay] = parseDate(to);
    if (from > to) return 0;
    let count = 0;
    for (let year = fromYear; year <= toYear; year++) {
      const bundle = await this.getBundle(region, year);
      const start = year === fromYear ? dayOfYear(year, fromMonth, fromDay) : 0;
      const end = year === toYear
        ? dayOfYear(year, toMonth, toDay)
        : bundle.days.length - 1;
      count += countBundleWorkdays(bundle, start, end);
    }
    return count;
  }

  async getNextHoliday(
    from: string,
    regionCode?: string,
  ): Promise<DayInfo | null> {
    const region = regionCode ?? this.defaultRegion;
    const [year, month, day] = parseDate(from);
    const startIndex = dayOfYear(year, month, day);

    // 先在当年内搜索
    const bundle = await this.getBundle(region, year);
    const current = findBundleStatutoryHoliday(bundle, startIndex);
    if (current) return current;

    // 当年没找到，搜索下一年
    try {
      const nextBundle = await this.getBundle(region, year + 1);
      return findBundleStatutoryHoliday(nextBundle, 0);
    } catch {
      // 下一年没有数据
    }

    return null;
  }
}

/**
 * 创建可复用的 HolidayService 实例。
 */
export function createHolidayService(
  options?: HolidayServiceOptions,
): HolidayService {
  return new HolidayServiceImpl(options);
}
