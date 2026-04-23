import type { DayInfo, Manifest } from '@holiday/spec';

type Rec = Record<string, unknown>;

/**
 * 请求级可选项，所有查询方法都接受。
 */
export interface RequestOptions {
  /** 透传给底层 `fetch` 的取消信号。 */
  signal?: AbortSignal;
  /** 跳过内存缓存（仍然会写回）。 */
  bypassCache?: boolean;
}

export interface RetryPolicy {
  /** 最大重试次数（不含首次请求）。默认 2。 */
  maxRetries?: number;
  /** 指数退避基础延迟（毫秒）。默认 100。 */
  baseDelayMs?: number;
  /** 退避上限（毫秒）。默认 2000。 */
  maxDelayMs?: number;
  /** 哪些 HTTP 状态码触发重试，默认 [502, 503, 504]。 */
  retryStatuses?: ReadonlyArray<number>;
}

export interface CachePolicy {
  /** 缓存条目最大数量；默认 64；<=0 关闭。 */
  maxEntries?: number;
  /** 条目存活时间（毫秒）；默认 60_000。 */
  ttlMs?: number;
}

export interface HolidayApiClientOptions {
  baseUrl: string;
  defaultRegion?: string;
  fetchFn?: typeof fetch;
  /** 默认 5_000 ms；<=0 关闭。 */
  timeoutMs?: number;
  retry?: RetryPolicy;
  cache?: CachePolicy;
}

interface ResolvedRetry {
  maxRetries: number;
  baseDelayMs: number;
  maxDelayMs: number;
  retryStatuses: ReadonlySet<number>;
}

interface CacheEntry {
  expiresAt: number;
  value: unknown;
}

const DEFAULT_RETRY: ResolvedRetry = {
  maxRetries: 2,
  baseDelayMs: 100,
  maxDelayMs: 2000,
  retryStatuses: new Set([502, 503, 504]),
};

const DEFAULT_TIMEOUT_MS = 5000;

/** 服务端返回非 2xx 时抛出，保留状态码与响应体片段。 */
export class HolidayApiError extends Error {
  readonly status: number;
  readonly statusText: string;
  readonly body: string | undefined;
  constructor(status: number, statusText: string, body?: string) {
    super(`API 请求失败: ${status} ${statusText}`);
    this.name = 'HolidayApiError';
    this.status = status;
    this.statusText = statusText;
    this.body = body;
  }
}

export class HolidayApiClient {
  private readonly baseUrl: string;
  private readonly defaultRegion: string;
  private readonly fetchFn: typeof fetch;
  private readonly timeoutMs: number;
  private readonly retry: ResolvedRetry;
  private readonly cacheMaxEntries: number;
  private readonly cacheTtlMs: number;
  private readonly cache: Map<string, CacheEntry>;
  private readonly inflight: Map<string, Promise<unknown>>;

  constructor(opts: HolidayApiClientOptions) {
    this.baseUrl = opts.baseUrl.replace(/\/+$/, '');
    this.defaultRegion = opts.defaultRegion ?? 'CN';
    this.fetchFn = opts.fetchFn ?? globalThis.fetch.bind(globalThis);
    this.timeoutMs = opts.timeoutMs ?? DEFAULT_TIMEOUT_MS;
    this.retry = {
      maxRetries: opts.retry?.maxRetries ?? DEFAULT_RETRY.maxRetries,
      baseDelayMs: opts.retry?.baseDelayMs ?? DEFAULT_RETRY.baseDelayMs,
      maxDelayMs: opts.retry?.maxDelayMs ?? DEFAULT_RETRY.maxDelayMs,
      retryStatuses: new Set(opts.retry?.retryStatuses ?? DEFAULT_RETRY.retryStatuses),
    };
    this.cacheMaxEntries = opts.cache?.maxEntries ?? 64;
    this.cacheTtlMs = opts.cache?.ttlMs ?? 60_000;
    this.cache = new Map();
    this.inflight = new Map();
  }

  /** 清空内存缓存（不影响进行中的请求）。 */
  clearCache(): void {
    this.cache.clear();
  }

  async getDayInfo(date: string, regionCode?: string, options?: RequestOptions): Promise<DayInfo> {
    const r = regionCode ?? this.defaultRegion;
    return normalizeDayInfo(
      await this.json(`/api/v1/day?date=${enc(date)}&regionCode=${enc(r)}`, options),
    );
  }

  async getRange(
    from: string,
    to: string,
    regionCode?: string,
    options?: RequestOptions,
  ): Promise<DayInfo[]> {
    const r = regionCode ?? this.defaultRegion;
    return normalizeDayInfoArray(
      await this.json(`/api/v1/range?from=${enc(from)}&to=${enc(to)}&regionCode=${enc(r)}`, options),
    );
  }

  async getYear(year: number, regionCode?: string, options?: RequestOptions): Promise<DayInfo[]> {
    const r = regionCode ?? this.defaultRegion;
    return normalizeDayInfoArray(
      await this.json(`/api/v1/year?year=${enc(String(year))}&regionCode=${enc(r)}`, options),
    );
  }

  async getManifest(options?: RequestOptions): Promise<Manifest> {
    return (await this.json('/api/v1/manifest', options)) as Manifest;
  }

  async getRegions(options?: RequestOptions): Promise<string[]> {
    const data = await this.json('/api/v1/regions', options);
    return arr(data, 'regions', (v): v is string => typeof v === 'string');
  }

