package com.amannmalik.mcp.spi;

/// - [Completion](specification/2025-06-18/server/utilities/completion.mdx)
public non-sealed interface CompletionProvider extends ExecutingProvider<Ref, CompleteResult> {
    static String encode(Ref ref) {
        return switch (ref) {
            case Ref.PromptRef prompt -> "prompt:" + prompt.name();
            case Ref.ResourceRef resource -> "resource:" + resource.uri();
        };
    }

    static Ref decode(String name) {
        if (name.startsWith("prompt:")) {
            return new Ref.PromptRef(name.substring(7), null, null);
        }
        if (name.startsWith("resource:")) {
            return new Ref.ResourceRef(name.substring(9));
        }
        throw new IllegalArgumentException("invalid ref");
    }

    CompleteResult complete(CompleteRequest request) throws InterruptedException;
}
