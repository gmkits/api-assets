import { defineComponent, h, ref, watch } from 'vue';
import { createHolidayService } from '@holiday/core';
import type { DayInfo } from '@holiday/spec';

/**
 * DayBadge — shows a small badge with day status.
 *
 * - 休 for holidays
 * - 班 for adjusted workdays (weekend days that are workdays)
 * - nothing for normal days
 */
export const DayBadge = defineComponent({
  name: 'DayBadge',
  props: {
    date: { type: String, required: true },
    regionCode: { type: String, default: 'CN' },
  },
  setup(props) {
    const dayInfo = ref<DayInfo | null>(null);
    const service = createHolidayService({ defaultRegion: props.regionCode });

    const loadDay = async () => {
      dayInfo.value = await service.getDayInfo(props.date, props.regionCode);
    };

    watch(
      () => [props.date, props.regionCode],
      () => { void loadDay(); },
      { immediate: true },
    );

    return () => {
      const info = dayInfo.value;
      if (!info) return null;

      // Holiday badge
      if (info.isHoliday) {
        return h(
          'span',
          {
            style: 'display:inline-block;padding:2px 6px;border-radius:4px;font-size:12px;color:#fff;background-color:#f44336;',
          },
          '休',
        );
      }

      // Adjusted workday badge (weekend day that is a workday)
      if (info.isAdjustedWorkday) {
        return h(
          'span',
          {
            style: 'display:inline-block;padding:2px 6px;border-radius:4px;font-size:12px;color:#fff;background-color:#ff9800;',
          },
          '班',
        );
      }

      // Normal day — no badge
      return null;
    };
  },
});
