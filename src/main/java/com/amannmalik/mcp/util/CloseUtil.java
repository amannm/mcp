package com.amannmalik.mcp.util;

import java.io.IOException;

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

    public static void closeAll(AutoCloseable... closeables) throws IOException {
        IOException ex = null;
        for (var c : closeables) {
            if (c == null) {
                continue;
            }
            try {
                c.close();
            } catch (IOException e) {
                if (ex == null) {
                    ex = e;
                } else {
                    ex.addSuppressed(e);
                }
            } catch (Exception e) {
                var io = new IOException(e);
                if (ex == null) {
                    ex = io;
                } else {
                    ex.addSuppressed(io);
                }
            }
        }
        if (ex != null) {
            throw ex;
        }
    }
}
