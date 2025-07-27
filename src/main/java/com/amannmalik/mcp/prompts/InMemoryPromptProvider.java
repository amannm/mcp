package com.amannmalik.mcp.prompts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Simple PromptProvider backed by in-memory templates. */
public final class InMemoryPromptProvider implements PromptProvider {
    private final Map<String, PromptTemplate> templates = new ConcurrentHashMap<>();

    public void add(PromptTemplate template) {
        templates.put(template.prompt().name(), template);
    }

    @Override
    public List<Prompt> listPrompts() {
        List<Prompt> list = new ArrayList<>();
        for (PromptTemplate t : templates.values()) {
            list.add(t.prompt());
        }
        return list;
    }

    @Override
    public PromptInstance getPrompt(String name, Map<String, String> arguments) {
        PromptTemplate tmpl = templates.get(name);
        if (tmpl == null) throw new IllegalArgumentException("unknown prompt: " + name);
        return tmpl.instantiate(arguments);
    }
}
