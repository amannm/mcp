package com.amannmalik.mcp.util;

import com.amannmalik.mcp.api.NotificationMethod;
import jakarta.json.JsonObject;

import java.io.IOException;

@FunctionalInterface
public interface NotificationSender {
    void send(NotificationMethod method, JsonObject params) throws IOException;
}
