package com.amannmalik.mcp.prompts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A template used to generate prompt instances. Placeholders in messages
 * use curly braces, e.g. {code}.
 */
public record PromptTemplate(Prompt prompt, List<PromptMessageTemplate> messages) {
    public PromptTemplate {
        messages = messages == null || messages.isEmpty() ? List.of() : List.copyOf(messages);
    }

    PromptInstance instantiate(Map<String, String> args) {
        List<PromptMessage> list = new ArrayList<>();
        for (PromptMessageTemplate t : messages) {
            list.add(new PromptMessage(t.role(), substitute(t.template(), args)));
        }
        return new PromptInstance(prompt.description(), list);
    }

    private static String substitute(String template, Map<String, String> args) {
        String result = template;
        if (args != null) {
            for (Map.Entry<String, String> e : args.entrySet()) {
                result = result.replace("{" + e.getKey() + "}", e.getValue());
            }
        }
        return result;
    }
}
