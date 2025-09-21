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

    public static <T> Set<T> set(Set<? extends T> items) {
        return Set.copyOf(Objects.requireNonNullElseGet(items, Set::of));
    }

    public static <E extends Enum<E>> Set<E> enumSet(Collection<? extends E> items) {
        return items == null || items.isEmpty() ? Set.of() : Set.copyOf(items);
    }
}
