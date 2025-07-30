package com.amannmalik.mcp.util;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

import java.util.function.Function;

public final class PaginationJson {
    private PaginationJson() {
    }

    public static <T> JsonObject toJson(String itemsField,
                                        Pagination.Page<T> page,
                                        Function<T, JsonValue> encoder) {
        JsonArrayBuilder arr = Json.createArrayBuilder();
        page.items().forEach(item -> arr.add(encoder.apply(item)));
        JsonObjectBuilder b = Json.createObjectBuilder().add(itemsField, arr.build());
        PaginationCodec.toJsonObject(new PaginatedResult(page.nextCursor()))
                .forEach(b::add);
        return b.build();
    }
}
