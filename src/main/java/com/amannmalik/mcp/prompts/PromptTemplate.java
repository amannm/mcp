package com.amannmalik.mcp.prompts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public record PromptTemplate(Prompt prompt, List<PromptMessageTemplate> messages) {
    public PromptTemplate {
        messages = messages == null || messages.isEmpty() ? List.of() : List.copyOf(messages);
    }

    PromptInstance instantiate(Map<String, String> args) {
        List<PromptMessage> list = new ArrayList<>();
        for (PromptMessageTemplate t : messages) {
            list.add(new PromptMessage(t.role(), instantiate(t.content(), args)));
        }
        return new PromptInstance(prompt.description(), list);
    }

    private static PromptContent instantiate(PromptContent tmpl, Map<String, String> args) {
        return switch (tmpl) {
            case PromptContent.Text t -> new PromptContent.Text(substitute(t.text(), args), t.annotations());
            case PromptContent.Image i -> new PromptContent.Image(i.data(), i.mimeType(), i.annotations());
            case PromptContent.Audio a -> new PromptContent.Audio(a.data(), a.mimeType(), a.annotations());
            case PromptContent.EmbeddedResource r -> new PromptContent.EmbeddedResource(r.resource(), r.annotations());
            case PromptContent.ResourceLink l -> new PromptContent.ResourceLink(l.resource());
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
