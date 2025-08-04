package com.amannmalik.mcp.transport;

import jakarta.json.JsonObject;

import java.io.EOFException;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public final class HttpTransport implements Transport {
    private final BlockingQueue<JsonObject> inbox = new LinkedBlockingQueue<>();
    private final Set<String> features = Set.of(
            "authorization",
            "resource_metadata",
            "session_management");
    private volatile boolean closed;

    @Override
    public void send(JsonObject message) {
        inbox.add(message);
    }

    @Override
    public JsonObject receive() throws IOException {
        try {
            JsonObject obj = inbox.take();
            if (closed) throw new EOFException();
            return obj;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    @Override
    public void close() {
        closed = true;
    }

    public boolean supports(String feature) {
        return features.contains(feature);
    }
}

