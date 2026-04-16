package com.github.gmkits.holiday.core;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LRUCacheTest {

    @Test
    void shouldThrowOnNonPositiveMaxSize() {
        assertThrows(IllegalArgumentException.class, () -> new LRUCache<String, Integer>(0));
        assertThrows(IllegalArgumentException.class, () -> new LRUCache<String, Integer>(-1));
    }

    @Test
    void shouldStoreAndRetrieve() {
        LRUCache<String, Integer> cache = new LRUCache<>(3);
        cache.put("a", 1);
        assertEquals(1, cache.get("a"));
    }

    @Test
    void shouldReturnNullForMissing() {
        LRUCache<String, Integer> cache = new LRUCache<>(3);
        assertNull(cache.get("missing"));
    }

    @Test
    void shouldEvictLeastRecentlyUsed() {
        LRUCache<String, Integer> cache = new LRUCache<>(3);
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);
        cache.put("d", 4); // evicts 'a'
        assertNull(cache.get("a"));
        assertEquals(2, cache.get("b"));
        assertEquals(4, cache.get("d"));
    }

    @Test
    void shouldPromoteOnAccess() {
        LRUCache<String, Integer> cache = new LRUCache<>(3);
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);
        cache.get("a"); // promote 'a'
        cache.put("d", 4); // evicts 'b'
        assertEquals(1, cache.get("a"));
        assertNull(cache.get("b"));
    }

    @Test
    void shouldReportSize() {
        LRUCache<String, Integer> cache = new LRUCache<>(5);
        assertEquals(0, cache.size());
        cache.put("a", 1);
        assertEquals(1, cache.size());
        cache.put("b", 2);
        assertEquals(2, cache.size());
    }

    @Test
    void shouldClear() {
        LRUCache<String, Integer> cache = new LRUCache<>(3);
        cache.put("a", 1);
        cache.put("b", 2);
        cache.clear();
        assertEquals(0, cache.size());
        assertNull(cache.get("a"));
    }

    @Test
    void shouldWorkWithSizeOne() {
        LRUCache<String, Integer> cache = new LRUCache<>(1);
        cache.put("a", 1);
        assertEquals(1, cache.get("a"));
        cache.put("b", 2);
        assertNull(cache.get("a"));
        assertEquals(2, cache.get("b"));
        assertEquals(1, cache.size());
    }

    @Test
    void defaultConstructorShouldUseDefaultSize() {
        LRUCache<String, Integer> cache = new LRUCache<>();
        assertEquals(0, cache.size());
        // Fill beyond default size to verify it doesn't throw
        for (int i = 0; i < LRUCache.DEFAULT_MAX_SIZE + 5; i++) {
            cache.put("key" + i, i);
        }
        assertEquals(LRUCache.DEFAULT_MAX_SIZE, cache.size());
    }

    @Test
    void shouldLoadMissingValueOnce() {
        LRUCache<String, Integer> cache = new LRUCache<>(3);
        AtomicInteger loadCount = new AtomicInteger();

        assertEquals(1, cache.get("a", key -> loadCount.incrementAndGet()));
        assertEquals(1, cache.get("a", key -> loadCount.incrementAndGet()));
        assertEquals(1, loadCount.get());
    }
}
