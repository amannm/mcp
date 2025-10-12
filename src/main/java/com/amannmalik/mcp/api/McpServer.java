package com.amannmalik.mcp.api;

import com.amannmalik.mcp.core.ServerRuntime;
import com.amannmalik.mcp.spi.Principal;

import java.io.Closeable;
import java.io.IOException;

public interface McpServer extends Closeable {
    static McpServer create(McpServerConfiguration config,
                            Principal principal,
                            String instructions) throws Exception {
        return new ServerRuntime(
                config,
                principal,
                instructions);
    }

    void serve() throws IOException;

    @Override
    void close() throws IOException;
}
