package com.amannmalik.mcp.server.logging;

@FunctionalInterface
public interface LoggingListener {
    void onMessage(LoggingMessageNotification notification);
}
