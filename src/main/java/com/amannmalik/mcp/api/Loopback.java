package com.amannmalik.mcp.api;

import com.amannmalik.mcp.transport.StdioTransport;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.EnumSet;
import java.util.function.Function;
import jakarta.json.JsonObject;

public final class Loopback {
    public record Connection(McpClient client, Transport transport, McpServer server) {}

    private Loopback() {}

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
}
