package com.amannmalik.mcp.util;

@FunctionalInterface
public interface ProgressListener {
    void onProgress(ProgressNotification notification);
}
