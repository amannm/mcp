package com.amannmalik.mcp.api;


import java.io.IOException;
import java.lang.System.Logger;
import java.time.Duration;
import java.util.concurrent.*;

/// - [Ping](specification/2025-06-18/basic/utilities/ping.mdx)
final class ClientBackgroundTasks implements AutoCloseable {
    private static final Logger LOG = System.getLogger(ClientBackgroundTasks.class.getName());
    private final McpClient client;
    private final Duration interval;
    private final Duration timeout;
    private Thread reader;
    private ScheduledExecutorService pinger;
    private int failures;

    public ClientBackgroundTasks(McpClient client, Duration interval, Duration timeout) {
        this.client = client;
        this.interval = interval;
        this.timeout = timeout;
    }

    public void start() {
        reader = new Thread(this::readLoop);
        reader.setDaemon(true);
        reader.start();
        if (interval.isPositive()) {
            pinger = Executors.newSingleThreadScheduledExecutor();
            failures = 0;
            pinger.scheduleAtFixedRate(this::ping, interval.toMillis(), interval.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    private void readLoop() {
        while (client.connected()) {
            try {
                var msg = JsonRpcEndpoint.CODEC.fromJson(client.transport.receive());
                client.process(msg);
            } catch (IOException e) {
                client.pending.values().forEach(f -> f.completeExceptionally(e));
                break;
            }
        }
    }

    private void ping() {
        try {
            client.ping(timeout);
            failures = 0;
        } catch (IOException | RuntimeException e) {
            handlePingFailure(e);
        }
    }

    private void handlePingFailure(Exception e) {
        failures++;
        LOG.log(Logger.Level.WARNING, "Ping failure", e);
        if (failures >= 3) {
            failures = 0;
            disconnectAfterPingFailures();
        }
    }

    private void disconnectAfterPingFailures() {
        try {
            client.disconnect();
        } catch (IOException e) {
            LOG.log(Logger.Level.ERROR, "Disconnect failed", e);
        }
    }

    @Override
    public void close() {
        if (pinger != null) {
            pinger.shutdownNow();
            pinger = null;
        }
        if (reader != null) {
            try {
                reader.join(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.log(Logger.Level.WARNING, "Interrupted while waiting for reader", e);
            }
            reader = null;
        }
    }
}

