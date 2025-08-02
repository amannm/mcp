package com.amannmalik.mcp.util;

import jakarta.json.*;

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
        if (obj == null) return new PaginatedRequest(null, null);
        JsonUtil.requireOnlyKeys(obj, REQUEST_KEYS);
        return new PaginatedRequest(obj.getString("cursor", null), obj.getJsonObject("_meta"));
    }

    public static JsonObject toJsonObject(PaginatedResult result) {
        JsonObjectBuilder b = Json.createObjectBuilder();
        if (result.nextCursor() != null) b.add("nextCursor", result.nextCursor());
        if (result._meta() != null) b.add("_meta", result._meta());
        return b.build();
    }

    public static PaginatedResult toPaginatedResult(JsonObject obj) {
        if (obj == null) return new PaginatedResult(null, null);
        JsonUtil.requireOnlyKeys(obj, RESULT_KEYS);
        return new PaginatedResult(obj.getString("nextCursor", null), obj.getJsonObject("_meta"));
    }
}
