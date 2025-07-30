package com.amannmalik.mcp.util;

public final class CloseUtil {
    private CloseUtil() {
    }

    public static void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) return;
        try {
            closeable.close();
        } catch (Exception ignore) {
        }
    }
}
