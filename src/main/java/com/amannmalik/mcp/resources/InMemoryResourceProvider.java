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
        listeners.computeIfAbsent(uri, k -> new CopyOnWriteArrayList<>()).add(listener);
        return () -> listeners.getOrDefault(uri, List.of()).remove(listener);
    }

    @Override
    public Optional<Resource> get(URI uri) {
        for (var r : items) {
            if (r.uri().equals(uri)) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean supportsSubscribe() {
        return true;
    }

    public void notifyUpdate(URI uri) {
        String title = null;
        for (var r : items) {
            if (r.uri().equals(uri)) {
                title = r.title();
                break;
            }
        }
        var update = new ResourceUpdate(uri, title);
        listeners.getOrDefault(uri, List.of()).forEach(l -> l.accept(update));
    }

    public void addResource(Resource resource, ResourceBlock content) {
        if (resource == null) {
            throw new IllegalArgumentException("resource required");
        }
        for (var r : items) {
            if (r.uri().equals(resource.uri())) {
                throw new IllegalArgumentException("duplicate resource uri: " + resource.uri());
            }
        }
        items.add(resource);
        if (content != null) contents.put(resource.uri(), content);
        notifyListChanged();
    }

    public void removeResource(String uri) {
        items.removeIf(r -> r.uri().equals(uri));
        contents.remove(uri);
        notifyListChanged();
    }

    public void addTemplate(ResourceTemplate template) {
        if (template == null) {
            throw new IllegalArgumentException("template required");
        }
        for (var t : templates) {
            if (t.name().equals(template.name())) {
                throw new IllegalArgumentException("duplicate template name: " + template.name());
            }
        }
        templates.add(template);
        notifyListChanged();
    }

    public void removeTemplate(String name) {
        templates.removeIf(t -> t.name().equals(name));
        notifyListChanged();
    }
}
