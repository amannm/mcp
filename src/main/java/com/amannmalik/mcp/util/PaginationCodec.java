package com.amannmalik.mcp.util;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;


public final class PaginationCodec {
    private PaginationCodec() {}

    public static JsonObject toJsonObject(PaginatedRequest req) {
        JsonObjectBuilder b = Json.createObjectBuilder();
        if (req.cursor() != null) b.add("cursor", req.cursor());
        return b.build();
    }

    public static PaginatedRequest toPaginatedRequest(JsonObject obj) {
        String cursor = obj == null ? null : obj.getString("cursor", null);
        return new PaginatedRequest(cursor);
    }

    public static JsonObject toJsonObject(PaginatedResult result) {
        JsonObjectBuilder b = Json.createObjectBuilder();
        if (result.nextCursor() != null) b.add("nextCursor", result.nextCursor());
        return b.build();
    }

    public static PaginatedResult toPaginatedResult(JsonObject obj) {
        String cursor = obj == null ? null : obj.getString("nextCursor", null);
        return new PaginatedResult(cursor);
    }
}
