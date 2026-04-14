import { ref, provide, inject, type InjectionKey, type Ref } from 'vue';
import type { DayInfo } from '@holiday/spec';
import { createHolidayService, type HolidayService, type HolidayServiceOptions } from '@holiday/core';

/**
 * Injection return type for the holiday service composable.
 */
export interface HolidayServiceInjection {
  /** The underlying HolidayService instance. */
  service: HolidayService;
  /** Get full day information for a single date. */
  getDayInfo: (date: string, regionCode?: string) => Promise<DayInfo | null>;
  /** Check whether a date is a holiday (day off). */
  isHoliday: (date: string, regionCode?: string) => Promise<boolean>;
  /** Check whether a date is a working day. */
  isWorkday: (date: string, regionCode?: string) => Promise<boolean>;
  /** Reactive loading state. */
  loading: Ref<boolean>;
}

/** Symbol key for provide/inject. */
const HOLIDAY_SERVICE_KEY: InjectionKey<HolidayServiceInjection> =
  Symbol('HolidayService');

/**
 * Vue 3 composable that creates and provides a {@link HolidayService}.
 *
 * Call in a parent component to make the service available to all descendants
 * via `inject`. Also returns the injection value directly.
 *
 * @param options - Options forwarded to {@link createHolidayService}.
 */
export function useHolidayService(
  options?: HolidayServiceOptions,
): HolidayServiceInjection {
  // Check if already provided by an ancestor
  const existing = inject(HOLIDAY_SERVICE_KEY, null);
  if (existing) {
    return existing;
  }

  const service = createHolidayService(options);
  const loading = ref(false);

  const getDayInfo = async (
    date: string,
    regionCode?: string,
  ): Promise<DayInfo | null> => {
    loading.value = true;
    try {
      return await service.getDayInfo(date, regionCode);
    } finally {
      loading.value = false;
    }
  };

  const isHoliday = async (
    date: string,
    regionCode?: string,
  ): Promise<boolean> => {
    loading.value = true;
    try {
      return await service.isHoliday(date, regionCode);
    } finally {
      loading.value = false;
    }
  };

  const isWorkday = async (
    date: string,
    regionCode?: string,
  ): Promise<boolean> => {
    loading.value = true;
    try {
      return await service.isWorkday(date, regionCode);
    } finally {
      loading.value = false;
    }
  };

  const injection: HolidayServiceInjection = {
    service,
    getDayInfo,
    isHoliday,
    isWorkday,
    loading,
  };

  provide(HOLIDAY_SERVICE_KEY, injection);

  return injection;
}
