package com.amannmalik.mcp.ping;

import com.amannmalik.mcp.jsonrpc.JsonRpcRequest;
import com.amannmalik.mcp.jsonrpc.JsonRpcResponse;
import com.amannmalik.mcp.jsonrpc.RequestId;
import jakarta.json.JsonValue;

public final class PingCodec {
    private PingCodec() {
    }

    public static PingRequest toPingRequest(JsonRpcRequest req) {
        if (req == null) throw new IllegalArgumentException("request required");
        if (req.params() != null && !req.params().isEmpty()) {
            throw new IllegalArgumentException("no params expected");
        }
        return new PingRequest();
    }

    public static JsonRpcResponse toResponse(RequestId id) {
        if (id == null) throw new IllegalArgumentException("id required");
        return new JsonRpcResponse(id, JsonValue.EMPTY_JSON_OBJECT);
    }

    public static PingResponse toPingResponse(JsonRpcResponse resp) {
        if (resp == null) throw new IllegalArgumentException("response required");
        if (resp.result() == null || !resp.result().isEmpty()) {
            throw new IllegalArgumentException("expected empty result");
        }
        return new PingResponse();
    }
}
