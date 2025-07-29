package com.amannmalik.mcp.server.resources;

import com.amannmalik.mcp.util.Pagination;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public final class InMemoryResourceProvider implements ResourceProvider {
    private final List<Resource> resources;
    private final Map<String, ResourceBlock> contents;
    private final List<ResourceTemplate> templates;
    private final Map<String, List<ResourceListener>> listeners = new ConcurrentHashMap<>();
    private final List<ResourceListListener> listListeners = new CopyOnWriteArrayList<>();
    private final AtomicLong version = new AtomicLong();

    public InMemoryResourceProvider(List<Resource> resources, Map<String, ResourceBlock> contents, List<ResourceTemplate> templates) {
        this.resources = resources == null ? new CopyOnWriteArrayList<>() : new CopyOnWriteArrayList<>(resources);
        this.contents = contents == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(contents);
        this.templates = templates == null ? new CopyOnWriteArrayList<>() : new CopyOnWriteArrayList<>(templates);
    }

    @Override
    public ResourceList list(String cursor) {
        long v = version.get();
        Pagination.Page<Resource> page = Pagination.page(resources, cursor, 100, v);
        return new ResourceList(page.items(), page.nextCursor());
    }

    @Override
    public ResourceBlock read(String uri) {
        return contents.get(uri);
    }

    @Override
    public ResourceTemplatePage listTemplates(String cursor) {
        long v = version.get();
        Pagination.Page<ResourceTemplate> page = Pagination.page(templates, cursor, 100, v);
        return new ResourceTemplatePage(page.items(), page.nextCursor());
    }

    @Override
    public ResourceSubscription subscribe(String uri, ResourceListener listener) {
        listeners.computeIfAbsent(uri, k -> new CopyOnWriteArrayList<>()).add(listener);
        return () -> listeners.getOrDefault(uri, List.of()).remove(listener);
    }

    @Override
    public ResourceListSubscription subscribeList(ResourceListListener listener) {
        listListeners.add(listener);
        return () -> listListeners.remove(listener);
    }

    public void notifyUpdate(String uri, String title) {
        ResourceUpdate update = new ResourceUpdate(uri, title);
        listeners.getOrDefault(uri, List.of()).forEach(l -> l.updated(update));
    }

    public void addResource(Resource resource, ResourceBlock content) {
        if (resource == null) {
            throw new IllegalArgumentException("resource required");
        }
        resources.add(resource);
        if (content != null) contents.put(resource.uri(), content);
        version.incrementAndGet();
        notifyListListeners();
    }

    public void removeResource(String uri) {
        resources.removeIf(r -> r.uri().equals(uri));
        contents.remove(uri);
        version.incrementAndGet();
        notifyListListeners();
    }

    public void addTemplate(ResourceTemplate template) {
        templates.add(template);
        version.incrementAndGet();
        notifyListListeners();
    }

    public void removeTemplate(String name) {
        templates.removeIf(t -> t.name().equals(name));
        version.incrementAndGet();
        notifyListListeners();
    }

    private void notifyListListeners() {
        listListeners.forEach(ResourceListListener::listChanged);
    }
}
