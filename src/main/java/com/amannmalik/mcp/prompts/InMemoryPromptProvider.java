package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.util.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryPromptProvider implements PromptProvider {
    private final Map<String, PromptTemplate> templates = new ConcurrentHashMap<>();
    private final ChangeSupport<Void> listChangeSupport = new ChangeSupport<>();

    public void add(PromptTemplate template) {
        String name = template.prompt().name();
        if (templates.putIfAbsent(name, template) != null) {
            throw new IllegalArgumentException("duplicate prompt name: " + name);
        }
        notifyListeners();
    }

    public void remove(String name) {
        templates.remove(name);
        notifyListeners();
    }

    @Override
    public Pagination.Page<Prompt> list(String cursor) {
        List<Prompt> all = new ArrayList<>();
        for (PromptTemplate t : templates.values()) {
            all.add(t.prompt());
        }
        all.sort(Comparator.comparing(Prompt::name));
        return Pagination.page(all, cursor, Pagination.DEFAULT_PAGE_SIZE);
    }

    @Override
    public PromptInstance get(String name, Map<String, String> arguments) {
        PromptTemplate tmpl = templates.get(name);
        if (tmpl == null) throw new IllegalArgumentException("unknown prompt: " + name);
        return tmpl.instantiate(arguments);
    }

    @Override
    public ChangeSubscription subscribeList(ChangeListener<Void> listener) {
        return listChangeSupport.subscribe(listener);
    }

    @Override
    public boolean supportsListChanged() {
        return true;
    }

    private void notifyListeners() {
        listChangeSupport.notifyListeners(null);
    }
}
