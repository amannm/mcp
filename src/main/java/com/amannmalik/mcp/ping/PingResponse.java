package com.amannmalik.mcp.ping;

import com.amannmalik.mcp.core.JsonCodec;
import jakarta.json.*;

public record PingResponse() {
    public static final JsonCodec<PingResponse> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(PingResponse resp) {
            return JsonValue.EMPTY_JSON_OBJECT;
        }

        @Override
        public PingResponse fromJson(JsonObject obj) {
            if (obj != null && !obj.isEmpty()) throw new IllegalArgumentException("unexpected fields");
            return new PingResponse();
        }
    };
}
