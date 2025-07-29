package com.amannmalik.mcp.client.elicitation;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;


public final class ElicitationCodec {
    private ElicitationCodec() {
    }

    public static JsonObject toJsonObject(ElicitationRequest req) {
        return Json.createObjectBuilder()
                .add("message", req.message())
                .add("requestedSchema", req.requestedSchema())
                .build();
    }

    public static ElicitationRequest toRequest(JsonObject obj) {
        return new ElicitationRequest(
                obj.getString("message"),
                obj.getJsonObject("requestedSchema")
        );
    }

    public static JsonObject toJsonObject(ElicitationResponse resp) {
        JsonObjectBuilder b = Json.createObjectBuilder()
                .add("action", resp.action().name().toLowerCase());
        if (resp.content() != null) b.add("content", resp.content());
        return b.build();
    }

    public static ElicitationResponse toResponse(JsonObject obj) {
        String a = obj.getString("action");
        ElicitationAction action = ElicitationAction.valueOf(a.toUpperCase());
        JsonObject content = obj.getJsonObject("content");
        return new ElicitationResponse(action, content);
    }
}
