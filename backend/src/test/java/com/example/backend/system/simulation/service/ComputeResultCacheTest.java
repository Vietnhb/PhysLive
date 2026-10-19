package com.example.backend.system.simulation.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/** Kept results stay within their byte budget, least recently used first out; 0 keeps nothing. */
class ComputeResultCacheTest {

    @Test
    void theLeastRecentlyUsedResultLeavesFirstWhenTheBudgetIsFull() {
        var cache = new ComputeResultCache(400);
        cache.put("a", new byte[100]);
        cache.put("b", new byte[100]);
        cache.put("c", new byte[100]);
        assertNotNull(cache.get("a")); // a is now the most recent
        cache.put("d", new byte[100]);
        cache.put("e", new byte[100]); // 500 bytes > 400: the least recently used (b) leaves
        assertNull(cache.get("b"));
        assertNotNull(cache.get("a"));
        assertNotNull(cache.get("e"));
    }

    @Test
    void aResultLargerThanAQuarterOfTheBudgetIsNotKept() {
        var cache = new ComputeResultCache(400);
        cache.put("small", new byte[50]);
        cache.put("huge", new byte[150]);
        assertNull(cache.get("huge"));
        assertNotNull(cache.get("small"));
    }

    @Test
    void aBudgetOfZeroKeepsNothing() {
        var cache = new ComputeResultCache(0);
        cache.put("a", new byte[1]);
        assertNull(cache.get("a"));
    }

    @Test
    void aKeptResultIsReturnedAsStored() {
        var cache = new ComputeResultCache(1000);
        byte[] value = {1, 2, 3};
        cache.put("k", value);
        assertArrayEquals(value, cache.get("k"));
    }
}
