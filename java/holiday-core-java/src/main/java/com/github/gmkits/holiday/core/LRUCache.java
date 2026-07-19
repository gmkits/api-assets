package com.github.gmkits.holiday.core;

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

    /** 默认最多缓存 32 个键值对。 */
    public static final int DEFAULT_MAX_SIZE = 32;

    private final LinkedHashMap<K, V> map;
    private final int maxSize;

    /**
     * 使用 {@link #DEFAULT_MAX_SIZE} 创建缓存。
     */
    public LRUCache() {
        this(DEFAULT_MAX_SIZE);
    }

    /**
     * 创建指定容量的缓存。
     *
     * @param maxSize 最大条目数
     * @throws IllegalArgumentException 最大条目数不是正数时抛出
     */
    public LRUCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize 必须为正数: " + maxSize);
        }
        this.maxSize = maxSize;
        this.map = new LinkedHashMap<K, V>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > LRUCache.this.maxSize;
            }
        };
    }

    /**
     * 查询缓存值，并更新条目的最近访问顺序。
     *
     * @param key 缓存键
     * @return 缓存值；键不存在时返回 {@code null}
     */
    public synchronized V get(K key) {
        return map.get(key);
    }

    /**
     * 查询缓存值；键不存在时在同步区内计算并写入。
     *
     * @param key 缓存键
     * @param mappingFunction 缓存未命中时使用的加载函数
     * @return 已存在或新加载的值
     */
    public synchronized V get(K key, Function<? super K, ? extends V> mappingFunction) {
        return map.computeIfAbsent(key, mappingFunction);
    }

    /**
     * 写入或替换缓存条目。
     *
     * @param key 缓存键
     * @param value 缓存值
     */
    public synchronized void put(K key, V value) {
        map.put(key, value);
    }

    /**
     * 返回当前缓存条目数。
     *
     * @return 当前缓存条目数
     */
    public synchronized int size() {
        return map.size();
    }

    /**
     * 清空全部缓存条目。
     */
    public synchronized void clear() {
        map.clear();
    }
}
