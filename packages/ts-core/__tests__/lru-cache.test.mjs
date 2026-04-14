import { describe, it } from 'node:test';
import assert from 'node:assert/strict';

import { LRUCache } from '../dist/esm/index.js';

describe('LRUCache', () => {
  it('should throw RangeError for maxSize < 1', () => {
    assert.throws(() => new LRUCache(0), RangeError);
    assert.throws(() => new LRUCache(-1), RangeError);
  });

  it('should store and retrieve values', () => {
    const cache = new LRUCache(3);
    cache.set('a', 1);
    assert.equal(cache.get('a'), 1);
  });

  it('should return undefined for missing keys', () => {
    const cache = new LRUCache(3);
    assert.equal(cache.get('missing'), undefined);
  });

  it('should evict least recently used when full', () => {
    const cache = new LRUCache(3);
    cache.set('a', 1);
    cache.set('b', 2);
    cache.set('c', 3);
    cache.set('d', 4); // evicts 'a'
    assert.equal(cache.get('a'), undefined);
    assert.equal(cache.get('b'), 2);
    assert.equal(cache.get('d'), 4);
  });

  it('should promote accessed entry to most recent', () => {
    const cache = new LRUCache(3);
    cache.set('a', 1);
    cache.set('b', 2);
    cache.set('c', 3);
    cache.get('a'); // promote 'a'
    cache.set('d', 4); // evicts 'b' (now the oldest)
    assert.equal(cache.get('a'), 1);
    assert.equal(cache.get('b'), undefined);
  });

  it('should update existing key without increasing size', () => {
    const cache = new LRUCache(2);
    cache.set('a', 1);
    cache.set('b', 2);
    cache.set('a', 10); // update
    assert.equal(cache.size, 2);
    assert.equal(cache.get('a'), 10);
  });

  it('should report correct size', () => {
    const cache = new LRUCache(5);
    assert.equal(cache.size, 0);
    cache.set('a', 1);
    assert.equal(cache.size, 1);
    cache.set('b', 2);
    assert.equal(cache.size, 2);
  });

  it('should support has()', () => {
    const cache = new LRUCache(3);
    cache.set('a', 1);
    assert.equal(cache.has('a'), true);
    assert.equal(cache.has('b'), false);
  });

  it('should support delete()', () => {
    const cache = new LRUCache(3);
    cache.set('a', 1);
    assert.equal(cache.delete('a'), true);
    assert.equal(cache.get('a'), undefined);
    assert.equal(cache.size, 0);
    assert.equal(cache.delete('a'), false);
  });

  it('should support clear()', () => {
    const cache = new LRUCache(3);
    cache.set('a', 1);
    cache.set('b', 2);
    cache.clear();
    assert.equal(cache.size, 0);
    assert.equal(cache.get('a'), undefined);
  });

  it('should work with maxSize of 1', () => {
    const cache = new LRUCache(1);
    cache.set('a', 1);
    assert.equal(cache.get('a'), 1);
    cache.set('b', 2);
    assert.equal(cache.get('a'), undefined);
    assert.equal(cache.get('b'), 2);
    assert.equal(cache.size, 1);
  });
});
