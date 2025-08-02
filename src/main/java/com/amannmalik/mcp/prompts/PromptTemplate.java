package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.content.ContentBlock;

import java.util.*;

public record PromptTemplate(Prompt prompt, List<PromptMessageTemplate> messages) {
    public PromptTemplate {
        messages = messages == null || messages.isEmpty() ? List.of() : List.copyOf(messages);
    }

    PromptInstance instantiate(Map<String, String> args) {
        Map<String, String> provided = args == null ? Map.of() : Map.copyOf(args);

        if (!prompt.arguments().isEmpty()) {
            java.util.Set<String> allowed = prompt.arguments().stream()
                    .map(PromptArgument::name)
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());

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
        if (args != null) {
            for (Map.Entry<String, String> e : args.entrySet()) {
                result = result.replace("{" + e.getKey() + "}", e.getValue());
            }
        }
        return result;
    }
}
