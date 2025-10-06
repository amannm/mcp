package com.amannmalik.mcp.test.impl;

import com.amannmalik.mcp.spi.*;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class DefaultResourceProvider implements ResourceProvider {
    private final List<Resource> resources = new CopyOnWriteArrayList<>(DefaultServerFixtures.RESOURCES);
    private final Map<URI, Resource> byUri = new ConcurrentHashMap<>();
    private final Map<URI, ResourceBlock> content = new ConcurrentHashMap<>(DefaultServerFixtures.RESOURCE_CONTENT);
    private final List<ResourceTemplate> templates = new CopyOnWriteArrayList<>(DefaultServerFixtures.RESOURCE_TEMPLATES);
    private final Map<URI, CopyOnWriteArrayList<Consumer<ResourceUpdate>>> subscribers = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Runnable> listChangedListeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean changeSimulationStarted = new AtomicBoolean();

    public DefaultResourceProvider() {
        resources.forEach(r -> byUri.put(r.uri(), r));
    }

    @Override
    public ResourceBlock read(URI uri) {
        Objects.requireNonNull(uri, "uri");
        return content.get(uri);
    }

    @Override
    public Optional<Resource> get(URI uri) {
        Objects.requireNonNull(uri, "uri");
        return Optional.ofNullable(byUri.get(uri));
    }

    @Override
    public Pagination.Page<Resource> list(Cursor cursor) {
        return Pagination.page(resources, cursor == null ? Cursor.Start.INSTANCE : cursor, Pagination.DEFAULT_PAGE_SIZE);
    }

    @Override
    public Pagination.Page<ResourceTemplate> listTemplates(Cursor cursor) {
        return Pagination.page(templates, cursor == null ? Cursor.Start.INSTANCE : cursor, Pagination.DEFAULT_PAGE_SIZE);
    }

    @Override
    public AutoCloseable subscribe(URI uri, Consumer<ResourceUpdate> listener) {
        Objects.requireNonNull(uri, "uri");
        Objects.requireNonNull(listener, "listener");
        var listeners = subscribers.computeIfAbsent(uri, ignored -> new CopyOnWriteArrayList<>());
        listeners.add(listener);
        return () -> removeSubscriber(uri, listener);
    }

    @Override
    public boolean supportsSubscribe() {
        return true;
    }

    @Override
    public AutoCloseable onListChanged(Runnable listener) {
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
        subscribers.clear();
        changeSimulationStarted.set(false);
    }

    private void removeSubscriber(URI uri, Consumer<ResourceUpdate> listener) {
        var listeners = subscribers.get(uri);
        if (listeners == null) {
            return;
        }
        listeners.remove(listener);
        if (listeners.isEmpty()) {
            subscribers.remove(uri, listeners);
        }
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
