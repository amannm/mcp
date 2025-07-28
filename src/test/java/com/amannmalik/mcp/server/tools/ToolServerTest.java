package com.amannmalik.mcp.server.tools;

import com.amannmalik.mcp.client.SimpleMcpClient;
import com.amannmalik.mcp.jsonrpc.JsonRpcMessage;
import com.amannmalik.mcp.jsonrpc.JsonRpcResponse;
import com.amannmalik.mcp.lifecycle.ClientCapability;
import com.amannmalik.mcp.lifecycle.ClientInfo;
import com.amannmalik.mcp.transport.StdioTransport;
import com.amannmalik.mcp.validation.SchemaValidator;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

class ToolServerTest {
    private StdioTransport clientTransport;
    private StdioTransport serverTransport;
    private ToolServer server;
    private Thread serverThread;

    @BeforeEach
    void setUp() throws IOException {
        PipedInputStream clientIn = new PipedInputStream();
        PipedOutputStream serverOut = new PipedOutputStream(clientIn);
        PipedInputStream serverIn = new PipedInputStream();
        PipedOutputStream clientOut = new PipedOutputStream(serverIn);
        clientTransport = new StdioTransport(clientIn, clientOut);
        serverTransport = new StdioTransport(serverIn, serverOut);
        server = ToolServer.create(new EchoProvider(), serverTransport);
        serverThread = new Thread(() -> {
            try {
                server.serve();
            } catch (IOException ignored) {
            }
        });
        serverThread.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        clientTransport.close();
        server.close();
        serverThread.join();
    }

    @Test
    void listAndCall() throws Exception {
        SimpleMcpClient client = new SimpleMcpClient(
                new ClientInfo("client", "Client", "1"),
                EnumSet.of(ClientCapability.EXPERIMENTAL),
                clientTransport);
        client.connect();
        JsonRpcMessage listMsg = client.request("tools/list", Json.createObjectBuilder().build());
        assertTrue(listMsg instanceof JsonRpcResponse);
        JsonObject listResult = ((JsonRpcResponse) listMsg).result();
        JsonArray tools = listResult.getJsonArray("tools");
        assertEquals(1, tools.size());

        JsonObject args = Json.createObjectBuilder().add("message", "hi").build();
        JsonRpcMessage callMsg = client.request("tools/call", Json.createObjectBuilder()
                .add("name", "echo")
                .add("arguments", args)
                .build());
        assertTrue(callMsg instanceof JsonRpcResponse);
        JsonObject callResult = ((JsonRpcResponse) callMsg).result();
        assertEquals("hi", callResult.getJsonArray("content").getJsonObject(0).getString("text"));
        client.disconnect();
    }

    private static class EchoProvider implements ToolProvider {
        private final Tool tool;

        EchoProvider() {
            JsonObject schema = Json.createObjectBuilder()
                    .add("type", "object")
                    .add("properties", Json.createObjectBuilder()
                            .add("message", Json.createObjectBuilder().add("type", "string")))
                    .add("required", Json.createArrayBuilder().add("message"))
                    .build();
            tool = new Tool("echo", "Echo", "Echo text", schema, null, null);
        }

        @Override
        public ToolPage list(String cursor) {
            return new ToolPage(java.util.List.of(tool), null);
        }

        @Override
        public ToolResult call(String name, JsonObject arguments) {
            if (!tool.name().equals(name)) throw new IllegalArgumentException("Unknown tool");
            SchemaValidator.validate(tool.inputSchema(), arguments);
            JsonArray content = Json.createArrayBuilder()
                    .add(Json.createObjectBuilder().add("type", "text").add("text", arguments.getString("message")).build())
                    .build();
            return new ToolResult(content, null, false);
        }
    }
}
