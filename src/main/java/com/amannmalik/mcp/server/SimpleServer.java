package com.amannmalik.mcp.server;

import com.amannmalik.mcp.lifecycle.ServerCapability;
import com.amannmalik.mcp.transport.Transport;

import java.util.EnumSet;

public final class SimpleServer extends McpServer {
    public SimpleServer(Transport transport) {
        super(EnumSet.noneOf(ServerCapability.class), transport);
    }
}
