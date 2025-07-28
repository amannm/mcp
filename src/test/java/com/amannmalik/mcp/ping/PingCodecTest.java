package com.amannmalik.mcp.ping;

import com.amannmalik.mcp.jsonrpc.JsonRpcRequest;
import com.amannmalik.mcp.jsonrpc.JsonRpcResponse;
import com.amannmalik.mcp.jsonrpc.RequestId;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PingCodecTest {
    @Test
    void requestAndResponse() {
        RequestId id = new RequestId.NumericId(1);
        JsonRpcRequest req = PingCodec.toRequest(id);
        assertEquals("ping", req.method());
        JsonRpcResponse resp = PingCodec.toResponse(id);
        assertEquals(id, resp.id());
        assertEquals(0, resp.result().size());
    }
}
