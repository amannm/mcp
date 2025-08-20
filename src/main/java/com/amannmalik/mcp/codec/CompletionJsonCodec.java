package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.spi.Completion;
import jakarta.json.*;

class CompletionJsonCodec implements JsonCodec<Completion> {
    @Override
    public JsonObject toJson(Completion c) {
        var arr = Json.createArrayBuilder();
        c.values().forEach(arr::add);
        var b = Json.createObjectBuilder().add("values", arr.build());
        if (c.total() != null) {
            b.add("total", c.total());
        }
        if (c.hasMore() != null) {
            b.add("hasMore", c.hasMore());
        }
        return b.build();
    }

    @Override
    public Completion fromJson(JsonObject obj) {
        var valuesArr = obj.getJsonArray("values");
        if (valuesArr == null) {
            throw new IllegalArgumentException("values required");
        }
        var values = valuesArr.getValuesAs(JsonString.class).stream()
                .map(JsonString::getString)
                .toList();
        var total = obj.containsKey("total") ? obj.getInt("total") : null;
        var hasMore = obj.containsKey("hasMore") ? obj.getBoolean("hasMore") : null;
        return new Completion(values, total, hasMore);
    }
}
