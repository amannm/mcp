package com.amannmalik.mcp.core;

import jakarta.json.JsonObject;

import java.io.IOException;

/// - [Transports](specification/2025-06-18/basic/transports.mdx)
public sealed interface Transport extends AutoCloseable permits StdioTransport, StreamableHttpClientTransport, StreamableHttpServerTransport {
    void send(JsonObject message) throws IOException;

    JsonObject receive() throws IOException;
    
    JsonObject receive(long timeoutMillis) throws IOException;

    @Override
    void close() throws IOException;
}
