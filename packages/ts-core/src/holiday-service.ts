/**
 * @holiday/core — Holiday Service
 *
 * Implements the {@link HolidayService} interface that is the main entry
 * point for SDK consumers.  The service manages:
 *
 * - An LRU cache of parsed {@link HdayBundle} objects
 * - Loading bundles from pre-loaded buffers, the file system (Node.js), or
 *   HTTP (browser / `fetch`)
 * - Delegating date queries to the {@link queryDay} / {@link queryYear}
 *   functions from the query engine
 *
 * @module
 */

import type { DayInfo } from '@holiday/spec';

import type { HdayBundle } from './hday-parser.js';
import { parseHdayBundle } from './hday-parser.js';
import { LRUCache } from './lru-cache.js';
import {
  dayOfYear,
  formatDate,
  monthDayFromIndex,
  parseDate,
  queryDay,
  queryYear,
} from './query-engine.js';

// ---------------------------------------------------------------------------
// Public types
// ---------------------------------------------------------------------------

/**
 * Options for creating a {@link HolidayService} instance.
 */
export interface HolidayServiceOptions {
  /** Default region code used when callers omit it (e.g. `"CN"`). */
  defaultRegion?: string;

  /**
   * Path to the data directory containing `.hday` bundles.
   *
   * - **Node.js**: an absolute or relative file-system path
   *   (e.g. `"./data/bundles"`).
   * - **Browser**: a base URL (e.g. `"https://cdn.example.com/bundles"`).
   *
   * The service appends `/{regionCode}/{year}.hday` to this path.
   */
  dataPath?: string;

  /**
   * Pre-loaded bundles provided at construction time.
   *
   * Keys use the format `"{regionCode}-{year}"` (e.g. `"CN-2026"`).
   * Values are raw `.hday` file bytes as `ArrayBuffer`.
   */
  preloadedBundles?: Map<string, ArrayBuffer>;

  /**
   * Maximum number of parsed bundles held in the LRU cache.
   *
   * @defaultValue 32
   */
  maxCacheSize?: number;
}

/**
 * The primary SDK interface for querying holiday data.
 *
 * All date parameters use ISO 8601 format (`YYYY-MM-DD`).
 */
export interface HolidayService {
  /**
   * Get full day information for a single date.
   *
   * @param date       - Date in `YYYY-MM-DD` format.
   * @param regionCode - Optional region override.
   * @returns The {@link DayInfo} DTO, or `null` if no data is available.
   */
  getDayInfo(date: string, regionCode?: string): Promise<DayInfo | null>;

  /**
   * Check whether a date is a holiday (day off).
   *
   * @param date       - Date in `YYYY-MM-DD` format.
   * @param regionCode - Optional region override.
   */
  isHoliday(date: string, regionCode?: string): Promise<boolean>;

  /**
   * Check whether a date is a working day.
   *
   * @param date       - Date in `YYYY-MM-DD` format.
   * @param regionCode - Optional region override.
   */
  isWorkday(date: string, regionCode?: string): Promise<boolean>;

  /**
   * Query day information for every day in a date range (inclusive).
   *
   * @param from       - Start date in `YYYY-MM-DD` format.
   * @param to         - End date in `YYYY-MM-DD` format.
   * @param regionCode - Optional region override.
   * @returns Array of {@link DayInfo} for each day in the range.
   */
  getRange(
    from: string,
    to: string,
    regionCode?: string,
  ): Promise<DayInfo[]>;

  /**
   * Query day information for every day of a given year.
   *
   * @param year       - Calendar year (e.g. 2026).
   * @param regionCode - Optional region override.
   * @returns Array of {@link DayInfo} for the full year.
   */
  getYear(year: number, regionCode?: string): Promise<DayInfo[]>;
}

// ---------------------------------------------------------------------------
// Implementation
// ---------------------------------------------------------------------------

/** Default LRU cache size. */
const DEFAULT_CACHE_SIZE = 32;

/**
 * Compute a cache key for a region + year combination.
 *
 * @param region - Region code (e.g. `"CN"`).
 * @param year   - Calendar year.
 * @returns Cache key string.
 */
function cacheKey(region: string, year: number): string {
  return `${region}-${year}`;
}

/**
 * Advance a Gregorian date by one day and return `[year, month, day]`.
 *
 * @param year  - Calendar year.
 * @param month - Month (1–12).
 * @param day   - Day of month.
 * @returns The next day as `[year, month, day]`.
 */
function nextDay(
  year: number,
  month: number,
  day: number,
): [number, number, number] {
  // Use Date to handle month/year rollovers correctly
  const d = new Date(Date.UTC(year, month - 1, day + 1));
  return [d.getUTCFullYear(), d.getUTCMonth() + 1, d.getUTCDate()];
}

/**
 * Concrete implementation of {@link HolidayService}.
 */
class HolidayServiceImpl implements HolidayService {
  private readonly defaultRegion: string;
  private readonly dataPath: string | undefined;
  private readonly preloaded: Map<string, ArrayBuffer>;
  private readonly cache: LRUCache<string, HdayBundle>;

  constructor(options: HolidayServiceOptions = {}) {
    this.defaultRegion = options.defaultRegion ?? 'CN';
    this.dataPath = options.dataPath;
    this.preloaded = options.preloadedBundles ?? new Map();
    this.cache = new LRUCache<string, HdayBundle>(
      options.maxCacheSize ?? DEFAULT_CACHE_SIZE,
    );
  }

  // -----------------------------------------------------------------------
  // Bundle loading
  // -----------------------------------------------------------------------

