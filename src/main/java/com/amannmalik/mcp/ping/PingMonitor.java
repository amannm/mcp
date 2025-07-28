package com.amannmalik.mcp.ping;

import com.amannmalik.mcp.client.SimpleMcpClient;
import com.amannmalik.mcp.jsonrpc.JsonRpcMessage;
import com.amannmalik.mcp.jsonrpc.JsonRpcResponse;
import jakarta.json.Json;

import java.util.concurrent.*;

/** Simple connectivity checker using ping requests. */
public final class PingMonitor {
    private PingMonitor() {}

    /**
     * Sends a ping request and waits for a response.
     *
     * @param client MCP client
     * @param timeoutMillis maximum time to wait for a response
     * @return {@code true} if a response was received within the timeout
     */
    public static boolean isAlive(SimpleMcpClient client, long timeoutMillis) {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<JsonRpcMessage> future = exec.submit(() ->
                client.request("ping", Json.createObjectBuilder().build())
        );
        try {
            JsonRpcMessage msg = future.get(timeoutMillis, TimeUnit.MILLISECONDS);
            return msg instanceof JsonRpcResponse;
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
