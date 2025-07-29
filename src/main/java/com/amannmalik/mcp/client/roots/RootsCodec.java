package com.amannmalik.mcp.client.roots;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import java.util.ArrayList;
import java.util.List;


public final class RootsCodec {
    private RootsCodec() {}

    public static JsonObject toJsonObject(ListRootsRequest req) {
        return Json.createObjectBuilder().build();
    }

    public static ListRootsRequest toListRootsRequest(JsonObject obj) {
        return new ListRootsRequest();
    }

    public static JsonObject toJsonObject(ListRootsResponse resp) {
        return toJsonObject(resp.roots());
    }

    public static ListRootsResponse toListRootsResponse(JsonObject obj) {
        return new ListRootsResponse(toRoots(obj));
    }

    public static JsonObject toJsonObject(RootsListChangedNotification n) {
        return Json.createObjectBuilder().build();
    }

    public static RootsListChangedNotification toRootsListChangedNotification(JsonObject obj) {
        return new RootsListChangedNotification();
    }

    public static JsonObject toJsonObject(Root root) {
        JsonObjectBuilder b = Json.createObjectBuilder().add("uri", root.uri());
        if (root.name() != null) b.add("name", root.name());
        return b.build();
    }

    public static Root toRoot(JsonObject obj) {
        return new Root(obj.getString("uri"), obj.getString("name", null));
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
