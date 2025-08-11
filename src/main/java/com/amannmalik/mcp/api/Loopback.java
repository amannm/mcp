package com.amannmalik.mcp.api;

import com.amannmalik.mcp.api.model.*;
import com.amannmalik.mcp.transport.StdioTransport;
import jakarta.json.JsonObject;

import java.io.*;
import java.util.EnumSet;
import java.util.function.Function;

public final class Loopback {
    private Loopback() {
    }

    public static Connection connect(McpHost host, String id, Function<Transport, McpServer> factory) throws IOException {
        PipedInputStream serverIn = new PipedInputStream();
        PipedOutputStream hostOut = new PipedOutputStream(serverIn);
        PipedInputStream hostIn = new PipedInputStream();
        PipedOutputStream serverOut = new PipedOutputStream(hostIn);
        Transport hostTransport = new StdioTransport(hostIn, hostOut);
        Transport serverTransport = new StdioTransport(serverIn, serverOut);
        McpServer server = factory.apply(serverTransport);
        Thread t = new Thread(() -> {
            try {
                server.serve();
            } catch (IOException ignored) {
            }
        });
        t.setDaemon(true);
        t.start();
        McpClient client = new McpClient(
                new ClientInfo(id, id, McpConfiguration.current().clientVersion()),
                EnumSet.noneOf(ClientCapability.class),
                hostTransport,
                null,
                null,
                null,
                null);
        host.register(id, client);
        return new Connection(client, hostTransport, server);
    }

    public static void request(McpClient client, RequestMethod method, JsonObject params) throws IOException {
        client.request(method, params, McpConfiguration.current().defaultMs());
    }

    public record Connection(McpClient client, Transport transport, McpServer server) {
    }
}
