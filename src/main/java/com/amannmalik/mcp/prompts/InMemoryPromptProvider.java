package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.util.ListChangeSupport;
import com.amannmalik.mcp.util.Pagination;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryPromptProvider implements PromptProvider {
    private final Map<String, PromptTemplate> templates = new ConcurrentHashMap<>();
    private final ListChangeSupport<PromptsListener> listChangeSupport = new ListChangeSupport<>();

    public void add(PromptTemplate template) {
        templates.put(template.prompt().name(), template);
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
        return Pagination.page(all, cursor, 100);
    }

    @Override
    public PromptInstance get(String name, Map<String, String> arguments) {
        PromptTemplate tmpl = templates.get(name);
        if (tmpl == null) throw new IllegalArgumentException("unknown prompt: " + name);
        return tmpl.instantiate(arguments);
    }

    @Override
    public PromptsSubscription subscribe(PromptsListener listener) {
        var sub = listChangeSupport.subscribe(listener);
        return sub::close;
    }

    @Override
    public boolean supportsListChanged() {
        return true;
    }

    private void notifyListeners() {
        listChangeSupport.notifyListeners();
    }
}
