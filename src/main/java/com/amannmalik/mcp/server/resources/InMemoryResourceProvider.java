package com.amannmalik.mcp.server.resources;

import com.amannmalik.mcp.util.ListChangeSupport;
import com.amannmalik.mcp.util.Pagination;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InMemoryResourceProvider implements ResourceProvider {
    private final List<Resource> resources;
    private final Map<String, ResourceBlock> contents;
    private final List<ResourceTemplate> templates;
    private final Map<String, List<ResourceListener>> listeners = new ConcurrentHashMap<>();
    private final ListChangeSupport<ResourceListListener> listChangeSupport = new ListChangeSupport<>();

    public InMemoryResourceProvider(List<Resource> resources, Map<String, ResourceBlock> contents, List<ResourceTemplate> templates) {
        this.resources = resources == null ? new CopyOnWriteArrayList<>() : new CopyOnWriteArrayList<>(resources);
        this.contents = contents == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(contents);
        this.templates = templates == null ? new CopyOnWriteArrayList<>() : new CopyOnWriteArrayList<>(templates);
    }

    @Override
    public Pagination.Page<Resource> list(String cursor) {
        return Pagination.page(resources, cursor, Pagination.DEFAULT_PAGE_SIZE);
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
    public ResourceSubscription subscribe(String uri, ResourceListener listener) {
        listeners.computeIfAbsent(uri, k -> new CopyOnWriteArrayList<>()).add(listener);
        return () -> listeners.getOrDefault(uri, List.of()).remove(listener);
    }

    @Override
    public java.util.Optional<Resource> get(String uri) {
        for (Resource r : resources) {
            if (r.uri().equals(uri)) {
                return java.util.Optional.of(r);
            }
        }
        return java.util.Optional.empty();
    }

    @Override
    public ResourceListSubscription subscribeList(ResourceListListener listener) {
        var sub = listChangeSupport.subscribe(listener);
        return sub::close;
    }

    @Override
    public boolean supportsSubscribe() {
        return true;
    }

    @Override
    public boolean supportsListChanged() {
        return true;
    }

    public void notifyUpdate(String uri) {
        String title = null;
        for (Resource r : resources) {
            if (r.uri().equals(uri)) {
                title = r.title();
                break;
            }
        }
        ResourceUpdate update = new ResourceUpdate(uri, title);
        listeners.getOrDefault(uri, List.of()).forEach(l -> l.updated(update));
    }

    public void addResource(Resource resource, ResourceBlock content) {
        if (resource == null) {
            throw new IllegalArgumentException("resource required");
        }
        for (Resource r : resources) {
            if (r.uri().equals(resource.uri())) {
                throw new IllegalArgumentException("duplicate resource uri: " + resource.uri());
            }
        }
        resources.add(resource);
        if (content != null) contents.put(resource.uri(), content);
        notifyListListeners();
    }

    public void removeResource(String uri) {
        resources.removeIf(r -> r.uri().equals(uri));
        contents.remove(uri);
        notifyListListeners();
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
        notifyListListeners();
    }

    public void removeTemplate(String name) {
        templates.removeIf(t -> t.name().equals(name));
        notifyListListeners();
    }

    private void notifyListListeners() {
        listChangeSupport.notifyListeners();
    }
}
