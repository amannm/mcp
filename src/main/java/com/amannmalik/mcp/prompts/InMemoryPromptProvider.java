package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.api.*;
import com.amannmalik.mcp.api.PromptInstance;
import com.amannmalik.mcp.api.PromptProvider;
import com.amannmalik.mcp.core.InMemoryProvider;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryPromptProvider extends InMemoryProvider<Prompt> implements PromptProvider {
    private final Map<String, PromptTemplate> templates = new ConcurrentHashMap<>();

    public void add(PromptTemplate template) {
        String name = template.prompt().name();
        if (templates.putIfAbsent(name, template) != null) {
            throw new IllegalArgumentException("duplicate prompt name: " + name);
        }
        items.add(template.prompt());
        notifyListeners();
    }

    public void remove(String name) {
        templates.remove(name);
        items.removeIf(p -> p.name().equals(name));
        notifyListeners();
    }

    @Override
    public Pagination.Page<Prompt> list(String cursor) {
        items.sort(Comparator.comparing(Prompt::name));
        return super.list(cursor);
    }

    @Override
    public PromptInstance get(String name, Map<String, String> arguments) {
        PromptTemplate tmpl = templates.get(name);
        if (tmpl == null) throw new IllegalArgumentException("unknown prompt: " + name);
        return tmpl.instantiate(arguments);
    }
}
