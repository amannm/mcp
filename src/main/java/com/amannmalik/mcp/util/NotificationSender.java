package com.amannmalik.mcp.util;

import com.amannmalik.mcp.jsonrpc.JsonRpcNotification;

import java.io.IOException;

@FunctionalInterface
public interface NotificationSender {
    void send(JsonRpcNotification notification) throws IOException;
}
