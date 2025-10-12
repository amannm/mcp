package com.amannmalik.mcp.test.impl;

import com.amannmalik.mcp.spi.*;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DefaultPromptProvider implements PromptProvider {
    private final List<Prompt> prompts = new CopyOnWriteArrayList<>(DefaultServerFixtures.PROMPTS);
    private final Map<String, PromptInstance> instances = new HashMap<>(DefaultServerFixtures.PROMPT_INSTANCES);
    private final List<Runnable> listChangedListeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean changeSimulationStarted = new AtomicBoolean();

    @Override
    public Pagination.Page<Prompt> list(Cursor cursor) {
        prompts.sort(Comparator.comparing(Prompt::name));
        return Pagination.page(prompts, cursor == null ? Cursor.Start.INSTANCE : cursor, Pagination.DEFAULT_PAGE_SIZE);
    }

    @Override
    public Closeable onListChanged(Runnable listener) {
        Objects.requireNonNull(listener, "listener");
        listChangedListeners.add(listener);
        startSimulation();
        return () -> listChangedListeners.remove(listener);
    }

    @Override
    public boolean supportsListChanged() {
        return true;
    }

    @Override
    public void close() {
        listChangedListeners.clear();
        prompts.clear();
        instances.clear();
        changeSimulationStarted.set(false);
    }

    @Override
    public Optional<Prompt> find(String name) {
        Objects.requireNonNull(name, "name");
        return prompts.stream().filter(p -> p.name().equals(name)).findFirst();
    }

    @Override
    public PromptInstance get(String name, Map<String, String> arguments) {
        Objects.requireNonNull(name, "name");
        var instance = instances.get(name);
        if (instance == null) {
            throw new IllegalArgumentException("unknown prompt: " + name);
        }
        return instance;
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
                    listChangedListeners.forEach(Runnable::run);
                }
            });
        }
    }
}
