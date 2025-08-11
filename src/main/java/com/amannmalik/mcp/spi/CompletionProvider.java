package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.codec.ArgumentJsonCodec;
import com.amannmalik.mcp.codec.ContextJsonCodec;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.util.List;

/// - [Completion](specification/2025-06-18/server/utilities/completion.mdx)
public non-sealed interface CompletionProvider extends ExecutingProvider<Ref, CompleteResult> {
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

    @Override
    default Pagination.Page<Ref> list(Cursor cursor) {
        return new Pagination.Page<>(List.of(), Cursor.End.INSTANCE);
    }

    default CompleteResult complete(CompleteRequest request) throws InterruptedException {
        JsonObject ctx = request.context() == null
                ? Json.createObjectBuilder().build()
                : new ContextJsonCodec().toJson(request.context());
        JsonObject args = Json.createObjectBuilder()
                .add("argument", new ArgumentJsonCodec().toJson(request.argument()))
                .add("context", ctx)
                .build();
        return execute(encode(request.ref()), args);
    }
}
