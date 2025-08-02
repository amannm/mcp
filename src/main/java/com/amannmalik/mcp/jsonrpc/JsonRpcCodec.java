package com.amannmalik.mcp.jsonrpc;

import jakarta.json.*;

public final class JsonRpcCodec {
    private JsonRpcCodec() {
    }

    public static JsonObject toJsonObject(JsonRpcMessage msg) {
        var builder = Json.createObjectBuilder();
        builder.add("jsonrpc", JsonRpc.VERSION);

        switch (msg) {
            case JsonRpcRequest r -> {
                RequestIdCodec.add(builder, "id", r.id());
                builder.add("method", r.method());
                if (r.params() != null) builder.add("params", r.params());
            }
            case JsonRpcNotification n -> {
                builder.add("method", n.method());
                if (n.params() != null) builder.add("params", n.params());
            }
            case JsonRpcResponse r -> {
                RequestIdCodec.add(builder, "id", r.id());
                builder.add("result", r.result());
            }
            case JsonRpcError e -> {
                RequestIdCodec.add(builder, "id", e.id());
                var err = e.error();
                var errBuilder = Json.createObjectBuilder()
                        .add("code", err.code())
                        .add("message", err.message());
                if (err.data() != null) errBuilder.add("data", err.data());
                builder.add("error", errBuilder.build());
            }
        }
        return builder.build();
    }

    public static JsonRpcMessage fromJsonObject(JsonObject obj) {
        validateVersion(obj);
        var idValue = obj.get("id");
        var method = obj.getString("method", null);
        var params = params(obj.get("params"));
        var kind = kind(method, idValue, obj.containsKey("result"), obj.containsKey("error"));
        return switch (kind) {
            case REQUEST -> new JsonRpcRequest(RequestIdCodec.from(idValue), method, params);
            case NOTIFICATION -> new JsonRpcNotification(method, params);
            case RESPONSE -> new JsonRpcResponse(requestId(idValue), result(obj.get("result")));
            case ERROR -> new JsonRpcError(optionalId(idValue), errorDetail(obj.getJsonObject("error")));
        };
    }

    private enum Kind {REQUEST, NOTIFICATION, RESPONSE, ERROR}

    private static void validateVersion(JsonObject obj) {
        var version = obj.getString("jsonrpc", null);
        if (!JsonRpc.VERSION.equals(version)) throw new IllegalArgumentException("Unsupported jsonrpc version: " + version);
    }

    private static JsonObject params(JsonValue value) {
        if (value == null) return null;
        if (value.getValueType() != JsonValue.ValueType.OBJECT) throw new IllegalArgumentException("params must be an object");
        return value.asJsonObject();
    }

    private static JsonObject result(JsonValue value) {
        if (value == null || value.getValueType() != JsonValue.ValueType.OBJECT) throw new IllegalArgumentException("result must be an object");
        return value.asJsonObject();
    }

    private static Kind kind(String method, JsonValue idValue, boolean hasResult, boolean hasError) {
        if (hasResult && hasError) throw new IllegalArgumentException("response cannot contain both result and error");
        if (method != null) {
            if (idValue != null && idValue.getValueType() != JsonValue.ValueType.NULL) return Kind.REQUEST;
            return Kind.NOTIFICATION;
        }
        if (hasResult) return Kind.RESPONSE;
        if (hasError) return Kind.ERROR;
        throw new IllegalArgumentException("Unknown message type");
    }

    private static RequestId requestId(JsonValue value) {
        if (value == null || value.getValueType() == JsonValue.ValueType.NULL) throw new IllegalArgumentException("id is required for response");
        return RequestIdCodec.from(value);
    }

    private static RequestId optionalId(JsonValue value) {
        if (value == null || value.getValueType() == JsonValue.ValueType.NULL) return RequestId.NullId.INSTANCE;
        return RequestIdCodec.from(value);
    }

    private static JsonRpcError.ErrorDetail errorDetail(JsonObject obj) {
        return new JsonRpcError.ErrorDetail(obj.getInt("code"), obj.getString("message"), obj.get("data"));
    }
}
