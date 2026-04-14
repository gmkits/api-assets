// CJS example: using @holiday/core
const { createHolidayService } = require('../../packages/ts-core/dist/cjs/index.cjs');

const service = createHolidayService({
  dataPath: '../../data/bundles',
  defaultRegion: 'CN',
});

async function main() {
  console.log('=== Holiday Kit CJS Example ===\n');

  const info = await service.getDayInfo('2025-10-01');
  console.log('2025-10-01:', JSON.stringify(info, null, 2));

  console.log('\nIs 2025-01-01 a holiday?', await service.isHoliday('2025-01-01'));
  console.log('Is 2025-03-03 a workday?', await service.isWorkday('2025-03-03'));
}

main().catch(console.error);
