import type { DayInfo, Manifest } from '@holiday/spec';

/**
 * Configuration options for {@link HolidayApiClient}.
 */
export interface HolidayApiClientOptions {
  /** Base URL of the Holiday API (e.g. "https://api.example.com"). */
  baseUrl: string;
  /** Default region code used when callers omit it (e.g. "CN"). */
  defaultRegion?: string;
  /** Custom fetch implementation. Defaults to `globalThis.fetch`. */
  fetchFn?: typeof fetch;
}

/**
 * Fetch-based HTTP client for the Holiday API.
 */
export class HolidayApiClient {
  private readonly baseUrl: string;
  private readonly defaultRegion: string;
  private readonly fetchFn: typeof fetch;

  constructor(options: HolidayApiClientOptions) {
    this.baseUrl = options.baseUrl.replace(/\/+$/, '');
    this.defaultRegion = options.defaultRegion ?? 'CN';
    this.fetchFn = options.fetchFn ?? globalThis.fetch.bind(globalThis);
  }

  /**
   * Get day information for a single date.
   *
   * @param date       - Date in YYYY-MM-DD format.
   * @param regionCode - Optional region override.
   */
  async getDayInfo(date: string, regionCode?: string): Promise<DayInfo> {
    const region = regionCode ?? this.defaultRegion;
    const url = `${this.baseUrl}/api/v1/day?date=${encodeURIComponent(date)}&region=${encodeURIComponent(region)}`;
    return this.fetchJson<DayInfo>(url);
  }

  /**
   * Get day information for a date range (inclusive).
   *
   * @param from       - Start date in YYYY-MM-DD format.
   * @param to         - End date in YYYY-MM-DD format.
   * @param regionCode - Optional region override.
   */
  async getRange(from: string, to: string, regionCode?: string): Promise<DayInfo[]> {
    const region = regionCode ?? this.defaultRegion;
    const url = `${this.baseUrl}/api/v1/range?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}&region=${encodeURIComponent(region)}`;
    return this.fetchJson<DayInfo[]>(url);
  }

  /**
   * Get day information for every day of a given year.
   *
   * @param year       - Calendar year (e.g. 2026).
   * @param regionCode - Optional region override.
   */
  async getYear(year: number, regionCode?: string): Promise<DayInfo[]> {
    const region = regionCode ?? this.defaultRegion;
    const url = `${this.baseUrl}/api/v1/year?year=${encodeURIComponent(String(year))}&region=${encodeURIComponent(region)}`;
    return this.fetchJson<DayInfo[]>(url);
  }

  /**
   * Get the data manifest.
   */
  async getManifest(): Promise<Manifest> {
    const url = `${this.baseUrl}/api/v1/manifest`;
    return this.fetchJson<Manifest>(url);
  }

  /**
   * Get the list of available region codes.
   */
  async getRegions(): Promise<string[]> {
    const url = `${this.baseUrl}/api/v1/regions`;
    return this.fetchJson<string[]>(url);
  }

  /**
   * Download a binary .hday bundle for a specific region and year.
   *
   * @param region - Region code (e.g. "CN").
   * @param year   - Calendar year (e.g. 2026).
   */
  async downloadBundle(region: string, year: number): Promise<ArrayBuffer> {
    const url = `${this.baseUrl}/api/v1/bundle?region=${encodeURIComponent(region)}&year=${encodeURIComponent(String(year))}`;
    const response = await this.fetchFn(url);
    if (!response.ok) {
        throw new Error(`节假日 API 请求失败: ${response.status} ${response.statusText}`);
    }
    return response.arrayBuffer();
  }

  private async fetchJson<T>(url: string): Promise<T> {
    const response = await this.fetchFn(url);
    if (!response.ok) {
        throw new Error(`节假日 API 请求失败: ${response.status} ${response.statusText}`);
    }
    return response.json() as Promise<T>;
  }
}
