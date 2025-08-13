package com.amannmalik.mcp.api;

import com.amannmalik.mcp.transport.*;
import jakarta.json.JsonObject;

import java.io.IOException;
import java.time.Duration;

/// - [Transports](specification/2025-06-18/basic/transports.mdx)
public sealed interface Transport extends AutoCloseable permits
        StdioTransport,
        StreamableHttpClientTransport,
        StreamableHttpServerTransport {
    void send(JsonObject message) throws IOException;

    JsonObject receive() throws IOException;

    JsonObject receive(Duration timeoutMillis) throws IOException;

    @Override
    void close() throws IOException;

    default void listen() throws IOException {
        // Default implementation for non-streaming transports
    }

    default void setProtocolVersion(String version) {
        // Default implementation for transports that don't need version setting
    }
}
