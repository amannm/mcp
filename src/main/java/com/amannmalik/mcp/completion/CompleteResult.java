package com.amannmalik.mcp.completion;

import com.amannmalik.mcp.config.McpConfiguration;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.util.Immutable;
import com.amannmalik.mcp.validation.ValidationUtil;
import jakarta.json.*;

import java.util.List;

public record CompleteResult(Completion completion, JsonObject _meta) {
    public static final JsonCodec<CompleteResult> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(CompleteResult res) {
            JsonObjectBuilder b = Json.createObjectBuilder()
                    .add("completion", Completion.CODEC.toJson(res.completion()));
            if (res._meta() != null) b.add("_meta", res._meta());
            return b.build();
        }

        @Override
        public CompleteResult fromJson(JsonObject obj) {
            JsonObject compObj = obj.getJsonObject("completion");
            if (compObj == null) throw new IllegalArgumentException("completion required");
            Completion comp = Completion.CODEC.fromJson(compObj);
            JsonObject meta = obj.getJsonObject("_meta");
            return new CompleteResult(comp, meta);
        }
    };

    public static final int MAX_VALUES =
            McpConfiguration.current().performance().maxCompletionValues();

    public CompleteResult {
        if (completion == null) throw new IllegalArgumentException("completion required");
        ValidationUtil.requireMeta(_meta);
    }

    public record Completion(List<String> values, Integer total, Boolean hasMore) {
        public static final JsonCodec<Completion> CODEC = new JsonCodec<>() {
            @Override
            public JsonObject toJson(Completion c) {
                JsonArrayBuilder arr = Json.createArrayBuilder();
                c.values().forEach(arr::add);
                JsonObjectBuilder b = Json.createObjectBuilder().add("values", arr.build());
                if (c.total() != null) b.add("total", c.total());
                if (c.hasMore() != null) b.add("hasMore", c.hasMore());
                return b.build();
            }

            @Override
            public Completion fromJson(JsonObject obj) {
                JsonArray valuesArr = obj.getJsonArray("values");
                if (valuesArr == null) throw new IllegalArgumentException("values required");
                var values = valuesArr.getValuesAs(JsonString.class).stream()
                        .map(JsonString::getString)
                        .toList();
                Integer total = obj.containsKey("total") ? obj.getInt("total") : null;
                Boolean hasMore = obj.containsKey("hasMore") ? obj.getBoolean("hasMore") : null;
                return new Completion(values, total, hasMore);
            }
        };

        public Completion(List<String> values, Integer total, Boolean hasMore) {
            List<String> copy = Immutable.list(values).stream()
                    .map(ValidationUtil::requireClean)
                    .toList();
            this.values = copy;
            this.total = total == null ? null : ValidationUtil.requireNonNegative(total, "total");
            this.hasMore = hasMore;
            if (this.values.size() > MAX_VALUES) {
                throw new IllegalArgumentException("values must not exceed " + MAX_VALUES + " items");
            }
            if (this.total != null && this.total < this.values.size()) {
                throw new IllegalArgumentException("total must be >= values length");
            }
        }
    }
}
