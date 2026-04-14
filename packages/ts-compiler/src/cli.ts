// ============================================================
// Holiday Data Platform — CLI Entry Point
// ============================================================

import * as fs from 'node:fs';
import * as path from 'node:path';
import { parseArgs } from 'node:util';
import { importGovNotice } from './importers/gov-notice-importer.js';
import { validate } from './validator.js';
import { materialize } from './materializer.js';
import { compile } from './hday-compiler.js';
import { readHday } from './hday-reader.js';
import { buildManifest } from './manifest-builder.js';

import type { CanonicalDocument, MaterializedYearData } from '@holiday/spec';

const USAGE = `
holiday-compiler — Holiday Data Platform compiler pipeline

Usage:
  holiday-compiler <command> [options]

Commands:
  import          Import a raw .source.json into canonical format
  validate        Validate a canonical JSON document
  materialize     Materialize a canonical document into year data
  compile         Compile materialized year data into .hday binary
  build-manifest  Build manifest.json from .hday bundles directory
  inspect         Inspect an .hday binary file

Options:
  --input, -i     Input file path
  --output, -o    Output file path
  --bundles-dir   Bundles directory (for build-manifest)
  --bundle, -b    Bundle file path (for inspect)
  --help, -h      Show this help message
`.trim();

function die(message: string): never {
  console.error(`Error: ${message}`);
  process.exit(1);
}

function showHelp(): never {
  console.log(USAGE);
  process.exit(0);
}

/**
 * CLI entry point for the holiday compiler.
 *
 * Supports commands: import, validate, materialize, compile, build-manifest, inspect.
 */
async function main(): Promise<void> {
  const { values, positionals } = parseArgs({
    options: {
      input: { type: 'string', short: 'i' },
      output: { type: 'string', short: 'o' },
      'bundles-dir': { type: 'string' },
      bundle: { type: 'string', short: 'b' },
      help: { type: 'boolean', short: 'h' },
    },
    allowPositionals: true,
    strict: true,
  });

  if (values.help || positionals.length === 0) {
    showHelp();
  }

  const command = positionals[0];

  switch (command) {
    case 'import': {
      const inputPath = values.input;
      if (!inputPath) die('--input is required for import');

      const raw = JSON.parse(fs.readFileSync(inputPath, 'utf-8'));
      const doc = importGovNotice(raw);
      const output = values.output;
      if (output) {
        fs.mkdirSync(path.dirname(output), { recursive: true });
        fs.writeFileSync(output, JSON.stringify(doc, null, 2));
        console.log(`✓ Imported: ${inputPath} → ${output}`);
      } else {
        console.log(JSON.stringify(doc, null, 2));
      }
      break;
    }

    case 'validate': {
      const inputPath = values.input;
      if (!inputPath) die('--input is required for validate');

      const doc: CanonicalDocument = JSON.parse(fs.readFileSync(inputPath, 'utf-8'));
      const result = validate(doc);

      if (result.warnings.length > 0) {
        console.log('Warnings:');
        for (const w of result.warnings) {
          console.log(`  ⚠ ${w}`);
        }
      }

      if (result.errors.length > 0) {
        console.log('Errors:');
        for (const e of result.errors) {
          console.log(`  ✗ ${e}`);
        }
        die(`Validation failed with ${result.errors.length} error(s)`);
      }

      console.log('✓ Validation passed');
      break;
    }

    case 'materialize': {
      const inputPath = values.input;
      const outputPath = values.output;
      if (!inputPath) die('--input is required for materialize');
      if (!outputPath) die('--output is required for materialize');

      const doc: CanonicalDocument = JSON.parse(fs.readFileSync(inputPath, 'utf-8'));
      const yearData = materialize(doc);

      fs.mkdirSync(path.dirname(outputPath), { recursive: true });
      fs.writeFileSync(outputPath, JSON.stringify(yearData, null, 2));
      console.log(`✓ Materialized: ${Object.keys(yearData.days).length} days → ${outputPath}`);
      break;
    }

    case 'compile': {
      const inputPath = values.input;
      const outputPath = values.output;
      if (!inputPath) die('--input is required for compile');
      if (!outputPath) die('--output is required for compile');

      const yearData: MaterializedYearData = JSON.parse(fs.readFileSync(inputPath, 'utf-8'));
      const hdayBuf = compile(yearData);

      fs.mkdirSync(path.dirname(outputPath), { recursive: true });
      fs.writeFileSync(outputPath, hdayBuf);
      console.log(`✓ Compiled: ${hdayBuf.length} bytes → ${outputPath}`);
      break;
    }

    case 'build-manifest': {
      const bundlesDir = values['bundles-dir'];
      const outputPath = values.output;
      if (!bundlesDir) die('--bundles-dir is required for build-manifest');
      if (!outputPath) die('--output is required for build-manifest');

      const manifest = buildManifest(bundlesDir);

      fs.mkdirSync(path.dirname(outputPath), { recursive: true });
      fs.writeFileSync(outputPath, JSON.stringify(manifest, null, 2));

      const regionCount = Object.keys(manifest.bundles).length;
      const bundleCount = Object.values(manifest.bundles).reduce(
        (sum, region) => sum + Object.keys(region).length, 0,
      );
      console.log(`✓ Manifest: ${bundleCount} bundle(s) across ${regionCount} region(s) → ${outputPath}`);
      break;
    }

    case 'inspect': {
      const bundlePath = values.bundle ?? values.input;
      if (!bundlePath) die('--bundle or --input is required for inspect');

      const buf = fs.readFileSync(bundlePath);
      const yearData = readHday(Buffer.from(buf));

      console.log(`=== .hday Inspection: ${bundlePath} ===`);
      console.log(`Region:    ${yearData.meta.regionCode}`);
      console.log(`Year:      ${yearData.meta.year}`);
      console.log(`Calendar:  ${yearData.meta.calendarSystem}`);
      console.log(`Days:      ${Object.keys(yearData.days).length}`);

      // Summary stats
      let holidays = 0;
      let workdays = 0;
      let weekends = 0;
      let statutory = 0;
      let adjusted = 0;

      for (const day of Object.values(yearData.days)) {
        if (day.isHoliday) holidays++;
        if (day.isWorkday) workdays++;
        if (day.isWeekend) weekends++;
        if (day.isStatutoryHoliday) statutory++;
        if (day.isAdjustedWorkday) adjusted++;
      }

      console.log(`\nSummary:`);
      console.log(`  Holidays:          ${holidays}`);
      console.log(`  Workdays:          ${workdays}`);
      console.log(`  Weekends:          ${weekends}`);
      console.log(`  Statutory:         ${statutory}`);
      console.log(`  Adjusted workdays: ${adjusted}`);

      if (values.output) {
        fs.mkdirSync(path.dirname(values.output), { recursive: true });
        fs.writeFileSync(values.output, JSON.stringify(yearData, null, 2));
        console.log(`\n✓ Written expanded JSON to ${values.output}`);
      }
      break;
    }

    default:
      die(`Unknown command: ${command}\n\n${USAGE}`);
  }
}

main().catch((err: Error) => {
  console.error(`Fatal: ${err.message}`);
  process.exit(1);
});
