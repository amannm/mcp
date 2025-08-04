package com.amannmalik.mcp.transport;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class ConnectionManager {
    private final AtomicBoolean connected = new AtomicBoolean();
    private final AtomicInteger pending = new AtomicInteger();

    public void connect() {
        connected.set(true);
    }

    public void sendRequest() {
        if (!connected.get()) throw new IllegalStateException("not connected");
        pending.incrementAndGet();
    }

    public void completeRequest() {
        pending.getAndUpdate(p -> p > 0 ? p - 1 : 0);
    }

    public void interrupt() {
        connected.set(false);
        pending.set(0);
    }

    public boolean connected() {
        return connected.get();
    }

    public int pending() {
        return pending.get();
    }
}

