package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.util.Immutable;
import com.amannmalik.mcp.util.PaginatedResult;
import com.amannmalik.mcp.util.Pagination;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.*;

import java.util.ArrayList;
import java.util.List;

public record ListPromptsResult(List<Prompt> prompts,
                                String nextCursor,
                                JsonObject _meta) {
    public static final JsonCodec<ListPromptsResult> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(ListPromptsResult result) {
            return AbstractEntityCodec.paginated(
                    "prompts",
                    new Pagination.Page<>(result.prompts(), result.nextCursor()),
                    Prompt.CODEC::toJson,
                    result._meta());
        }

        @Override
        public ListPromptsResult fromJson(JsonObject obj) {
            Pagination.Page<Prompt> page = promptPage(obj);
            PaginatedResult pr = AbstractEntityCodec.fromPaginatedResult(obj);
            return new ListPromptsResult(page.items(), page.nextCursor(), pr._meta());
        }

        private Pagination.Page<Prompt> promptPage(JsonObject obj) {
            if (obj == null) throw new IllegalArgumentException("object required");
            JsonArray arr = obj.getJsonArray("prompts");
            if (arr == null) throw new IllegalArgumentException("prompts required");
            List<Prompt> ps = new ArrayList<>();
            for (JsonValue v : arr) {
                if (v.getValueType() != JsonValue.ValueType.OBJECT) {
                    throw new IllegalArgumentException("prompt must be object");
                }
                ps.add(Prompt.CODEC.fromJson(v.asJsonObject()));
            }
            String cursor = AbstractEntityCodec.fromPaginatedResult(obj).nextCursor();
            return new Pagination.Page<>(ps, cursor);
        }
    };

    public ListPromptsResult {
        prompts = Immutable.list(prompts);
        MetaValidator.requireValid(_meta);
    }
}
