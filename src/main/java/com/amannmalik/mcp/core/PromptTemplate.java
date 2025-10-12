package com.amannmalik.mcp.core;

import com.amannmalik.mcp.spi.*;

import java.util.List;
import java.util.Map;

public record PromptTemplate(Prompt prompt, List<PromptMessageTemplate> messages) {
    private static PromptContent instantiate(PromptContent tmpl, Map<String, String> args) {
        return switch (tmpl) {
            case ContentBlock.Text t -> new ContentBlock.Text(substitute(t.text(), args), t.annotations(), t._meta());
            case ContentBlock.Image i -> new ContentBlock.Image(i.data(), i.mimeType(), i.annotations(), i._meta());
            case ContentBlock.Audio a -> new ContentBlock.Audio(a.data(), a.mimeType(), a.annotations(), a._meta());
            case ContentBlock.EmbeddedResource r -> new ContentBlock.EmbeddedResource(r.resource(), r.annotations(), r._meta());
            case ContentBlock.ResourceLink l -> new ContentBlock.ResourceLink(l.resource());
        };
    }

    private static String substitute(String template, Map<String, String> args) {
        var result = template;
        for (var e : args.entrySet()) {
            result = result.replace("{" + e.getKey() + "}", e.getValue());
        }
        return result;
    }

    @Override
    public List<PromptMessageTemplate> messages() {
        return List.copyOf(messages);
    }
}
