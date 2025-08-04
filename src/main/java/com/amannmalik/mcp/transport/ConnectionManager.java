package com.amannmalik.mcp.transport;

public final class ConnectionManager {
    private boolean connected;
    private int pending;

    public void connect() {
        connected = true;
    }

    public void sendRequest() {
        if (!connected) throw new IllegalStateException("not connected");
        pending++;
    }

    public void completeRequest() {
        if (pending > 0) pending--;
    }

    public void interrupt() {
        connected = false;
        pending = 0;
    }

    public boolean connected() {
        return connected;
    }

    public int pending() {
        return pending;
    }
}

