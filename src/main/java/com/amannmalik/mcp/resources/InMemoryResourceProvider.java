package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.api.*;
import com.amannmalik.mcp.core.InMemoryProvider;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class InMemoryResourceProvider extends InMemoryProvider<Resource> implements ResourceProvider {
    private final Map<String, ResourceBlock> contents;
    private final List<ResourceTemplate> templates;
    private final Map<String, List<Consumer<ResourceUpdate>>> listeners = new ConcurrentHashMap<>();

    public InMemoryResourceProvider(List<Resource> resources,
                                    Map<String, ResourceBlock> contents,
                                    List<ResourceTemplate> templates) {
        super(resources);
        this.contents = contents == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(contents);
        this.templates = templates == null ? new CopyOnWriteArrayList<>() : new CopyOnWriteArrayList<>(templates);
    }

    @Override
    public ResourceBlock read(String uri) {
        return contents.get(uri);
    }

    @Override
    public Pagination.Page<ResourceTemplate> listTemplates(String cursor) {
        return Pagination.page(templates, cursor, Pagination.DEFAULT_PAGE_SIZE);
    }

    @Override
    public ChangeSubscription subscribe(String uri, Consumer<ResourceUpdate> listener) {
        listeners.computeIfAbsent(uri, k -> new CopyOnWriteArrayList<>()).add(listener);
        return () -> listeners.getOrDefault(uri, List.of()).remove(listener);
    }

    @Override
    public Optional<Resource> get(String uri) {
        for (Resource r : items) {
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

    public void notifyUpdate(String uri) {
        String title = null;
        for (Resource r : items) {
            if (r.uri().equals(uri)) {
                title = r.title();
                break;
            }
        }
        ResourceUpdate update = new ResourceUpdate(uri, title);
        listeners.getOrDefault(uri, List.of()).forEach(l -> l.accept(update));
    }

    public void addResource(Resource resource, ResourceBlock content) {
        if (resource == null) {
            throw new IllegalArgumentException("resource required");
        }
        for (Resource r : items) {
            if (r.uri().equals(resource.uri())) {
                throw new IllegalArgumentException("duplicate resource uri: " + resource.uri());
            }
        }
        items.add(resource);
        if (content != null) contents.put(resource.uri(), content);
        notifyListeners();
    }

    public void removeResource(String uri) {
        items.removeIf(r -> r.uri().equals(uri));
        contents.remove(uri);
        notifyListeners();
    }

    public void addTemplate(ResourceTemplate template) {
        if (template == null) {
            throw new IllegalArgumentException("template required");
        }
        for (ResourceTemplate t : templates) {
            if (t.name().equals(template.name())) {
                throw new IllegalArgumentException("duplicate template name: " + template.name());
            }
        }
        templates.add(template);
        notifyListeners();
    }

    public void removeTemplate(String name) {
        templates.removeIf(t -> t.name().equals(name));
        notifyListeners();
    }
}
