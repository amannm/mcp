package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.util.Pagination;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryPromptProvider implements PromptProvider {
    private final Map<String, PromptTemplate> templates = new ConcurrentHashMap<>();
    private final List<PromptsListener> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    public void add(PromptTemplate template) {
        templates.put(template.prompt().name(), template);
        notifyListeners();
    }

    public void remove(String name) {
        templates.remove(name);
        notifyListeners();
    }

    @Override
    public PromptPage list(String cursor) {
        List<Prompt> all = new ArrayList<>();
        for (PromptTemplate t : templates.values()) {
            all.add(t.prompt());
        }
        all.sort(java.util.Comparator.comparing(Prompt::name));
        Pagination.Page<Prompt> page = Pagination.page(all, cursor, 100);
        return new PromptPage(page.items(), page.nextCursor());
    }

    @Override
    public PromptInstance get(String name, Map<String, String> arguments) {
        PromptTemplate tmpl = templates.get(name);
        if (tmpl == null) throw new IllegalArgumentException("unknown prompt: " + name);
        return tmpl.instantiate(arguments);
    }

    @Override
    public PromptsSubscription subscribe(PromptsListener listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    private void notifyListeners() {
        listeners.forEach(PromptsListener::listChanged);
    }
}
