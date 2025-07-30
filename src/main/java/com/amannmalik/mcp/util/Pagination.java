package com.amannmalik.mcp.util;

import java.util.List;

public final class Pagination {
    private Pagination() {
    }

    public static final int DEFAULT_PAGE_SIZE = 100;

    public static <T> Page<T> page(List<T> items, String cursor, int size) {
        int start = decode(cursor);
        if (start < 0 || start > items.size()) throw new IllegalArgumentException("Invalid cursor");
        int end = Math.min(items.size(), start + size);
        List<T> slice = items.subList(start, end);
        String next = end < items.size() ? encode(end) : null;
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

    /**
     * Validate that a cursor string can be decoded. Returns the original cursor
     * if valid to allow call chaining.
     */
    public static String requireValidCursor(String cursor) {
        decode(cursor);
        return cursor;
    }

    public record Page<T>(List<T> items, String nextCursor) {
        public Page {
            items = items == null ? List.of() : List.copyOf(items);
        }
    }
}
