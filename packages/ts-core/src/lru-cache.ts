/**
 * @holiday/core — LRU (Least Recently Used) Cache
 *
 * A simple, generic LRU cache backed by a `Map`. Because `Map` preserves
 * insertion order, we can implement LRU eviction by deleting and re-inserting
 * on every access, and evicting the first (oldest) entry when capacity is
 * exceeded.
 *
 * @module
 */

/**
 * A bounded cache that automatically evicts the least-recently-used entry
 * when the configured maximum size is reached.
 *
 * @typeParam K - Cache key type.
 * @typeParam V - Cache value type.
 *
 * @example
 * ```ts
 * const cache = new LRUCache<string, number>(3);
 * cache.set('a', 1);
 * cache.set('b', 2);
 * cache.set('c', 3);
 * cache.set('d', 4); // evicts 'a'
 * cache.get('b');     // returns 2 and promotes 'b' to most-recent
 * ```
 */
export class LRUCache<K, V> {
  /** Internal ordered map (insertion-order = access-order after promotion). */
  private readonly map: Map<K, V>;

  /** Maximum number of entries before the oldest is evicted. */
  private readonly maxSize: number;

  /**
   * Create a new LRU cache.
   *
   * @param maxSize - Maximum number of entries. Must be ≥ 1.
   * @throws {RangeError} If `maxSize` is less than 1.
   */
  constructor(maxSize: number) {
    if (maxSize < 1) {
        throw new RangeError(`LRUCache maxSize 必须 >= 1，当前值: ${maxSize}`);
    }
    this.maxSize = maxSize;
    this.map = new Map<K, V>();
  }

  /**
   * Retrieve the value for `key`, promoting it to most-recently-used.
   *
   * @param key - The cache key.
   * @returns The cached value, or `undefined` if not present.
   */
  get(key: K): V | undefined {
    const value = this.map.get(key);
    if (value === undefined) {
      return undefined;
    }
    // Promote to most-recently-used by re-inserting
    this.map.delete(key);
    this.map.set(key, value);
    return value;
  }

  /**
   * Insert or update a cache entry. If inserting would exceed `maxSize`,
   * the least-recently-used entry is evicted first.
   *
   * @param key   - The cache key.
   * @param value - The value to cache.
   */
  set(key: K, value: V): void {
    // If the key already exists, delete it so re-insertion updates order
    if (this.map.has(key)) {
      this.map.delete(key);
    }
    this.map.set(key, value);

    // Evict the oldest entry if we exceed capacity
    if (this.map.size > this.maxSize) {
      const oldest = this.map.keys().next().value as K;
      this.map.delete(oldest);
    }
  }

  /**
   * Check whether the cache contains `key` (does **not** promote it).
   *
   * @param key - The cache key.
   * @returns `true` if the key is present.
   */
  has(key: K): boolean {
    return this.map.has(key);
  }

  /**
   * Remove a single entry from the cache.
   *
   * @param key - The cache key to remove.
   * @returns `true` if the key was present and removed.
   */
  delete(key: K): boolean {
    return this.map.delete(key);
  }

  /**
   * Remove all entries from the cache.
   */
  clear(): void {
    this.map.clear();
  }

  /**
   * The current number of entries in the cache.
   */
  get size(): number {
    return this.map.size;
  }
}
