package com.amannmalik.mcp.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ListChangeSupport<L extends ListChangeListener> {
    private final List<L> listeners = new CopyOnWriteArrayList<>();

    public ListChangeSubscription subscribe(L listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public void notifyListeners() {
        for (L l : listeners) {
            l.listChanged();
        }
    }
}
