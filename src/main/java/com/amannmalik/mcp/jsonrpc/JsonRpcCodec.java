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
        var version = obj.getString("jsonrpc", null);
        if (!JsonRpc.VERSION.equals(version)) {
            throw new IllegalArgumentException("Unsupported jsonrpc version: " + version);
        }

        var idValue = obj.get("id");
        var method = obj.getString("method", null);
        var paramsValue = obj.get("params");
        if (paramsValue != null && paramsValue.getValueType() != JsonValue.ValueType.OBJECT) {
            throw new IllegalArgumentException("params must be an object");
        }
        var hasError = obj.containsKey("error");
        var hasResult = obj.containsKey("result");
        if (hasError && hasResult) {
            throw new IllegalArgumentException("response cannot contain both result and error");
        }

        JsonObject paramsObj = paramsValue == null ? null : paramsValue.asJsonObject();

        if (method != null && idValue != null && idValue.getValueType() != JsonValue.ValueType.NULL) {
            return new JsonRpcRequest(RequestIdCodec.from(idValue), method, paramsObj);
        }
        if (method != null) {
            return new JsonRpcNotification(method, paramsObj);
        }
        if (hasResult) {
            if (idValue == null || idValue.getValueType() == JsonValue.ValueType.NULL) {
                throw new IllegalArgumentException("id is required for response");
            }
            var resultVal = obj.get("result");
            if (resultVal == null || resultVal.getValueType() != JsonValue.ValueType.OBJECT) {
                throw new IllegalArgumentException("result must be an object");
            }
            return new JsonRpcResponse(RequestIdCodec.from(idValue), resultVal.asJsonObject());
        }
        if (hasError) {
            RequestId id;
            if (idValue == null || idValue.getValueType() == JsonValue.ValueType.NULL) {
                id = new RequestId.NullId();
            } else {
                id = RequestIdCodec.from(idValue);
            }
            var errObj = obj.getJsonObject("error");
            var detail = new JsonRpcError.ErrorDetail(
                    errObj.getInt("code"),
                    errObj.getString("message"),
                    errObj.get("data")
            );
            return new JsonRpcError(id, detail);
        }
        throw new IllegalArgumentException("Unknown message type");
    }
}
