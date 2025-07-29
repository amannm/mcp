package com.amannmalik.mcp.transport;

import jakarta.json.JsonObject;

import java.io.IOException;

public interface Transport extends AutoCloseable {
    void send(JsonObject message) throws IOException;

    JsonObject receive() throws IOException;

    @Override
    void close() throws IOException;
}
