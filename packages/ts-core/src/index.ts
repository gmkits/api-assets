/**
 * @holiday/core —— 包入口
 *
 * 对外导出 holiday 查询 SDK 的主要能力：服务工厂、二进制解析器、
 * 查询引擎工具以及通用 LRU 缓存。
 */

// Holiday service（主查询 API）
export {
  createHolidayService,
} from './holiday-service.js';

export type {
  HolidayService,
  HolidayServiceOptions,
} from './holiday-service.js';

// Binary parser（二进制解析）
export {
  parseHdayBundle,
} from './hday-parser.js';

export type {
  DayEntry,
  HdayBundle,
  HdayHeader,
  NameListEntry,
} from './hday-parser.js';

// Query engine（查询引擎）
export {
  dayEntryToDayInfo,
  dayOfYear,
  formatDate,
  isLeapYear,
  monthDayFromIndex,
  parseDate,
  queryDay,
  queryRange,
  queryYear,
  resolveLabels,
  resolveNames,
} from './query-engine.js';

// LRU cache
export { LRUCache } from './lru-cache.js';
