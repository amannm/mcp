package com.amannmalik.mcp.ping;

import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.util.JsonUtil;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import java.util.Set;

/// - [Ping](specification/2025-06-18/basic/utilities/ping.mdx)
public final class PingCodec {
    private PingCodec() {
    }

    public static PingRequest toPingRequest(JsonRpcRequest req) {
        if (req == null) throw new IllegalArgumentException("request required");
        JsonObject params = req.params();
        if (params != null) JsonUtil.requireOnlyKeys(params, Set.of("_meta"));
        JsonObject meta = params == null ? null : params.getJsonObject("_meta");
        MetaValidator.requireValid(meta);
        return new PingRequest(meta);
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
