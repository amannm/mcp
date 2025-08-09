package com.amannmalik.mcp.roots;

import com.amannmalik.mcp.config.McpConfiguration;
import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.transport.Transport;
import com.amannmalik.mcp.util.JsonRpcRequestProcessor;
import com.amannmalik.mcp.util.ProgressManager;
import com.amannmalik.mcp.util.RateLimiter;
import com.amannmalik.mcp.wire.RequestMethod;
import jakarta.json.JsonObject;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

public final class JsonRpcRequestSender implements RequestSender {
    private final Transport transport;
    private final JsonRpcRequestProcessor processor;
    private final Map<RequestId, CompletableFuture<JsonRpcMessage>> pending = new ConcurrentHashMap<>();
    private long counter;

    public JsonRpcRequestSender(Transport transport) {
        this(transport, new JsonRpcRequestProcessor(new ProgressManager(new RateLimiter(
                McpConfiguration.current().progressPerSecond(),
                McpConfiguration.current().rateLimiterWindowMs())), m -> {
            try {
                transport.send(JsonRpcCodec.CODEC.toJson(m));
            } catch (IOException ignore) {
            }
        }));
    }

    public JsonRpcRequestSender(Transport transport, JsonRpcRequestProcessor processor) {
        if (transport == null || processor == null) throw new IllegalArgumentException("transport and processor required");
        this.transport = transport;
        this.processor = processor;
    }

    @Override
    public JsonRpcMessage send(RequestMethod method, JsonObject params) throws IOException {
        RequestId id = new RequestId.NumericId(counter++);
        CompletableFuture<JsonRpcMessage> future = new CompletableFuture<>();
        pending.put(id, future);
        transport.send(JsonRpcCodec.CODEC.toJson(new JsonRpcRequest(id, method.method(), params)));
        long end = System.currentTimeMillis() + McpConfiguration.current().defaultMs();
        while (true) {
            if (future.isDone()) {
                try {
                    return future.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException(e);
                } catch (ExecutionException e) {
                    var cause = e.getCause();
                    if (cause instanceof IOException io) throw io;
                    throw new IOException(cause);
                }
            }
            if (System.currentTimeMillis() >= end) {
                throw new IOException(McpConfiguration.current().errorTimeout());
            }
            JsonRpcMessage msg = JsonRpcCodec.CODEC.fromJson(transport.receive());
            switch (msg) {
                case JsonRpcRequest req -> processor.handle(req, true).ifPresent(r -> {
                    try {
                        transport.send(JsonRpcCodec.CODEC.toJson(r));
                    } catch (IOException ignore) {
                    }
                });
                case JsonRpcNotification note -> processor.handle(note);
                case JsonRpcResponse resp -> complete(resp.id(), resp);
                case JsonRpcError err -> complete(err.id(), err);
                default -> { }
            }
        }
    }

    private void complete(RequestId id, JsonRpcMessage msg) {
        var f = pending.remove(id);
        if (f != null) f.complete(msg);
    }
}

