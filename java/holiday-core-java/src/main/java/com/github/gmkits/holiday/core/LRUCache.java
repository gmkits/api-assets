package com.github.gmkits.holiday.core;

import com.google.common.base.Preconditions;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 线程安全 LRU 缓存（基于 access-order LinkedHashMap）。
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
public final class LRUCache<K, V> {

    public static final int DEFAULT_MAX_SIZE = 32;

    private final LinkedHashMap<K, V> map;
    private final int maxSize;

    public LRUCache() {
        this(DEFAULT_MAX_SIZE);
    }

    public LRUCache(int maxSize) {
        Preconditions.checkArgument(maxSize > 0, "maxSize 必须为正数: %s", maxSize);
        this.maxSize = maxSize;
        this.map = new LinkedHashMap<K, V>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > LRUCache.this.maxSize;
            }
        };
    }

    public synchronized V get(K key) {
        return map.get(key);
    }

    public synchronized V get(K key, Function<? super K, ? extends V> mappingFunction) {
        return map.computeIfAbsent(key, mappingFunction);
    }

    public synchronized void put(K key, V value) {
        map.put(key, value);
    }

    public synchronized int size() {
        return map.size();
    }

    public synchronized void clear() {
        map.clear();
    }
}
