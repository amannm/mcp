package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.api.McpHostConfiguration;
import com.amannmalik.mcp.util.*;

import java.util.List;

/// - [Pagination](specification/2025-06-18/server/utilities/pagination.mdx)
public final class Pagination {
    public static final int DEFAULT_PAGE_SIZE =
            McpHostConfiguration.defaultConfiguration().defaultPageSize();

    private Pagination() {
    }

    public static <T> Page<T> page(List<T> items, String cursor, int size) {
        int start = ValidationUtil.requireNonNegative(decode(cursor), "cursor");
        if (start > items.size()) throw new IllegalArgumentException("Invalid cursor");
        int end = Math.min(items.size(), start + size);
        List<T> slice = items.subList(start, end);
        Cursor next = end < items.size() ? Cursor.of(encode(end)) : Cursor.End.INSTANCE;
        return new Page<>(slice, next);
    }

    private static String encode(int index) {
        String raw = Integer.toString(index);
        return Base64Util.encodeUrl(raw.getBytes());
    }

    private static int decode(String cursor) {
        if (cursor == null) return 0;
        try {
            String s = new String(Base64Util.decodeUrl(cursor));
            return Integer.parseInt(s);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid cursor");
        }
    }

    public static String requireValidCursor(String cursor) {
        decode(cursor);
        return cursor;
    }

    public static String sanitize(String cursor) {
        return cursor == null ? null : requireValidCursor(cursor);
    }

    public record Page<T>(List<T> items, Cursor nextCursor) {
        public Page {
            items = Immutable.list(items);
            nextCursor = nextCursor == null ? Cursor.End.INSTANCE : nextCursor;
        }
    }
}
