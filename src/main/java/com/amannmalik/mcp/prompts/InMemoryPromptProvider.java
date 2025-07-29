package com.amannmalik.mcp.prompts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.amannmalik.mcp.util.Pagination;


public final class InMemoryPromptProvider implements PromptProvider {
    private final Map<String, PromptTemplate> templates = new ConcurrentHashMap<>();

    public void add(PromptTemplate template) {
        templates.put(template.prompt().name(), template);
    }

    @Override
    public PromptPage list(String cursor) {
        List<Prompt> all = new ArrayList<>();
        for (PromptTemplate t : templates.values()) {
            all.add(t.prompt());
        }
        Pagination.Page<Prompt> page = Pagination.page(all, cursor, 100);
        return new PromptPage(page.items(), page.nextCursor());
    }

    @Override
    public PromptInstance get(String name, Map<String, String> arguments) {
        PromptTemplate tmpl = templates.get(name);
        if (tmpl == null) throw new IllegalArgumentException("unknown prompt: " + name);
        return tmpl.instantiate(arguments);
    }
}
