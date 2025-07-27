package com.amannmalik.mcp.server;

import com.amannmalik.mcp.jsonrpc.JsonRpcCodec;
import com.amannmalik.mcp.jsonrpc.JsonRpcRequest;
import com.amannmalik.mcp.jsonrpc.JsonRpcResponse;
import com.amannmalik.mcp.jsonrpc.RequestId;
import com.amannmalik.mcp.lifecycle.ServerCapability;
import com.amannmalik.mcp.transport.StdioTransport;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpServerTest {
    @Test
    void initializationRoundTrip() throws Exception {
        ByteArrayOutputStream serverOut = new ByteArrayOutputStream();
        StdioTransport transport = new StdioTransport(new ByteArrayInputStream(new byte[0]), serverOut);
        TestServer server = new TestServer(transport);

        JsonObject initParams = Json.createObjectBuilder()
                .add("protocolVersion", "2025-06-18")
                .add("capabilities", Json.createObjectBuilder()
                        .add("client", Json.createObjectBuilder())
                        .add("server", Json.createObjectBuilder())
                        .build())
                .add("clientInfo", Json.createObjectBuilder()
                        .add("name", "test")
                        .add("title", "Test")
                        .add("version", "0")
                        .build())
                .build();
        JsonRpcRequest request = new JsonRpcRequest(new RequestId.NumericId(1), "initialize", initParams);

        server.onRequest(request);

        JsonObject responseJson = Json.createReader(new ByteArrayInputStream(serverOut.toByteArray())).readObject();
        JsonRpcResponse response = (JsonRpcResponse) JsonRpcCodec.fromJsonObject(responseJson);
        assertEquals(new RequestId.NumericId(1), response.id());
    }

    private static class TestServer extends McpServer {
        TestServer(StdioTransport transport) {
            super(EnumSet.of(ServerCapability.PROMPTS), transport);
        }
    }
}
