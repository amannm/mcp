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
        var arr = Json.createArrayBuilder();
        result.roots().forEach(r -> arr.add(CODEC.toJson(r)));
        var b = Json.createObjectBuilder().add("roots", arr);
        if (result._meta() != null) {
            b.add("_meta", result._meta());
        }
        return b.build();
    }

    @Override
    public ListRootsResult fromJson(JsonObject obj) {
        if (obj == null) {
            throw new IllegalArgumentException("object required");
        }
        requireOnlyKeys(obj, Set.of("roots", "_meta"));
        var arr = obj.getJsonArray("roots");
        var roots = arr == null || arr.isEmpty()
                ? List.<Root>of()
                : arr.stream().map(JsonValue::asJsonObject).map(CODEC::fromJson).toList();
        return new ListRootsResult(roots, getObject(obj, "_meta"));
    }
}
