package com.github.gmkits.holiday.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A simple, thread-safe Least-Recently-Used (LRU) cache.
 *
 * <p>When the number of entries exceeds {@code maxSize}, the least
 * recently accessed entry is automatically evicted.</p>
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public final class LRUCache<K, V> {

    /** Default maximum number of entries. */
    public static final int DEFAULT_MAX_SIZE = 32;

    private final int maxSize;
    private final LinkedHashMap<K, V> map;

    /**
     * Creates a cache with the {@link #DEFAULT_MAX_SIZE default} capacity.
     */
    public LRUCache() {
        this(DEFAULT_MAX_SIZE);
    }

    /**
     * Creates a cache with the given maximum size.
     *
     * @param maxSize the maximum number of entries to retain
     * @throws IllegalArgumentException if {@code maxSize} is not positive
     */
    public LRUCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be positive: " + maxSize);
        }
        this.maxSize = maxSize;
        this.map = new LinkedHashMap<K, V>(maxSize + 1, 0.75f, true) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > LRUCache.this.maxSize;
            }
        };
    }

    /**
     * Returns the value for the given key, or {@code null} if absent.
     *
     * @param key the key
     * @return the cached value, or {@code null}
     */
    public synchronized V get(K key) {
        return map.get(key);
    }

    /**
     * Associates the given key with the given value.
     *
     * @param key   the key
     * @param value the value
     */
    public synchronized void put(K key, V value) {
        map.put(key, value);
    }

    /**
     * Returns the current number of entries.
     *
     * @return cache size
     */
    public synchronized int size() {
        return map.size();
    }

    /**
     * Removes all entries from the cache.
     */
    public synchronized void clear() {
        map.clear();
    }
}
