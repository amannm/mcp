package com.amannmalik.mcp.util;

import java.util.*;

public final class Immutable {
    private Immutable() {
    }

    public static <T> List<T> list(Collection<? extends T> items) {
        return List.copyOf(Objects.requireNonNullElseGet(items, List::of));
    }

    public static <K, V> Map<K, V> map(Map<? extends K, ? extends V> items) {
        return Map.copyOf(Objects.requireNonNullElseGet(items, Map::of));
    }
}
