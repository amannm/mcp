package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.util.Pagination;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public final class InMemoryPromptProvider implements PromptProvider {
    private final Map<String, PromptTemplate> templates = new ConcurrentHashMap<>();
    private final List<PromptsListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicLong version = new AtomicLong();

    public void add(PromptTemplate template) {
        templates.put(template.prompt().name(), template);
        version.incrementAndGet();
        notifyListeners();
    }

    public void remove(String name) {
        templates.remove(name);
        version.incrementAndGet();
        notifyListeners();
    }

    @Override
    public PromptPage list(String cursor) {
        List<Prompt> all = new ArrayList<>();
        for (PromptTemplate t : templates.values()) {
            all.add(t.prompt());
        }
        all.sort(Comparator.comparing(Prompt::name));
        long v = version.get();
        Pagination.Page<Prompt> page = Pagination.page(all, cursor, 100, v);
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
