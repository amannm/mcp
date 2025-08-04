package com.amannmalik.mcp.ping;

import com.amannmalik.mcp.McpClient;
import com.amannmalik.mcp.validation.ValidationUtil;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/// - [Prompts](specification/2025-06-18/basic/utilities/ping.mdx)
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
                         Runnable onFailure,
                         int maxFailures) {
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(onFailure, "onFailure");
        this.client = client;
        this.interval = ValidationUtil.requirePositive(intervalMillis, "intervalMillis");
        this.timeout = ValidationUtil.requirePositive(timeoutMillis, "timeoutMillis");
        this.onFailure = onFailure;
        this.maxFailures = ValidationUtil.requirePositive(maxFailures, "maxFailures");
    }

    public void start() {
        if (!started.compareAndSet(false, true)) throw new IllegalStateException("already started");
        exec.scheduleAtFixedRate(this::check, interval, interval, TimeUnit.MILLISECONDS);
    }

    private void check() {
        try {
            client.ping(timeout);
            failureCount = 0;
        } catch (IOException | RuntimeException e) {
            failureCount++;
            System.err.println("Ping failure: " + e.getMessage());
            if (failureCount >= maxFailures) {
                failureCount = 0;
                try {
                    onFailure.run();
                } catch (Exception ignore) {
                }
            }
        }
    }

    @Override
    public void close() {
        exec.shutdownNow();
    }
}
