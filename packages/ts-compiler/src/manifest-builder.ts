// ============================================================
// Holiday Data Platform — Manifest Builder
// ============================================================

import * as fs from 'node:fs';
import * as path from 'node:path';
import * as crypto from 'node:crypto';
import type { Manifest, BundleEntry } from '@holiday/spec';
import {
  HDAY_MAGIC,
} from '@holiday/spec';
import { crc32 } from './hday-compiler.js';

/**
 * Parse region code and year from an .hday file header.
 */
function parseHdayHeader(buf: Buffer): { regionCode: string; year: number } {
  const magic = buf.subarray(0, 4).toString('ascii');
  if (magic !== HDAY_MAGIC) {
    throw new Error('Invalid .hday file');
  }
  const year = buf.readUInt16LE(6);
  const regionCodeLen = buf.readUInt8(8);
  const regionCode = buf.subarray(9, 9 + regionCodeLen).toString('utf-8');
  return { regionCode, year };
}

/**
 * Scan a directory of .hday bundles and generate a manifest.json.
 *
 * The manifest indexes all .hday files by region and year, including
 * integrity checksums (SHA-256 and CRC32) for each bundle.
 *
 * @param bundlesDir - Path to the directory containing .hday files
 * @param options - Optional overrides for specVersion, defaultRegion, etc.
 * @returns A complete Manifest object
 *
 * @example
 * ```ts
 * const manifest = buildManifest('./dist/bundles');
 * fs.writeFileSync('./dist/manifest.json', JSON.stringify(manifest, null, 2));
 * ```
 */
export function buildManifest(
  bundlesDir: string,
  options?: {
    specVersion?: string;
    bundleFormatVersion?: string;
    defaultRegion?: string;
  },
): Manifest {
  const files = fs.readdirSync(bundlesDir).filter(f => f.endsWith('.hday'));
  const bundles: Record<string, Record<string, BundleEntry>> = {};

  for (const file of files) {
    const filePath = path.join(bundlesDir, file);
    const buf = fs.readFileSync(filePath);
    const stat = fs.statSync(filePath);

    const { regionCode, year } = parseHdayHeader(buf);

    const sha256 = crypto.createHash('sha256').update(buf).digest('hex');
    const crcValue = crc32(buf);
    const crcHex = crcValue.toString(16).padStart(8, '0');

    if (!bundles[regionCode]) {
      bundles[regionCode] = {};
    }

    bundles[regionCode][String(year)] = {
      file,
      sha256,
      crc32: crcHex,
      sourceVersion: '',
      size: stat.size,
    };
  }

  return {
    specVersion: options?.specVersion ?? '1.0.0',
    bundleFormatVersion: options?.bundleFormatVersion ?? '1',
    defaultRegion: options?.defaultRegion ?? Object.keys(bundles)[0] ?? 'CN',
    publishedAt: new Date().toISOString(),
    bundles,
  };
}
