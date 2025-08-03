package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.util.*;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.*;

import java.util.*;

public record ListPromptsResult(List<Prompt> prompts,
                                String nextCursor,
                                JsonObject _meta) {
    public static final JsonCodec<ListPromptsResult> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(ListPromptsResult r) {
            return AbstractEntityCodec.paginated(
                    "prompts",
                    new Pagination.Page<>(r.prompts(), r.nextCursor()),
                    Prompt.CODEC::toJson,
                    r._meta());
        }

        @Override
        public ListPromptsResult fromJson(JsonObject obj) {
            if (obj == null) throw new IllegalArgumentException("object required");
            JsonArray arr = obj.getJsonArray("prompts");
            if (arr == null) throw new IllegalArgumentException("prompts required");
            List<Prompt> prompts = new ArrayList<>();
            for (JsonValue v : arr) {
                if (v.getValueType() != JsonValue.ValueType.OBJECT) {
                    throw new IllegalArgumentException("prompt must be object");
                }
                prompts.add(Prompt.CODEC.fromJson(v.asJsonObject()));
            }
            PaginatedResult pr = AbstractEntityCodec.fromPaginatedResult(obj);
            return new ListPromptsResult(prompts, pr.nextCursor(), pr._meta());
        }
    };

    public ListPromptsResult {
        prompts = Immutable.list(prompts);
        MetaValidator.requireValid(_meta);
    }
}
