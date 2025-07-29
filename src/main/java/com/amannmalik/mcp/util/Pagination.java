package com.amannmalik.mcp.util;

import java.util.Base64;
import java.util.List;

public final class Pagination {
    private Pagination() {
    }

    public static <T> Page<T> page(List<T> items, String cursor, int size) {
        int start = decode(cursor);
        if (start < 0 || start > items.size()) throw new IllegalArgumentException("Invalid cursor");
        int end = Math.min(items.size(), start + size);
        List<T> slice = items.subList(start, end);
        String next = end < items.size() ? encode(end) : null;
        return new Page<>(List.copyOf(slice), next);
    }

    private static String encode(int index) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(Integer.toString(index).getBytes());
    }

    private static int decode(String cursor) {
        if (cursor == null) return 0;
        try {
            String s = new String(Base64.getUrlDecoder().decode(cursor));
            return Integer.parseInt(s);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid cursor");
        }
    }

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