  async downloadBundle(region: string, year: number, options?: RequestOptions): Promise<ArrayBuffer> {
    const res = await this.executeFetch(
      `${this.baseUrl}/api/v1/bundle/${enc(region)}/${enc(String(year))}`,
      options?.signal,
    );
    if (!res.ok) throw await apiErr(res);
    return res.arrayBuffer();
  }

  private async json(path: string, options?: RequestOptions): Promise<unknown> {
    const cacheKey = `GET ${path}`;
    if (!options?.bypassCache && this.cacheTtlMs > 0 && this.cacheMaxEntries > 0) {
      const cached = this.readCache(cacheKey);
      if (cached !== undefined) return cached;
    }
    // In-flight dedup: identical concurrent requests share one Promise.
    const existing = this.inflight.get(cacheKey);
    if (existing) return existing;
    const promise = this.fetchJson(`${this.baseUrl}${path}`, options?.signal)
      .then((value) => {
        if (this.cacheTtlMs > 0 && this.cacheMaxEntries > 0) {
          this.writeCache(cacheKey, value);
        }
        return value;
      })
      .finally(() => {
        this.inflight.delete(cacheKey);
      });
    this.inflight.set(cacheKey, promise);
    return promise;
  }

  private async fetchJson(url: string, signal: AbortSignal | undefined): Promise<unknown> {
    const res = await this.executeFetch(url, signal);
    if (!res.ok) throw await apiErr(res);
    return res.json();
  }

  /** Execute fetch with timeout + retry + abort propagation. */
  private async executeFetch(url: string, externalSignal: AbortSignal | undefined): Promise<Response> {
    let attempt = 0;
    let lastError: unknown;
    // eslint-disable-next-line no-constant-condition
    while (true) {
      const { signal, cancel } = composeSignal(externalSignal, this.timeoutMs);
      try {
        const res = await this.fetchFn(url, { signal });
        if (!res.ok && this.retry.retryStatuses.has(res.status) && attempt < this.retry.maxRetries) {
          attempt += 1;
          await delay(this.backoffDelay(attempt), externalSignal);
          continue;
        }
        return res;
      } catch (err) {
        // Abort caused by external signal: rethrow immediately.
        if (externalSignal?.aborted) throw err;
        lastError = err;
        if (attempt >= this.retry.maxRetries) throw err;
        attempt += 1;
        await delay(this.backoffDelay(attempt), externalSignal);
      } finally {
        cancel();
      }
    }
    // Unreachable, but appease the type checker.
    // eslint-disable-next-line no-unreachable
    throw lastError;
  }

  private backoffDelay(attempt: number): number {
    const exp = Math.min(this.retry.maxDelayMs, this.retry.baseDelayMs * 2 ** (attempt - 1));
    // Full jitter
    return Math.floor(Math.random() * exp);
  }

  private readCache(key: string): unknown | undefined {
    const entry = this.cache.get(key);
    if (!entry) return undefined;
    if (entry.expiresAt < Date.now()) {
      this.cache.delete(key);
      return undefined;
    }
    // LRU promotion
    this.cache.delete(key);
    this.cache.set(key, entry);
    return entry.value;
  }

  private writeCache(key: string, value: unknown): void {
    if (this.cache.has(key)) this.cache.delete(key);
    this.cache.set(key, { expiresAt: Date.now() + this.cacheTtlMs, value });
    while (this.cache.size > this.cacheMaxEntries) {
      const oldest = this.cache.keys().next().value;
      if (oldest === undefined) break;
      this.cache.delete(oldest);
    }
  }
}

function composeSignal(
  external: AbortSignal | undefined,
  timeoutMs: number,
): { signal: AbortSignal; cancel: () => void } {
  if (timeoutMs <= 0 && !external) {
    const ac = new AbortController();
    return { signal: ac.signal, cancel: () => undefined };
  }
  const ac = new AbortController();
  let timer: ReturnType<typeof setTimeout> | undefined;
  const onAbort = () => {
    ac.abort((external as AbortSignal & { reason?: unknown })?.reason);
  };
  if (external) {
    if (external.aborted) ac.abort((external as AbortSignal & { reason?: unknown }).reason);
    else external.addEventListener('abort', onAbort, { once: true });
  }
  if (timeoutMs > 0) {
    timer = setTimeout(() => ac.abort(new Error(`请求超时（${timeoutMs}ms）`)), timeoutMs);
  }
  return {
    signal: ac.signal,
    cancel: () => {
      if (timer) clearTimeout(timer);
      external?.removeEventListener('abort', onAbort);
    },
  };
}

function delay(ms: number, signal: AbortSignal | undefined): Promise<void> {
  return new Promise((resolve, reject) => {
    if (signal?.aborted) {
      reject(signal.reason ?? new Error('aborted'));
      return;
    }
    const timer = setTimeout(() => {
      signal?.removeEventListener('abort', onAbort);
      resolve();
    }, ms);
    const onAbort = () => {
      clearTimeout(timer);
      reject(signal?.reason ?? new Error('aborted'));
    };
    signal?.addEventListener('abort', onAbort, { once: true });
  });
}

// --- 归一化工具 ---

const enc = encodeURIComponent;
async function apiErr(r: Response): Promise<HolidayApiError> {
  let body: string | undefined;
  try {
    body = await r.text();
  } catch {
    body = undefined;
  }
  return new HolidayApiError(r.status, r.statusText, body);
}

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
