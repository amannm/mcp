package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.JsonRpcMessage;
import com.amannmalik.mcp.api.RequestId;
import com.amannmalik.mcp.jsonrpc.*;
import jakarta.json.*;

public class JsonRpcMessageJsonCodec implements JsonCodec<JsonRpcMessage> {

    static void validateVersion(JsonObject obj) {
        var version = obj.getString("jsonrpc", null);
        if (!JsonRpc.VERSION.equals(version)) throw new IllegalArgumentException("Unsupported jsonrpc version: " + version);
    }

    static JsonObject params(JsonValue value) {
        if (value == null) return null;
        if (value.getValueType() != JsonValue.ValueType.OBJECT) throw new IllegalArgumentException("params must be an object");
        return value.asJsonObject();
    }

    static JsonObject result(JsonValue value) {
        if (value == null || value.getValueType() != JsonValue.ValueType.OBJECT) throw new IllegalArgumentException("result must be an object");
        return value.asJsonObject();
    }

    static JsonRpcKind kind(String method, JsonValue idValue, boolean hasResult, boolean hasError) {
        if (hasResult && hasError) throw new IllegalArgumentException("response cannot contain both result and error");
        if (method != null) {
            if (idValue != null && idValue.getValueType() != JsonValue.ValueType.NULL) return JsonRpcKind.REQUEST;
            return JsonRpcKind.NOTIFICATION;
        }
        if (hasResult) return JsonRpcKind.RESPONSE;
        if (hasError) return JsonRpcKind.ERROR;
        throw new IllegalArgumentException("Unknown message type");
    }

    static RequestId requestId(JsonValue value) {
        if (value == null || value.getValueType() == JsonValue.ValueType.NULL) throw new IllegalArgumentException("id is required for response");
        return RequestId.from(value);
    }

    static RequestId optionalId(JsonValue value) {
        if (value == null || value.getValueType() == JsonValue.ValueType.NULL) return RequestId.NullId.INSTANCE;
        return RequestId.from(value);
    }

    static JsonRpcError.ErrorDetail errorDetail(JsonObject obj) {
        var codeValue = obj.get("code");
        if (!(codeValue instanceof JsonNumber number) || !number.isIntegral()) throw new IllegalArgumentException("error code must be integer");
        int code;
        try {
            code = number.intValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("error code must be integer", e);
        }
        var messageValue = obj.get("message");
        if (!(messageValue instanceof JsonString msg)) throw new IllegalArgumentException("error message must be string");
        return new JsonRpcError.ErrorDetail(code, msg.getString(), obj.get("data"));
    }

    @Override
    public JsonObject toJson(JsonRpcMessage msg) {
        var builder = Json.createObjectBuilder().add("jsonrpc", JsonRpc.VERSION);
        return switch (msg) {
            case JsonRpcRequest r -> {
                builder.add("id", RequestId.toJsonValue(r.id()));
                builder.add("method", r.method());
                if (r.params() != null) builder.add("params", r.params());
                yield builder.build();
            }
            case JsonRpcNotification n -> {
                builder.add("method", n.method());
                if (n.params() != null) builder.add("params", n.params());
                yield builder.build();
            }
            case JsonRpcResponse r -> {
                builder.add("id", RequestId.toJsonValue(r.id()));
                builder.add("result", r.result());
                yield builder.build();
            }
            case JsonRpcError e -> {
                builder.add("id", RequestId.toJsonValue(e.id()));
                var err = e.error();
                var errBuilder = Json.createObjectBuilder()
                        .add("code", err.code())
                        .add("message", err.message());
                if (err.data() != null) errBuilder.add("data", err.data());
                builder.add("error", errBuilder.build());
                yield builder.build();
            }
        };
    }

    @Override
    public JsonRpcMessage fromJson(JsonObject obj) {
        validateVersion(obj);
        var idValue = obj.get("id");
        var method = obj.getString("method", null);
        var params = params(obj.get("params"));
        var kind = kind(method, idValue, obj.containsKey("result"), obj.containsKey("error"));
        return switch (kind) {
            case REQUEST -> new JsonRpcRequest(RequestId.from(idValue), method, params);
            case NOTIFICATION -> new JsonRpcNotification(method, params);
            case RESPONSE -> new JsonRpcResponse(requestId(idValue), result(obj.get("result")));
            case ERROR -> new JsonRpcError(optionalId(idValue), errorDetail(obj.getJsonObject("error")));
        };
    }

}
