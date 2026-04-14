/**
 * @holiday/core — Main Entry Point
 *
 * Re-exports the public API surface of the `@holiday/core` package:
 *
 * - {@link createHolidayService} — factory function for the SDK service
 * - {@link HolidayService} / {@link HolidayServiceOptions} — service types
 * - {@link parseHdayBundle} — low-level `.hday` binary parser
 * - {@link HdayBundle}, {@link HdayHeader}, etc. — parser result types
 * - Query-engine utilities and helpers
 * - {@link LRUCache} — generic LRU cache
 *
 * @module
 */

// Holiday service (primary API)
export {
  createHolidayService,
} from './holiday-service.js';

export type {
  HolidayService,
  HolidayServiceOptions,
} from './holiday-service.js';

// Binary parser
export {
  parseHdayBundle,
} from './hday-parser.js';

export type {
  DayEntry,
  HdayBundle,
  HdayHeader,
  NameListEntry,
} from './hday-parser.js';

// Query engine
export {
  dayEntryToDayInfo,
  dayOfYear,
  formatDate,
  isLeapYear,
  monthDayFromIndex,
  parseDate,
  queryDay,
  queryYear,
  resolveLabels,
  resolveNames,
} from './query-engine.js';

// LRU cache
export { LRUCache } from './lru-cache.js';
