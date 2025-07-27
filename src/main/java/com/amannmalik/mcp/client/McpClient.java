package com.amannmalik.mcp.client;

import com.amannmalik.mcp.lifecycle.ClientInfo;
import java.io.IOException;

/** Basic client contract managed by a HostProcess. */
public interface McpClient extends AutoCloseable {
    ClientInfo info();

    void connect() throws IOException;

    void disconnect() throws IOException;

    boolean connected();

    /** Context currently held by the client for aggregation. */
    String context();

    @Override
    default void close() throws IOException {
        disconnect();
    }
}
