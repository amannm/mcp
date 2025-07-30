package com.amannmalik.mcp.server.completion;

import com.amannmalik.mcp.validation.InputSanitizer;
import com.amannmalik.mcp.validation.UriTemplateValidator;
import java.util.Map;

public record CompleteRequest(
        Ref ref,
        Argument argument,
        Context context
) {
    public CompleteRequest {
        if (ref == null || argument == null) {
            throw new IllegalArgumentException("ref and argument are required");
        }
    }

    public record Argument(String name, String value) {
        public Argument {
            if (name == null || value == null) {
                throw new IllegalArgumentException("name and value are required");
            }
            name = InputSanitizer.requireClean(name);
            value = InputSanitizer.requireClean(value);
        }
    }

    public record Context(Map<String, String> arguments) {
        public Context {
            if (arguments == null || arguments.isEmpty()) {
                arguments = Map.of();
            } else {
                Map<String, String> copy = new java.util.HashMap<>();
                arguments.forEach((k, v) -> {
                    copy.put(InputSanitizer.requireClean(k), InputSanitizer.requireClean(v));
                });
                arguments = Map.copyOf(copy);
            }
        }

        @Override
        public Map<String, String> arguments() {
            return Map.copyOf(arguments);
        }
    }

    public sealed interface Ref permits Ref.PromptRef, Ref.ResourceRef {
        String type();

        record PromptRef(String name) implements Ref {
            public PromptRef {
                if (name == null) throw new IllegalArgumentException("name required");
                name = InputSanitizer.requireClean(name);
            }

            @Override
            public String type() {
                return "ref/prompt";
            }
        }

        record ResourceRef(String uri) implements Ref {
            public ResourceRef {
                if (uri == null) throw new IllegalArgumentException("uri required");
                uri = InputSanitizer.requireClean(uri);
                uri = UriTemplateValidator.requireAbsoluteTemplate(uri);
            }

            @Override
            public String type() {
                return "ref/resource";
            }
        }
    }
}
