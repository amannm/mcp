package com.amannmalik.mcp.util;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import java.util.Set;

public final class PaginationCodec {
    private PaginationCodec() {
    }

    private static final Set<String> REQUEST_KEYS = Set.of("cursor", "_meta");
    private static final Set<String> RESULT_KEYS = Set.of("nextCursor", "_meta");

    public static JsonObject toJsonObject(PaginatedRequest req) {
        JsonObjectBuilder b = Json.createObjectBuilder();
        if (req.cursor() != null) b.add("cursor", req.cursor());
        if (req._meta() != null) b.add("_meta", req._meta());
        return b.build();
    }

    public static PaginatedRequest toPaginatedRequest(JsonObject obj) {
        if (obj != null) JsonUtil.requireOnlyKeys(obj, REQUEST_KEYS);
        String cursor = obj == null ? null : obj.getString("cursor", null);
        JsonObject meta = obj == null ? null : obj.getJsonObject("_meta");
        return new PaginatedRequest(cursor, meta);
    }

    public static JsonObject toJsonObject(PaginatedResult result) {
        JsonObjectBuilder b = Json.createObjectBuilder();
        if (result.nextCursor() != null) b.add("nextCursor", result.nextCursor());
        if (result._meta() != null) b.add("_meta", result._meta());
        return b.build();
    }

    public static PaginatedResult toPaginatedResult(JsonObject obj) {
        if (obj != null) JsonUtil.requireOnlyKeys(obj, RESULT_KEYS);
        String cursor = obj == null ? null : obj.getString("nextCursor", null);
        JsonObject meta = obj == null ? null : obj.getJsonObject("_meta");
        return new PaginatedResult(cursor, meta);
    }
}
