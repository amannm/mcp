package com.amannmalik.mcp.server.completion;

import com.amannmalik.mcp.jsonrpc.JsonRpcCodec;
import com.amannmalik.mcp.jsonrpc.JsonRpcRequest;
import com.amannmalik.mcp.jsonrpc.JsonRpcResponse;
import com.amannmalik.mcp.jsonrpc.RequestId;
import com.amannmalik.mcp.transport.StdioTransport;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CompletionServerTest {
    @Test
    void complete() throws Exception {
        CompletionProvider provider = req -> new CompleteResult(new CompleteResult.Completion(List.of("java"), 1, false));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StdioTransport transport = new StdioTransport(new ByteArrayInputStream(new byte[0]), out);
        TestServer server = new TestServer(provider, transport);

        CompleteRequest request = new CompleteRequest(
                new CompleteRequest.Ref.PromptRef("code"),
                new CompleteRequest.Argument("lang", "j"),
                new CompleteRequest.Context(Map.of())
        );
        JsonRpcRequest rpcReq = new JsonRpcRequest(new RequestId.NumericId(1), "completion/complete", CompletionCodec.toJsonObject(request));
        server.handle(rpcReq);

        JsonObject respJson = Json.createReader(new ByteArrayInputStream(out.toByteArray())).readObject();
        JsonRpcResponse resp = (JsonRpcResponse) JsonRpcCodec.fromJsonObject(respJson);
        assertEquals("java", resp.result().getJsonObject("completion").getJsonArray("values").getString(0));
    }

    private static class TestServer extends CompletionServer {
        TestServer(CompletionProvider provider, StdioTransport transport) {
            super(provider, transport);
        }

        void handle(JsonRpcRequest req) throws Exception {
            onRequest(req);
        }
    }
}
