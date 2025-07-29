package com.amannmalik.mcp.server.resources;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import com.amannmalik.mcp.util.Pagination;

public final class InMemoryResourceProvider implements ResourceProvider {
    private final List<Resource> resources;
    private final Map<String, ResourceBlock> contents;
    private final List<ResourceTemplate> templates;
    private final Map<String, List<ResourceListener>> listeners = new ConcurrentHashMap<>();

    public InMemoryResourceProvider(List<Resource> resources, Map<String, ResourceBlock> contents, List<ResourceTemplate> templates) {
        this.resources = resources == null ? List.of() : List.copyOf(resources);
        this.contents = contents == null ? Map.of() : Map.copyOf(contents);
        this.templates = templates == null ? List.of() : List.copyOf(templates);
    }

    @Override
    public ResourceList list(String cursor) {
        Pagination.Page<Resource> page = Pagination.page(resources, cursor, 100);
        return new ResourceList(page.items(), page.nextCursor());
    }

    @Override
    public ResourceBlock read(String uri) {
        return contents.get(uri);
    }

    @Override
    public ResourceTemplatePage listTemplates(String cursor) {
        Pagination.Page<ResourceTemplate> page = Pagination.page(templates, cursor, 100);
        return new ResourceTemplatePage(page.items(), page.nextCursor());
    }

    @Override
    public ResourceSubscription subscribe(String uri, ResourceListener listener) {
        listeners.computeIfAbsent(uri, k -> new CopyOnWriteArrayList<>()).add(listener);
        return () -> listeners.getOrDefault(uri, List.of()).remove(listener);
    }

    public void notifyUpdate(String uri, String title) {
        ResourceUpdate update = new ResourceUpdate(uri, title);
        listeners.getOrDefault(uri, List.of()).forEach(l -> l.updated(update));
    }
}
