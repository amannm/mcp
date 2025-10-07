package com.amannmalik.mcp.test.impl;

import com.amannmalik.mcp.spi.*;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DefaultRootsProvider implements RootsProvider {
    private final CopyOnWriteArrayList<Root> roots;
    private final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean changeSimulationStarted = new AtomicBoolean();

    public DefaultRootsProvider() {
        this(List.of());
    }

    public DefaultRootsProvider(List<Root> initial) {
        roots = new CopyOnWriteArrayList<>(initial == null ? List.of() : List.copyOf(initial));
    }

    @Override
    public Pagination.Page<Root> list(Cursor cursor) {
        return Pagination.page(roots, cursor == null ? Cursor.Start.INSTANCE : cursor, Pagination.DEFAULT_PAGE_SIZE);
    }

    @Override
    public AutoCloseable onListChanged(Runnable listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
        startSimulation();
        return () -> listeners.remove(listener);
    }

    @Override
    public boolean supportsListChanged() {
        return true;
    }

    private void startSimulation() {
        if (changeSimulationStarted.compareAndSet(false, true)) {
            Thread.ofVirtual().start(() -> {
                for (int attempt = 0; attempt < 10; attempt++) {
                    try {
                        Thread.sleep(1_000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    for (var listener : listeners) {
                        listener.run();
                    }
                }
            });
        }
    }
}
