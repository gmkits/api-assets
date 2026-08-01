// CJS example: using @holiday/core
const { createHolidayService } = require('../../packages/ts-core/dist/cjs/index.cjs');
const { readFileSync } = require('node:fs');

const calendarBytes = readFileSync('../../data/date-assets/calendar/calendar.cdat');

const service = createHolidayService({
  dataPath: '../../data/date-assets/holidays/bundles',
  defaultRegion: 'CN',
  calendarData: calendarBytes.buffer.slice(
    calendarBytes.byteOffset,
    calendarBytes.byteOffset + calendarBytes.byteLength,
  ),
});

async function main() {
  console.log('=== Holiday Kit CJS Example ===\n');

  const info = await service.getDayInfo('2025-10-01');
  console.log('2025-10-01:', JSON.stringify(info, null, 2));

  console.log('\nIs 2025-01-01 a holiday?', await service.isHoliday('2025-01-01'));
  console.log('Is 2025-03-03 a workday?', await service.isWorkday('2025-03-03'));
}

main().catch(console.error);
