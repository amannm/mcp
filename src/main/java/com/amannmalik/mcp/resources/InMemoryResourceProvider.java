package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.core.InMemoryProvider;
import com.amannmalik.mcp.spi.*;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class InMemoryResourceProvider extends InMemoryProvider<Resource> implements ResourceProvider {
    private final Map<URI, ResourceBlock> contents;
    private final List<ResourceTemplate> templates;
    private final Map<URI, List<Consumer<ResourceUpdate>>> listeners = new ConcurrentHashMap<>();

    public InMemoryResourceProvider(List<Resource> resources,
                                    Map<URI, ResourceBlock> contents,
                                    List<ResourceTemplate> templates) {
        super(resources);
        this.contents = contents == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(contents);
        this.templates = templates == null ? new CopyOnWriteArrayList<>() : new CopyOnWriteArrayList<>(templates);
    }

    @Override
    public ResourceBlock read(URI uri) {
        return contents.get(uri);
    }

    @Override
    public Pagination.Page<ResourceTemplate> listTemplates(Cursor cursor) {
        return Pagination.page(templates, cursor == null ? Cursor.Start.INSTANCE : cursor, Pagination.DEFAULT_PAGE_SIZE);
    }

    @Override
    public AutoCloseable subscribe(URI uri, Consumer<ResourceUpdate> listener) {
        Objects.requireNonNull(uri, "uri");
        Objects.requireNonNull(listener, "listener");

        var listenersForUri = listeners.computeIfAbsent(uri, k -> new CopyOnWriteArrayList<>());
        listenersForUri.add(listener);
        return () -> removeListener(uri, listener);
    }

    @Override
    public Optional<Resource> get(URI uri) {
        return findResource(uri);
    }

    @Override
    public boolean supportsSubscribe() {
        return true;
    }

    public void notifyUpdate(URI uri) {
        Objects.requireNonNull(uri, "uri");

        var listenersForUri = listeners.get(uri);
        if (listenersForUri == null || listenersForUri.isEmpty()) {
            return;
        }

        var title = findResource(uri).map(Resource::title).orElse(null);
        var update = new ResourceUpdate(uri, title);
        listenersForUri.forEach(listener -> listener.accept(update));
    }

    public void addResource(Resource resource, ResourceBlock content) {
        Objects.requireNonNull(resource, "resource");

        var uri = resource.uri();
        if (findResource(uri).isPresent()) {
            throw new IllegalArgumentException("duplicate resource uri: " + uri);
        }
        items.add(resource);
        if (content != null) {
            contents.put(uri, content);
        }
        notifyListChanged();
    }

    public void removeResource(URI uri) {
        Objects.requireNonNull(uri, "uri");

        var removed = items.removeIf(r -> r.uri().equals(uri));
        contents.remove(uri);
        listeners.remove(uri);
        if (removed) {
            notifyListChanged();
        }
    }

    public void addTemplate(ResourceTemplate template) {
        Objects.requireNonNull(template, "template");

        if (templateExists(template.name())) {
            throw new IllegalArgumentException("duplicate template name: " + template.name());
        }
        templates.add(template);
        notifyListChanged();
    }

    public void removeTemplate(String name) {
        Objects.requireNonNull(name, "name");

        if (templates.removeIf(t -> t.name().equals(name))) {
            notifyListChanged();
        }
    }

    private Optional<Resource> findResource(URI uri) {
        Objects.requireNonNull(uri, "uri");

        for (var resource : items) {
            if (resource.uri().equals(uri)) {
                return Optional.of(resource);
            }
        }
        return Optional.empty();
    }

    private boolean templateExists(String name) {
        for (var template : templates) {
            if (template.name().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private void removeListener(URI uri, Consumer<ResourceUpdate> listener) {
        var listenersForUri = listeners.get(uri);
        if (listenersForUri == null) {
            return;
        }

        listenersForUri.remove(listener);
        if (listenersForUri.isEmpty()) {
            listeners.remove(uri, listenersForUri);
        }
    }
}