  /**
   * Obtain a parsed bundle for the given region and year. Checks, in order:
   *
   * 1. The LRU cache of already-parsed bundles
   * 2. The `preloadedBundles` map (raw ArrayBuffers)
   * 3. The file system (Node.js) or HTTP fetch (browser)
   *
   * @param region - Region code.
   * @param year   - Calendar year.
   * @returns A parsed {@link HdayBundle}.
   * @throws {Error} If the bundle cannot be found or loaded.
   */
  private async getBundle(region: string, year: number): Promise<HdayBundle> {
    const key = cacheKey(region, year);

    // 1. LRU cache hit
    const cached = this.cache.get(key);
    if (cached) {
      return cached;
    }

    // 2. Pre-loaded raw buffer
    const preloaded = this.preloaded.get(key);
    if (preloaded) {
      const bundle = parseHdayBundle(preloaded);
      this.cache.set(key, bundle);
      return bundle;
    }

    // 3. Load from dataPath
    if (!this.dataPath) {
      throw new Error(
        `No bundle available for ${key}: no preloaded data and no dataPath configured`,
      );
    }

    const buffer = await this.loadFromDataPath(region, year);
    const bundle = parseHdayBundle(buffer);
    this.cache.set(key, bundle);
    return bundle;
  }

  /**
   * Load a raw `.hday` file from `dataPath`.
   *
   * Attempts Node.js `fs.readFile` first (via dynamic import to avoid
   * breaking browser bundles), then falls back to `fetch`.
   *
   * @param region - Region code.
   * @param year   - Calendar year.
   * @returns The raw file bytes as an `ArrayBuffer`.
   */
  private async loadFromDataPath(
    region: string,
    year: number,
  ): Promise<ArrayBuffer> {
    const filePath = `${this.dataPath}/${region}/${year}.hday`;

    // Try Node.js fs first (dynamic import avoids bundler issues).
    // The module name is assigned to a variable so that TypeScript does not
    // attempt to resolve it at compile time (no @types/node dependency).
    try {
      const fsModule = 'fs';
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const fs: any = await import(/* webpackIgnore: true */ fsModule);
      const nodeBuffer: { buffer: ArrayBuffer; byteOffset: number; byteLength: number } =
        await fs.promises.readFile(filePath);
      // Convert Node Buffer to ArrayBuffer
      return nodeBuffer.buffer.slice(
        nodeBuffer.byteOffset,
        nodeBuffer.byteOffset + nodeBuffer.byteLength,
      );
    } catch {
      // fs import failed (browser) or file not found — try fetch
    }

    // Fallback to fetch (browser or Deno)
    if (typeof fetch === 'function') {
      const response = await fetch(filePath);
      if (!response.ok) {
        throw new Error(
          `Failed to fetch bundle ${filePath}: HTTP ${response.status}`,
        );
      }
      return response.arrayBuffer();
    }

    throw new Error(
      `Cannot load bundle ${filePath}: neither fs nor fetch is available`,
    );
  }

  // -----------------------------------------------------------------------
  // Public API
  // -----------------------------------------------------------------------

  /** @inheritdoc */
  async getDayInfo(
    date: string,
    regionCode?: string,
  ): Promise<DayInfo | null> {
    const region = regionCode ?? this.defaultRegion;
    const [year] = parseDate(date);
    const bundle = await this.getBundle(region, year);
    return queryDay(bundle, date);
  }

  /** @inheritdoc */
  async isHoliday(date: string, regionCode?: string): Promise<boolean> {
    const info = await this.getDayInfo(date, regionCode);
    return info?.isHoliday ?? false;
  }

  /** @inheritdoc */
  async isWorkday(date: string, regionCode?: string): Promise<boolean> {
    const info = await this.getDayInfo(date, regionCode);
    return info?.isWorkday ?? false;
  }

  /** @inheritdoc */
  async getRange(
    from: string,
    to: string,
    regionCode?: string,
  ): Promise<DayInfo[]> {
    const results: DayInfo[] = [];
    let [year, month, day] = parseDate(from);
    const [toYear, toMonth, toDay] = parseDate(to);

    // Walk from `from` to `to` inclusive, day by day
    while (
      year < toYear ||
      (year === toYear && month < toMonth) ||
      (year === toYear && month === toMonth && day <= toDay)
    ) {
      const dateStr = formatDate(year, month, day);
      const info = await this.getDayInfo(dateStr, regionCode);
      if (info) {
        results.push(info);
      }
      [year, month, day] = nextDay(year, month, day);
    }

    return results;
  }

  /** @inheritdoc */
  async getYear(year: number, regionCode?: string): Promise<DayInfo[]> {
    const region = regionCode ?? this.defaultRegion;
    const bundle = await this.getBundle(region, year);
    return queryYear(bundle);
  }
}

// ---------------------------------------------------------------------------
// Factory
// ---------------------------------------------------------------------------

/**
 * Create a new {@link HolidayService} instance.
 *
 * @param options - Configuration options.
 * @returns A ready-to-use service instance.
 *
 * @example
 * ```ts
 * import { createHolidayService } from '@holiday/core';
 *
 * const svc = createHolidayService({
 *   defaultRegion: 'CN',
 *   dataPath: './data/bundles',
 * });
 *
 * const info = await svc.getDayInfo('2026-01-01');
 * console.log(info?.isHoliday); // true
 * ```
 */
export function createHolidayService(
  options?: HolidayServiceOptions,
): HolidayService {
  return new HolidayServiceImpl(options);
}
