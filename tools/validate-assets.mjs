#!/usr/bin/env node

import { existsSync, readFileSync, readdirSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import Ajv from 'ajv';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const schema = JSON.parse(readFileSync(join(root, 'shared/schemas/api-asset.schema.json'), 'utf8'));
const validate = new Ajv({ allErrors: true, strict: true }).compile(schema);
const apiRoot = join(root, 'apis');
const ids = new Set();
let count = 0;

for (const name of readdirSync(apiRoot, { withFileTypes: true })) {
  if (!name.isDirectory()) continue;
  const directory = join(apiRoot, name.name);
  const manifestPath = join(directory, 'api-asset.json');
  if (!existsSync(manifestPath)) throw new Error(`${name.name}: missing api-asset.json`);
  const manifest = JSON.parse(readFileSync(manifestPath, 'utf8'));
  if (!validate(manifest)) {
    throw new Error(`${name.name}: ${new Ajv().errorsText(validate.errors, { separator: '\n' })}`);
  }
  if (manifest.id !== name.name) throw new Error(`${name.name}: id must match directory name`);
  if (ids.has(manifest.id)) throw new Error(`${name.name}: duplicate asset id`);
  ids.add(manifest.id);
  if (!manifest.container.image.includes('${version}')) {
    throw new Error(`${name.name}: container.image must contain \${version}`);
  }
  const artifactIds = new Set();
  for (const relative of [manifest.contract.path, ...manifest.artifacts.map((item) => item.path)]) {
    const target = resolve(directory, relative);
    if (!target.startsWith(directory + '/') || !existsSync(target)) {
      throw new Error(`${name.name}: declared path does not exist: ${relative}`);
    }
  }
  for (const artifact of manifest.artifacts) {
    if (artifactIds.has(artifact.id)) throw new Error(`${name.name}: duplicate artifact id ${artifact.id}`);
    artifactIds.add(artifact.id);
  }
  const contract = readFileSync(join(directory, manifest.contract.path), 'utf8');
  if (!contract.includes(`\n  version: ${manifest.version}\n`)) {
    throw new Error(`${name.name}: OpenAPI version must match manifest version ${manifest.version}`);
  }
  const assetManifest = manifest.artifacts.find((item) => item.id === 'asset-manifest');
  if (assetManifest) {
    const runtime = JSON.parse(readFileSync(join(directory, assetManifest.path), 'utf8'));
    if (runtime.releaseVersion !== manifest.version) {
      throw new Error(`${name.name}: runtime asset version must match ${manifest.version}`);
    }
  }
  count++;
}

if (count === 0) throw new Error('No API assets found');
console.log(`✓ ${count} API asset manifest(s) validated`);
