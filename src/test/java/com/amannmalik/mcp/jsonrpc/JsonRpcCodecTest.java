package com.amannmalik.mcp.jsonrpc;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonRpcCodecTest {
    @Test
    void roundTripRequest() {
        var request = new JsonRpcRequest(new RequestId.NumericId(1), "ping", null);
        JsonObject json = JsonRpcCodec.toJsonObject(request);
        var parsed = JsonRpcCodec.fromJsonObject(json);
        assertEquals(request, parsed);
    }

    @Test
    void roundTripError() {
        var detail = new JsonRpcError.ErrorDetail(JsonRpcErrorCode.INVALID_PARAMS.code(), "oops", JsonValue.NULL);
        var error = new JsonRpcError(new RequestId.StringId("1"), detail);
        JsonObject json = JsonRpcCodec.toJsonObject(error);
        var parsed = JsonRpcCodec.fromJsonObject(json);
        assertEquals(error, parsed);
    }
}
