package com.amannmalik.mcp.ping;

import com.amannmalik.mcp.jsonrpc.JsonRpcRequest;
import com.amannmalik.mcp.jsonrpc.JsonRpcResponse;
import com.amannmalik.mcp.jsonrpc.RequestId;
import jakarta.json.Json;

public final class PingCodec {
    private PingCodec() {
    }

    public static JsonRpcRequest toRequest(RequestId id) {
        if (id == null) throw new IllegalArgumentException("id required");
        return new JsonRpcRequest(id, "ping", null);
    }

    public static PingRequest toPingRequest(JsonRpcRequest req) {
        if (req == null) throw new IllegalArgumentException("request required");
        return new PingRequest();
    }

    public static JsonRpcResponse toResponse(RequestId id) {
        if (id == null) throw new IllegalArgumentException("id required");
        return new JsonRpcResponse(id, Json.createObjectBuilder().build());
    }

    public static PingResponse toPingResponse(JsonRpcResponse resp) {
        if (resp == null) throw new IllegalArgumentException("response required");
        return new PingResponse();
    }
}
