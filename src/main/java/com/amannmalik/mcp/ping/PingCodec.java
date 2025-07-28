package com.amannmalik.mcp.ping;

import com.amannmalik.mcp.jsonrpc.JsonRpcRequest;
import com.amannmalik.mcp.jsonrpc.JsonRpcResponse;
import com.amannmalik.mcp.jsonrpc.RequestId;
import jakarta.json.Json;
import jakarta.json.JsonObject;

/** JSON-RPC helpers for ping messages. */
public final class PingCodec {
    private PingCodec() {}

    public static JsonRpcRequest toRequest(RequestId id) {
        return new JsonRpcRequest(id, "ping", null);
    }

    public static PingRequest toPingRequest(JsonRpcRequest req) {
        return new PingRequest();
    }

    public static JsonRpcResponse toResponse(RequestId id) {
        return new JsonRpcResponse(id, Json.createObjectBuilder().build());
    }

    public static PingResponse toPingResponse(JsonRpcResponse resp) {
        return new PingResponse();
    }
}
