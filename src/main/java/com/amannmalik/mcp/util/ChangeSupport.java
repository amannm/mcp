package com.amannmalik.mcp.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ChangeSupport<T> {
    private final List<ChangeListener<T>> listeners = new CopyOnWriteArrayList<>();

    public ChangeSubscription subscribe(ChangeListener<T> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public void notifyListeners(T change) {
        for (var l : listeners) l.changed(change);
    }
}
