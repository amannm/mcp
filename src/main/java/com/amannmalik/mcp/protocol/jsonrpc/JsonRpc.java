package com.amannmalik.mcp.protocol.jsonrpc;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import java.util.Optional;

import static jakarta.json.JsonValue.ValueType.OBJECT;

public final class JsonRpc {
    private JsonRpc() {}

    public static Optional<JsonRpcMessage> fromJson(JsonObject obj) {
        if (!"2.0".equals(obj.getString("jsonrpc", null))) return Optional.empty();
        if (obj.containsKey("method")) {
            var params = Optional.ofNullable(obj.get("params"));
            if (obj.containsKey("id")) {
                return RequestId.from(obj.get("id")).map(id -> new JsonRpcRequest(id, obj.getString("method"), params));
            }
            return Optional.of(new JsonRpcNotification(obj.getString("method"), params));
        }
        if (obj.containsKey("result") && obj.containsKey("id")) {
            return RequestId.from(obj.get("id")).map(id -> new JsonRpcResponse(id, obj.get("result")));
        }
        if (obj.containsKey("error") && obj.get("error").getValueType() == OBJECT && obj.containsKey("id")) {
            var errObj = obj.getJsonObject("error");
            var data = Optional.ofNullable(errObj.get("data"));
            return RequestId.from(obj.get("id")).map(id -> new JsonRpcError(id, errObj.getInt("code"), errObj.getString("message"), data));
        }
        return Optional.empty();
    }
}
