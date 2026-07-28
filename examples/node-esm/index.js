// ESM example: using @holiday/core
// In a real project: npm install @holiday/core
// Here we use relative imports for demonstration

import { createHolidayService } from '../../packages/ts-core/dist/esm/index.js';

const service = createHolidayService({
  dataPath: '../../data/date-assets/holidays/bundles',
  defaultRegion: 'CN',
});

async function main() {
  console.log('=== Holiday Kit ESM Example ===\n');

  // Check specific dates
  const dates = ['2025-01-01', '2025-01-28', '2025-05-01', '2025-10-01', '2025-03-03'];
  for (const date of dates) {
    const info = await service.getDayInfo(date);
    const status = info.isHoliday ? '🎉 Holiday' : info.isAdjustedWorkday ? '💼 Adjusted Workday' : info.isWorkday ? '💼 Workday' : '🏖️ Weekend';
    const name = info.holidayNames?.['zh-CN']?.[0] ?? '';
    console.log(`${date}: ${status} ${name}`);
  }

  console.log('\n--- Quick checks ---');
  console.log('Is 2025-10-01 a holiday?', await service.isHoliday('2025-10-01'));
  console.log('Is 2025-01-26 a workday?', await service.isWorkday('2025-01-26'));
}

main().catch(console.error);
