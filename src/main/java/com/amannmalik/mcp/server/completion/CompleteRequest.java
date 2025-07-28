package com.amannmalik.mcp.server.completion;

import java.util.Map;

/** Request sent by the client to ask for completion options. */
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

    /** Which argument to complete. */
    public record Argument(String name, String value) {
        public Argument {
            if (name == null || value == null) {
                throw new IllegalArgumentException("name and value are required");
            }
        }
    }

    /** Additional completion context. */
    public record Context(Map<String, String> arguments) {
        public Context {
            arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
        }

        @Override
        public Map<String, String> arguments() {
            return Map.copyOf(arguments);
        }
    }

    /** Completion reference type. */
    public sealed interface Ref permits Ref.PromptRef, Ref.ResourceRef {
        String type();

        /** References a prompt by name. */
        record PromptRef(String name) implements Ref {
            public PromptRef {
                if (name == null) throw new IllegalArgumentException("name required");
            }

            @Override
            public String type() { return "ref/prompt"; }
        }

        /** References a resource URI or template. */
        record ResourceRef(String uri) implements Ref {
            public ResourceRef {
                if (uri == null) throw new IllegalArgumentException("uri required");
            }

            @Override
            public String type() { return "ref/resource"; }
        }
    }
}
