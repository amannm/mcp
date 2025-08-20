package com.amannmalik.mcp.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class EventSupport {
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    public AutoCloseable subscribe(Runnable listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public void notifyListeners() {
        for (var l : listeners) {
            l.run();
        }
    }
}
