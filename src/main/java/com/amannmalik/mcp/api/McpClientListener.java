package com.amannmalik.mcp.api;

import com.amannmalik.mcp.api.Notification.LoggingMessageNotification;
import com.amannmalik.mcp.api.Notification.ProgressNotification;

public interface McpClientListener {
    default void onProgress(ProgressNotification notification) {
    }

    default void onMessage(LoggingMessageNotification notification) {
    }

    default void onResourceListChanged() {
    }

    default void onToolListChanged() {
    }

    default void onPromptsListChanged() {
    }
}
