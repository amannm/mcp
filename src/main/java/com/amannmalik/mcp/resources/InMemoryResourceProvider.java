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
    private final Map<URI, Resource> resourcesByUri;
    private final List<ResourceTemplate> templates;
    private final Map<String, ResourceTemplate> templatesByName;
    private final Map<URI, List<Consumer<ResourceUpdate>>> listeners = new ConcurrentHashMap<>();

    public InMemoryResourceProvider(List<Resource> resources,
                                    Map<URI, ResourceBlock> contents,
                                    List<ResourceTemplate> templates) {
        super(resources);
        this.contents = contents == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(contents);
        this.resourcesByUri = new ConcurrentHashMap<>();
        for (var resource : items) {
            registerInitialResource(resource);
        }
        this.templates = templates == null ? new CopyOnWriteArrayList<>() : new CopyOnWriteArrayList<>(templates);
        this.templatesByName = new ConcurrentHashMap<>();
        for (var template : this.templates) {
            registerInitialTemplate(template);
        }
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
        Objects.requireNonNull(uri, "uri");
        return Optional.ofNullable(resourcesByUri.get(uri));
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

        var resource = resourcesByUri.get(uri);
        var title = resource == null ? null : resource.title();
        var update = new ResourceUpdate(uri, title);
        listenersForUri.forEach(listener -> listener.accept(update));
    }

    public void addResource(Resource resource, ResourceBlock content) {
        Objects.requireNonNull(resource, "resource");

        var uri = resource.uri();
        if (resourcesByUri.putIfAbsent(uri, resource) != null) {
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

        var removed = resourcesByUri.remove(uri);
        contents.remove(uri);
        listeners.remove(uri);
        if (removed != null) {
            if (!items.remove(removed)) {
                resourcesByUri.putIfAbsent(uri, removed);
                throw new IllegalStateException("Failed to remove resource: " + uri);
            }
            notifyListChanged();
        }
    }

    public void addTemplate(ResourceTemplate template) {
        Objects.requireNonNull(template, "template");

        var name = template.name();
        if (templatesByName.putIfAbsent(name, template) != null) {
            throw new IllegalArgumentException("duplicate template name: " + name);
        }
        templates.add(template);
        notifyListChanged();
    }

    public void removeTemplate(String name) {
        Objects.requireNonNull(name, "name");

        var removed = templatesByName.remove(name);
        if (removed != null && templates.remove(removed)) {
            notifyListChanged();
        }
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

    private void registerInitialResource(Resource resource) {
        Objects.requireNonNull(resource, "resource");
        var previous = resourcesByUri.putIfAbsent(resource.uri(), resource);
        if (previous != null) {
            throw new IllegalArgumentException("duplicate resource uri: " + resource.uri());
        }
    }

    private void registerInitialTemplate(ResourceTemplate template) {
        Objects.requireNonNull(template, "template");
        var previous = templatesByName.putIfAbsent(template.name(), template);
        if (previous != null) {
            throw new IllegalArgumentException("duplicate template name: " + template.name());
        }
    }
}
