#!/usr/bin/env node

import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { buildCalendarAsset } from './lib/calendar-asset.mjs';

const root = fileURLToPath(new URL('../..', import.meta.url));
const output = join(root, 'assets/runtime/calendar/calendar.cdat');
const bytes = buildCalendarAsset({
  lunarSource: join(root, 'assets/source/calendar/lunar-years.hex'),
  solarSource: join(root, 'tests/golden/solar-terms.csv'),
  output,
});
console.log(`✓ calendar.cdat built: ${bytes} bytes → ${output}`);
