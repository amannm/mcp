package com.amannmalik.mcp.api;

import com.amannmalik.mcp.core.ExecutingProvider;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.util.List;

/// - [Completion](specification/2025-06-18/server/utilities/completion.mdx)
public interface CompletionProvider extends ExecutingProvider<Ref, CompleteResult> {
    @Override
    default Pagination.Page<Ref> list(String cursor) {
        return new Pagination.Page<>(List.of(), null);
    }

    default CompleteResult complete(CompleteRequest request) throws InterruptedException {
        JsonObject ctx = request.context() == null
                ? Json.createObjectBuilder().build()
                : Context.CODEC.toJson(request.context());
        JsonObject args = Json.createObjectBuilder()
                .add("argument", Argument.CODEC.toJson(request.argument()))
                .add("context", ctx)
                .build();
        return execute(encode(request.ref()), args);
    }

    static String encode(Ref ref) {
        return switch (ref) {
            case Ref.PromptRef(var name, var _, var _) -> "prompt:" + name;
            case Ref.ResourceRef(var uri) -> "resource:" + uri;
        };
    }

    static Ref decode(String name) {
        if (name.startsWith("prompt:")) return new Ref.PromptRef(name.substring(7), null, null);
        if (name.startsWith("resource:")) return new Ref.ResourceRef(name.substring(9));
        throw new IllegalArgumentException("invalid ref");
    }
}
