package com.amannmalik.mcp.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public final class RateLimiter {
    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final int limit;
    private final long windowMs;

    public RateLimiter(int limit, long windowMs) {
        this.limit = ValidationUtil.requirePositive(limit, "limit");
        this.windowMs = ValidationUtil.requirePositive(windowMs, "windowMs");
    }

    public void requireAllowance(String key) {
        var w = windows.computeIfAbsent(key, k -> new Window());
        var now = System.currentTimeMillis();
        w.lock.lock();
        try {
            if (now - w.start >= windowMs) {
                w.start = now;
                w.count = 0;
            }
            if (w.count >= limit) {
                throw new SecurityException("Rate limit exceeded: " + key);
            }
            w.count++;
        } finally {
            w.lock.unlock();
        }
    }

    private static final class Window {
        final ReentrantLock lock = new ReentrantLock();
        long start;
        int count;
    }
}
