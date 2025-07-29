package com.amannmalik.mcp.ping;

import com.amannmalik.mcp.client.DefaultMcpClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class PingMonitor {
    private PingMonitor() {
    }

    public static boolean isAlive(DefaultMcpClient client, long timeoutMillis) {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<PingResponse> future = exec.submit(client::ping);
        try {
            future.get(timeoutMillis, TimeUnit.MILLISECONDS);
            return true;
        } catch (TimeoutException e) {
            future.cancel(true);
            return false;
        } catch (Exception e) {
            return false;
        } finally {
            exec.shutdownNow();
        }
    }
}
