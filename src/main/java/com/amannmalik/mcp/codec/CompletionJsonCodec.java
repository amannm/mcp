package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.spi.Completion;
import jakarta.json.*;

class CompletionJsonCodec implements JsonCodec<Completion> {
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
}
