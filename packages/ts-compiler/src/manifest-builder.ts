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

/**
 * Recursively find all .hday files in a directory.
 */
function findHdayFiles(
  dir: string,
  baseDir: string,
): Array<{ relativePath: string; absolutePath: string }> {
  const results: Array<{ relativePath: string; absolutePath: string }> = [];
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      results.push(...findHdayFiles(fullPath, baseDir));
    } else if (entry.isFile() && entry.name.endsWith('.hday')) {
      results.push({
        relativePath: path.relative(baseDir, fullPath),
        absolutePath: fullPath,
      });
    }
  }
  return results;
}

/**
 * Parse region code and year from an .hday file header.
 */
function parseHdayHeader(buf: Buffer): { regionCode: string; year: number } {
  const magic = buf.subarray(0, 4).toString('ascii');
  if (magic !== HDAY_MAGIC) {
    throw new Error('Invalid .hday file');
  }
  const year = buf.readUInt16LE(8);
  const regionCodeLen = buf.readUInt8(10);
  const regionCode = buf.subarray(11, 11 + regionCodeLen).toString('utf-8');
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
  const hdayFiles = findHdayFiles(bundlesDir, bundlesDir);
  const bundles: Record<string, Record<string, BundleEntry>> = {};

  for (const { relativePath, absolutePath } of hdayFiles) {
    const buf = fs.readFileSync(absolutePath);
    const stat = fs.statSync(absolutePath);

    const { regionCode, year } = parseHdayHeader(buf);

    const sha256 = crypto.createHash('sha256').update(buf).digest('hex');
    // Read the embedded CRC32 from the last 4 bytes of the .hday file
    const embeddedCrc = buf.readUInt32LE(buf.length - 4);
    const crcHex = embeddedCrc.toString(16).padStart(8, '0');

    if (!bundles[regionCode]) {
      bundles[regionCode] = {};
    }

    bundles[regionCode][String(year)] = {
      file: relativePath,
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
