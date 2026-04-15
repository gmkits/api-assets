package com.github.gmkits.holiday.core;

import com.google.common.base.Preconditions;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 简单的线程安全 LRU（最近最少使用）缓存。
 *
 * <p>当条目数超过 {@code maxSize} 时，最久未访问的条目会被自动淘汰。</p>
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
public final class LRUCache<K, V> {

    /** 默认最大条目数。 */
    public static final int DEFAULT_MAX_SIZE = 32;

    private final int maxSize;
    private final LinkedHashMap<K, V> map;

    /**
     * 使用 {@link #DEFAULT_MAX_SIZE 默认容量} 创建缓存。
     */
    public LRUCache() {
        this(DEFAULT_MAX_SIZE);
    }

    /**
     * 以给定的最大容量创建缓存。
     *
     * @param maxSize 最大条目数
     * @throws IllegalArgumentException 若 {@code maxSize} 不为正数
     */
    public LRUCache(int maxSize) {
        Preconditions.checkArgument(maxSize > 0, "maxSize 必须为正数: %s", maxSize);
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
     * 获取指定键的值，不存在时返回 {@code null}。
     *
     * @param key 键
     * @return 缓存的值，或 {@code null}
     */
    public synchronized V get(K key) {
        return map.get(key);
    }

    /**
     * 将键值对放入缓存。
     *
     * @param key   键
     * @param value 值
     */
    public synchronized void put(K key, V value) {
        map.put(key, value);
    }

    /**
     * 返回当前缓存条目数。
     *
     * @return 缓存大小
     */
    public synchronized int size() {
        return map.size();
    }

    /**
     * 清空所有缓存条目。
     */
    public synchronized void clear() {
        map.clear();
    }
}
