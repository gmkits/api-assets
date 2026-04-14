import { defineComponent, h, ref, watch, type PropType } from 'vue';
import type { DayInfo } from '@holiday/spec';
import { createHolidayService } from '@holiday/core';

/**
 * HolidayCalendar — renders a simple month-grid calendar.
 *
 * Uses programmatic `h()` rendering (no SFC).
 * Marks holidays in red and adjusted workdays with a badge.
 */
export const HolidayCalendar = defineComponent({
  name: 'HolidayCalendar',
  props: {
    year: { type: Number, required: true },
    month: { type: Number, required: true },
    regionCode: { type: String, default: 'CN' },
    locale: { type: String as PropType<string>, default: 'zh-CN' },
  },
  emits: ['dayClick'],
  setup(props, { emit }) {
    const days = ref<DayInfo[]>([]);
    const service = createHolidayService({ defaultRegion: props.regionCode });

    const loadMonth = async () => {
      const y = props.year;
      const m = props.month;
      const firstDay = `${y}-${String(m).padStart(2, '0')}-01`;
      const lastDate = new Date(y, m, 0).getDate();
      const lastDay = `${y}-${String(m).padStart(2, '0')}-${String(lastDate).padStart(2, '0')}`;
      days.value = await service.getRange(firstDay, lastDay, props.regionCode);
    };

    watch(
      () => [props.year, props.month, props.regionCode],
      () => { void loadMonth(); },
      { immediate: true },
    );

    return () => {
      const weekHeaders = ['一', '二', '三', '四', '五', '六', '日'];

      // Build header row
      const headerCells = weekHeaders.map((d) =>
        h('th', { style: 'padding:4px 8px;text-align:center;' }, d),
      );

      // Build day cells
      const firstDate = days.value.length > 0 ? new Date(days.value[0].date) : null;
      // getDay: 0=Sun..6=Sat → convert to Mon=0..Sun=6
      const startOffset = firstDate
        ? (firstDate.getUTCDay() + 6) % 7
        : 0;

      const cells: ReturnType<typeof h>[] = [];
      // Blank cells before first day
      for (let i = 0; i < startOffset; i++) {
        cells.push(h('td', { style: 'padding:4px 8px;' }));
      }

      for (const day of days.value) {
        const dateNum = parseInt(day.date.slice(8), 10);
        const isHoliday = day.isHoliday;
        const isAdjusted = day.isAdjustedWorkday;

        const style: Record<string, string> = {
          padding: '4px 8px',
          textAlign: 'center',
          cursor: 'pointer',
          borderRadius: '4px',
        };

        if (isHoliday) {
          style.color = 'red';
          style.fontWeight = 'bold';
        }
        if (isAdjusted) {
          style.backgroundColor = '#fff3e0';
        }

        const children: (string | ReturnType<typeof h>)[] = [String(dateNum)];
        if (isHoliday) {
          children.push(h('sup', { style: 'font-size:10px;color:red;' }, '休'));
        } else if (isAdjusted) {
          children.push(h('sup', { style: 'font-size:10px;color:#e65100;' }, '班'));
        }

        cells.push(
          h(
            'td',
            {
              style,
              onClick: () => emit('dayClick', day),
            },
            children,
          ),
        );
      }

      // Build rows (7 cells per row)
      const rows: ReturnType<typeof h>[] = [];
      for (let i = 0; i < cells.length; i += 7) {
        rows.push(h('tr', cells.slice(i, i + 7)));
      }

      const names = days.value.length > 0 ? days.value[0].holidayNames : {};
      const localeName = names[props.locale]?.[0] ?? '';

      return h('table', { style: 'border-collapse:collapse;' }, [
        h('caption', { style: 'font-weight:bold;padding:8px;' }, [
          `${props.year}年${props.month}月`,
          localeName ? ` — ${localeName}` : '',
        ]),
        h('thead', [h('tr', headerCells)]),
        h('tbody', rows),
      ]);
    };
  },
});
