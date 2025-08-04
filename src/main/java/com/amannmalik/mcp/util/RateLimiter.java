package com.amannmalik.mcp.util;

import com.amannmalik.mcp.validation.ValidationUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RateLimiter {
    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final int limit;
    private final long windowMs;

    public RateLimiter(int limit, long windowMs) {
        this.limit = ValidationUtil.requirePositive(limit, "limit");
        this.windowMs = ValidationUtil.requirePositive(windowMs, "windowMs");
    }

    public void requireAllowance(String key) {
        Window w = windows.computeIfAbsent(key, k -> new Window());
        long now = System.currentTimeMillis();
        synchronized (w) {
            if (now - w.start >= windowMs) {
                w.start = now;
                w.count = 0;
            }
            if (w.count >= limit) throw new SecurityException("Rate limit exceeded: " + key);
            w.count++;
        }
    }

    private static final class Window {
        long start;
        int count;
    }
}
