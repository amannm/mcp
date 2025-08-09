package com.amannmalik.mcp.completion;

import com.amannmalik.mcp.transport.ExecutingProvider;
import com.amannmalik.mcp.util.Pagination;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.util.List;

/// - [Completion](specification/2025-06-18/server/utilities/completion.mdx)
public interface CompletionProvider extends ExecutingProvider<CompleteRequest.Ref, CompleteResult> {
    @Override
    default Pagination.Page<CompleteRequest.Ref> list(String cursor) {
        return new Pagination.Page<>(List.of(), null);
    }

    default CompleteResult complete(CompleteRequest request) throws InterruptedException {
        JsonObject ctx = request.context() == null
                ? Json.createObjectBuilder().build()
                : CompleteRequest.Context.CODEC.toJson(request.context());
        JsonObject args = Json.createObjectBuilder()
                .add("argument", CompleteRequest.Argument.CODEC.toJson(request.argument()))
                .add("context", ctx)
                .build();
        return execute(encode(request.ref()), args);
    }

    static String encode(CompleteRequest.Ref ref) {
        return switch (ref) {
            case CompleteRequest.Ref.PromptRef(var name, var _, var _) -> "prompt:" + name;
            case CompleteRequest.Ref.ResourceRef(var uri) -> "resource:" + uri;
        };
    }

    static CompleteRequest.Ref decode(String name) {
        if (name.startsWith("prompt:")) return new CompleteRequest.Ref.PromptRef(name.substring(7), null, null);
        if (name.startsWith("resource:")) return new CompleteRequest.Ref.ResourceRef(name.substring(9));
        throw new IllegalArgumentException("invalid ref");
    }
}
