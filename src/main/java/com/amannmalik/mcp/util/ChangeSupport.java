package com.amannmalik.mcp.util;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class ChangeSupport<T> {
    private final List<Consumer<T>> listeners = new CopyOnWriteArrayList<>();

    public Closeable subscribe(Consumer<T> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public void notifyListeners(T change) {
        for (var l : listeners) l.accept(change);
    }
}
