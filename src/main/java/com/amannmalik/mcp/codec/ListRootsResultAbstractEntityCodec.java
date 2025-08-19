package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.spi.ListRootsResult;
import com.amannmalik.mcp.spi.Root;
import jakarta.json.*;

import java.util.List;
import java.util.Set;

public final class ListRootsResultAbstractEntityCodec extends AbstractEntityCodec<ListRootsResult> {

    static final JsonCodec<Root> CODEC = new RootAbstractEntityCodec();


    @Override
    public JsonObject toJson(ListRootsResult result) {
        JsonArrayBuilder arr = Json.createArrayBuilder();
        result.roots().forEach(r -> arr.add(CODEC.toJson(r)));
        return addMeta(Json.createObjectBuilder().add("roots", arr), result._meta()).build();
    }

    @Override
    public ListRootsResult fromJson(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        requireOnlyKeys(obj, Set.of("roots", "_meta"));
        JsonArray arr = obj.getJsonArray("roots");
        List<Root> roots = arr == null || arr.isEmpty()
                ? List.of()
                : arr.stream().map(JsonValue::asJsonObject).map(CODEC::fromJson).toList();
        return new ListRootsResult(roots, meta(obj));
    }
}
