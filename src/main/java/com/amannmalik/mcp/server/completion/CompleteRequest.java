package com.amannmalik.mcp.server.completion;

import com.amannmalik.mcp.validation.InputSanitizer;
import com.amannmalik.mcp.validation.MetaValidator;
import com.amannmalik.mcp.validation.UriTemplateValidator;
import jakarta.json.JsonObject;

import java.util.HashMap;
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
        public Argument(String name, String value) {
            if (name == null || value == null) {
                throw new IllegalArgumentException("name and value are required");
            }
            this.name = InputSanitizer.requireClean(name);
            this.value = InputSanitizer.requireClean(value);
        }
    }

    public record Context(Map<String, String> arguments) {
        public Context(Map<String, String> arguments) {
            if (arguments == null || arguments.isEmpty()) {
                this.arguments = Map.of();
            } else {
                Map<String, String> copy = new HashMap<>();
                arguments.forEach((k, v) -> {
                    copy.put(InputSanitizer.requireClean(k), InputSanitizer.requireClean(v));
                });
                this.arguments = Map.copyOf(copy);
            }
        }

        @Override
        public Map<String, String> arguments() {
            return Map.copyOf(arguments);
        }
    }

    public sealed interface Ref permits Ref.PromptRef, Ref.ResourceRef {
        String type();

        record PromptRef(String name, String title, JsonObject _meta) implements Ref {
            public PromptRef(String name, String title, JsonObject _meta) {
                if (name == null) throw new IllegalArgumentException("name required");
                this.name = InputSanitizer.requireClean(name);
                this.title = title == null ? null : InputSanitizer.requireClean(title);
                MetaValidator.requireValid(_meta);
                this._meta = _meta;
            }

            @Override
            public String type() {
                return "ref/prompt";
            }
        }

        record ResourceRef(String uri) implements Ref {
            public ResourceRef(String uri) {
                if (uri == null) throw new IllegalArgumentException("uri required");
                this.uri = UriTemplateValidator.requireAbsoluteTemplate(uri);
            }

            @Override
            public String type() {
                return "ref/resource";
            }
        }
    }
}
