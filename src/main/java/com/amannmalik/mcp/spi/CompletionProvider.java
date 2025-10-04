package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.codec.ArgumentJsonCodec;
import com.amannmalik.mcp.codec.ContextJsonCodec;
import jakarta.json.Json;

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

    default CompleteResult complete(CompleteRequest request) throws InterruptedException {
        var ctx = request.context() == null
                ? Json.createObjectBuilder().build()
                : new ContextJsonCodec().toJson(request.context());
        var args = Json.createObjectBuilder()
                .add("argument", new ArgumentJsonCodec().toJson(request.argument()))
                .add("context", ctx)
                .build();
        return execute(encode(request.ref()), args);
    }
}
