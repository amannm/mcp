package com.amannmalik.mcp.util;

import java.util.Base64;
import java.util.List;

public final class Pagination {
    private Pagination() {
    }

    public static <T> Page<T> page(List<T> items, String cursor, int size) {
        return page(items, cursor, size, -1L);
    }

    public static <T> Page<T> page(List<T> items, String cursor, int size, long version) {
        Token t = decode(cursor);
        if (version >= 0 && t.version >= 0 && t.version != version) {
            throw new IllegalArgumentException("Invalid cursor");
        }
        int start = t.index;
        if (start < 0 || start > items.size()) throw new IllegalArgumentException("Invalid cursor");
        int end = Math.min(items.size(), start + size);
        List<T> slice = items.subList(start, end);
        String next = end < items.size() ? encode(version, end) : null;
        return new Page<>(List.copyOf(slice), next);
    }

    private static String encode(long version, int index) {
        String raw = version >= 0 ? version + ":" + index : Integer.toString(index);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes());
    }

    private static Token decode(String cursor) {
        if (cursor == null) return new Token(-1L, 0);
        try {
            String s = new String(Base64.getUrlDecoder().decode(cursor));
            int colon = s.indexOf(':');
            if (colon < 0) return new Token(-1L, Integer.parseInt(s));
            long v = Long.parseLong(s.substring(0, colon));
            int idx = Integer.parseInt(s.substring(colon + 1));
            return new Token(v, idx);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid cursor");
        }
    }

    private record Token(long version, int index) {}

    public record Page<T>(List<T> items, String nextCursor) {
        public Page {
            items = items == null ? List.of() : List.copyOf(items);
        }

        @Override
        public List<T> items() {
            return List.copyOf(items);
        }
    }
}
