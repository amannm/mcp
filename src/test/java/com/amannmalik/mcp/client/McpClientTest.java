package com.amannmalik.mcp.client;

import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.lifecycle.*;
import com.amannmalik.mcp.transport.Transport;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

class McpClientTest {
    @Test
    void successfulInitialization() throws Exception {
        StubTransport transport = new StubTransport();
        McpClient client = new McpClient(transport);
        Capabilities caps = new Capabilities(EnumSet.of(ClientCapability.ROOTS), EnumSet.noneOf(ServerCapability.class));
        ClientInfo info = new ClientInfo("client", "Client", "1");

        // prepare server response
        InitializeResponse response = new InitializeResponse(
                ProtocolLifecycle.SUPPORTED_VERSION,
                new Capabilities(caps.client(), EnumSet.of(ServerCapability.PROMPTS)),
                new ServerInfo("server", "Server", "1"),
                null
        );
        RequestId id = new RequestId.NumericId(1);
        JsonRpcResponse rpcResponse = new JsonRpcResponse(id, LifecycleCodec.toJson(response));
        transport.nextResponse = JsonRpcCodec.toJsonObject(rpcResponse);

        InitializeResponse result = client.initialize(caps, info);
        assertEquals(LifecycleState.OPERATION, client.state());
        assertEquals(response, result);
        assertEquals("initialize", transport.firstSent.getString("method"));
        assertEquals("notifications/initialized", transport.lastSent.getString("method"));
    }

    private static class StubTransport implements Transport {
        JsonObject firstSent;
        JsonObject lastSent;
        JsonObject nextResponse;
        @Override public void send(JsonObject message) {
            if (firstSent == null) firstSent = message;
            lastSent = message;
        }
        @Override public JsonObject receive() { return nextResponse; }
        @Override public void close() throws IOException { }
    }
}
