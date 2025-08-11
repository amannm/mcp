package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.api.*;
import com.amannmalik.mcp.util.Immutable;

import java.util.*;
import java.util.stream.Collectors;

public record PromptTemplate(Prompt prompt, List<PromptMessageTemplate> messages) {
    public PromptTemplate {
        messages = Immutable.list(messages);
    }

    private static PromptContent instantiate(PromptContent tmpl, Map<String, String> args) {
        return switch (tmpl) {
            case ContentBlock.Text t -> new ContentBlock.Text(substitute(t.text(), args), t.annotations(), t._meta());
            case ContentBlock.Image i -> new ContentBlock.Image(i.data(), i.mimeType(), i.annotations(), i._meta());
            case ContentBlock.Audio a -> new ContentBlock.Audio(a.data(), a.mimeType(), a.annotations(), a._meta());
            case ContentBlock.EmbeddedResource r -> new ContentBlock.EmbeddedResource(r.resource(), r.annotations(), r._meta());
            case ContentBlock.ResourceLink l -> new ContentBlock.ResourceLink(l.resource());
            default -> tmpl;
        };
    }

    private static String substitute(String template, Map<String, String> args) {
        String result = template;
        for (Map.Entry<String, String> e : args.entrySet()) {
            result = result.replace("{" + e.getKey() + "}", e.getValue());
        }
        return result;
    }

    PromptInstance instantiate(Map<String, String> args) {
        Map<String, String> provided = Immutable.map(args);

        if (!prompt.arguments().isEmpty()) {
            Set<String> allowed = prompt.arguments().stream()
                    .map(PromptArgument::name)
                    .collect(Collectors.toUnmodifiableSet());

            for (String name : provided.keySet()) {
                if (!allowed.contains(name)) {
                    throw new IllegalArgumentException("unknown argument: " + name);
                }
            }

            for (PromptArgument a : prompt.arguments()) {
                if (a.required() && !provided.containsKey(a.name())) {
                    throw new IllegalArgumentException("missing argument: " + a.name());
                }
            }
        }

        List<PromptMessage> list = new ArrayList<>(messages.size());
        for (PromptMessageTemplate t : messages) {
            list.add(new PromptMessage(t.role(), instantiate(t.content(), provided)));
        }
        return new PromptInstance(prompt.description(), list);
    }
}
