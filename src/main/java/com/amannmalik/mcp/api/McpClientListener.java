package com.amannmalik.mcp.api;

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
