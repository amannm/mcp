package com.amannmalik.mcp.ping;

import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.util.EmptyJsonObjectCodec;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

public record PingResponse() {
    public static final JsonCodec<PingResponse> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(PingResponse resp) {
            return JsonValue.EMPTY_JSON_OBJECT;
        }

        @Override
        public PingResponse fromJson(JsonObject obj) {
            EmptyJsonObjectCodec.requireEmpty(obj);
            return new PingResponse();
        }
    };
}
