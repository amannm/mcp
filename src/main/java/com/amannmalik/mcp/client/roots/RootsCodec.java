package com.amannmalik.mcp.client.roots;

import com.amannmalik.mcp.util.EmptyJsonObjectCodec;
import com.amannmalik.mcp.util.JsonUtil;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import java.util.Set;

import java.util.ArrayList;
import java.util.List;

public final class RootsCodec {
    private RootsCodec() {
    }

    public static JsonObject toJsonObject(ListRootsRequest req) {
        if (req == null) throw new IllegalArgumentException("request required");
        JsonObjectBuilder b = Json.createObjectBuilder();
        if (req._meta() != null) b.add("_meta", req._meta());
        return b.build();
    }

    public static ListRootsRequest toListRootsRequest(JsonObject obj) {
        if (obj != null) JsonUtil.requireOnlyKeys(obj, Set.of("_meta"));
        JsonObject meta = obj == null ? null : obj.getJsonObject("_meta");
        return new ListRootsRequest(meta);
    }

    public static JsonObject toJsonObject(ListRootsResult result) {
        JsonObjectBuilder b = Json.createObjectBuilder();
        b.add("roots", toJsonObject(result.roots()).getJsonArray("roots"));
        if (result._meta() != null) b.add("_meta", result._meta());
        return b.build();
    }

    public static JsonObject toJsonObject(RootsListChangedNotification n) {
        if (n == null) throw new IllegalArgumentException("notification required");
        return EmptyJsonObjectCodec.toJsonObject();
    }

    public static RootsListChangedNotification toRootsListChangedNotification(JsonObject obj) {
        EmptyJsonObjectCodec.requireEmpty(obj);
        return new RootsListChangedNotification();
    }

    public static JsonObject toJsonObject(Root root) {
        JsonObjectBuilder b = Json.createObjectBuilder().add("uri", root.uri());
        if (root.name() != null) b.add("name", root.name());
        if (root._meta() != null) b.add("_meta", root._meta());
        return b.build();
    }

    public static Root toRoot(JsonObject obj) {
        return new Root(
                obj.getString("uri"),
                obj.getString("name", null),
                obj.containsKey("_meta") ? obj.getJsonObject("_meta") : null
        );
    }

    public static JsonObject toJsonObject(List<Root> roots) {
        JsonArrayBuilder arr = Json.createArrayBuilder();
        for (Root r : roots) arr.add(toJsonObject(r));
        return Json.createObjectBuilder().add("roots", arr).build();
    }

    public static List<Root> toRoots(JsonObject obj) {
        var arr = obj.getJsonArray("roots");
        if (arr == null || arr.isEmpty()) return List.of();
        List<Root> list = new ArrayList<>(arr.size());
        arr.forEach(v -> list.add(toRoot(v.asJsonObject())));
        return List.copyOf(list);
    }
}
