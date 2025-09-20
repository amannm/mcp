package com.amannmalik.mcp.util;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public final class EventSupport {
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    public AutoCloseable subscribe(Runnable listener) {
        var subscription = new Subscription(Objects.requireNonNull(listener, "listener"));
        listeners.add(listener);
        return subscription;
    }

    public void notifyListeners() {
        for (var l : listeners) {
            l.run();
        }
    }

    private final class Subscription implements AutoCloseable {
        private final Runnable listener;
        private final AtomicBoolean closed = new AtomicBoolean();

        private Subscription(Runnable listener) {
            this.listener = listener;
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                listeners.remove(listener);
            }
        }
    }
}
