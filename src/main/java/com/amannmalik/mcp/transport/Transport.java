package com.amannmalik.mcp.transport;

import jakarta.json.JsonObject;

import java.io.IOException;

/// - [Transports](specification/2025-06-18/basic/transports.mdx)
public sealed interface Transport extends AutoCloseable permits StdioTransport, StreamableHttpClientTransport, StreamableHttpServerTransport {
    void send(JsonObject message) throws IOException;

    JsonObject receive() throws IOException;
    
    JsonObject receive(long timeoutMillis) throws IOException;

    @Override
    void close() throws IOException;
    
    /**
     * Start listening for incoming messages (streaming transports only).
     * Default implementation does nothing for non-streaming transports.
     */
    default void listen() throws IOException {
        // Default implementation for non-streaming transports
    }
    
    /**
     * Set the protocol version for the transport.
     * Default implementation does nothing for transports that don't need version setting.
     */
    default void setProtocolVersion(String version) {
        // Default implementation for transports that don't need version setting
    }
}
