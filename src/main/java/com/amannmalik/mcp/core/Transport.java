package com.amannmalik.mcp.core;

import jakarta.json.JsonObject;

import java.io.IOException;

/// - [Transports](specification/2025-06-18/basic/transports.mdx)
interface Transport extends AutoCloseable {
    void send(JsonObject message) throws IOException;

    JsonObject receive() throws IOException;

    @Override
    void close() throws IOException;
}
