package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.spi.*;
import jakarta.json.*;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class ResourceEntityFieldCodec<I, R> extends AbstractEntityCodec<R> {
    private final String field;
    private final Function<R, Pagination.Page<I>> toPage;
    private final JsonCodec<I> itemCodec;
    private final Function<R, JsonObject> meta;
    private final String itemName;
    private final BiFunction<Pagination.Page<I>, JsonObject, R> from;

    public ResourceEntityFieldCodec(String field, Function<R, Pagination.Page<I>> toPage, JsonCodec<I> itemCodec, Function<R, JsonObject> meta, String itemName, BiFunction<Pagination.Page<I>, JsonObject, R> from) {
        this.field = field;
        this.toPage = toPage;
        this.itemCodec = itemCodec;
        this.meta = meta;
        this.itemName = itemName;
        this.from = from;
    }

    @Override
    public JsonObject toJson(R value) {
        return paginated(field, toPage.apply(value), itemCodec::toJson, meta.apply(value));
    }

    @Override
    public R fromJson(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        JsonArray arr = obj.getJsonArray(field);
        if (arr == null) throw new IllegalArgumentException(field + " required");
        List<I> items = new ArrayList<>();
        for (JsonValue v : arr) {
            if (v.getValueType() != JsonValue.ValueType.OBJECT) {
                throw new IllegalArgumentException(itemName + " must be object");
            }
            items.add(itemCodec.fromJson(v.asJsonObject()));
        }
        String next = obj.getString("nextCursor", null);
        JsonObject m = obj.getJsonObject("_meta");
        requireOnlyKeys(obj, Set.of(field, "nextCursor", "_meta"));
        return from.apply(new Pagination.Page<>(items, Cursor.of(next)), m);
    }
}
