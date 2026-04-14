/**
 * Ambient type declarations for APIs that exist in both Node.js and browsers
 * but are not part of the ES2020 lib.
 *
 * These allow the code to type-check without pulling in `@types/node` or
 * full DOM lib typings, keeping the package environment-agnostic.
 */

/* eslint-disable @typescript-eslint/no-explicit-any */

// TextEncoder / TextDecoder are available in all modern runtimes
declare class TextDecoder {
  constructor(label?: string, options?: { fatal?: boolean; ignoreBOM?: boolean });
  decode(input?: ArrayBufferView | ArrayBuffer, options?: { stream?: boolean }): string;
}

// fetch is globally available in browsers, Node 18+, Deno, Bun
declare function fetch(input: string, init?: Record<string, any>): Promise<{
  ok: boolean;
  status: number;
  arrayBuffer(): Promise<ArrayBuffer>;
}>;
