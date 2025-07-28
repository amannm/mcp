package com.amannmalik.mcp.prompts;

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

class PromptServerTest {
    @Test
    void listAndGet() throws Exception {
        InMemoryPromptProvider provider = new InMemoryPromptProvider();
        Prompt meta = new Prompt(
                "hello",
                "Hello",
                "Simple greeting",
                List.of(new PromptArgument("name", "Name", null, true))
        );
        PromptTemplate tmpl = new PromptTemplate(
                meta,
                List.of(new PromptMessageTemplate(Role.USER, new PromptContent.Text("Hello {name}!")))
        );
        provider.add(tmpl);

        ByteArrayOutputStream serverOut = new ByteArrayOutputStream();
        StdioTransport transport = new StdioTransport(new ByteArrayInputStream(new byte[0]), serverOut);
        TestServer server = new TestServer(provider, transport);

        JsonRpcRequest listReq = new JsonRpcRequest(new RequestId.NumericId(1), "prompts/list", Json.createObjectBuilder().build());
        server.handle(listReq);
        JsonObject listRespJson = Json.createReader(new ByteArrayInputStream(serverOut.toByteArray())).readObject();
        serverOut.reset();
        JsonRpcResponse listResp = (JsonRpcResponse) JsonRpcCodec.fromJsonObject(listRespJson);
        assertTrue(listResp.result().getJsonArray("prompts").size() == 1);

        JsonObject getParams = Json.createObjectBuilder()
                .add("name", "hello")
                .add("arguments", Json.createObjectBuilder().add("name", "World").build())
                .build();
        JsonRpcRequest getReq = new JsonRpcRequest(new RequestId.NumericId(2), "prompts/get", getParams);
        server.handle(getReq);
        JsonObject getRespJson = Json.createReader(new ByteArrayInputStream(serverOut.toByteArray())).readObject();
        JsonRpcResponse getResp = (JsonRpcResponse) JsonRpcCodec.fromJsonObject(getRespJson);
        assertEquals("Hello World!", getResp.result().getJsonArray("messages").getJsonObject(0).getJsonObject("content").getString("text"));
    }

    private static class TestServer extends PromptServer {
        TestServer(PromptProvider provider, StdioTransport transport) {
            super(provider, transport);
        }

        void handle(JsonRpcRequest req) throws Exception {
            onRequest(req);
        }
    }
}
