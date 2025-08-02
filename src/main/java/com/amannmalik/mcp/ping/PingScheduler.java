package com.amannmalik.mcp.ping;

import com.amannmalik.mcp.client.McpClient;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PingScheduler implements AutoCloseable {
    private final McpClient client;
    private final long interval;
    private final long timeout;
    private final Runnable onFailure;
    private final int maxFailures;
    private int failureCount;
    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean started = new AtomicBoolean();

    public PingScheduler(McpClient client,
                         long intervalMillis,
                         long timeoutMillis,
                         Runnable onFailure) {
        this(client, intervalMillis, timeoutMillis, onFailure, 1);
    }

    public PingScheduler(McpClient client,
                         long intervalMillis,
                         long timeoutMillis,
                         Runnable onFailure,
                         int maxFailures) {
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(onFailure, "onFailure");
        if (intervalMillis <= 0 || timeoutMillis <= 0) throw new IllegalArgumentException("invalid timing");
        if (maxFailures <= 0) throw new IllegalArgumentException("maxFailures must be > 0");
        this.client = client;
        this.interval = intervalMillis;
        this.timeout = timeoutMillis;
        this.onFailure = onFailure;
        this.maxFailures = maxFailures;
    }

    public void start() {
        if (!started.compareAndSet(false, true)) throw new IllegalStateException("already started");
        exec.scheduleAtFixedRate(this::check, interval, interval, TimeUnit.MILLISECONDS);
    }

    private void check() {
        if (PingMonitor.isAlive(client, timeout)) {
            failureCount = 0;
            return;
        }

        failureCount++;
        if (failureCount >= maxFailures) {
            try {
                onFailure.run();
            } catch (Exception ignore) {
            } finally {
                failureCount = 0;
            }
        }
    }

    @Override
    public void close() {
        exec.shutdownNow();
    }
}
