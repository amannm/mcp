package com.amannmalik.mcp.util;

public final class CloseUtil {
    private CloseUtil() {
    }

    public static void close(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
