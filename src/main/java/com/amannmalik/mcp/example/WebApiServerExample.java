package com.amannmalik.mcp.example;

import com.amannmalik.mcp.server.tools.ToolServer;
import com.amannmalik.mcp.server.tools.WebApiToolProvider;
import com.amannmalik.mcp.transport.StdioTransport;

/** Example web API tool server using stdio. */
public final class WebApiServerExample {
    public static void main(String[] args) throws Exception {
        try (ToolServer server = ToolServer.create(new WebApiToolProvider(),
                new StdioTransport(System.in, System.out))) {
            server.serve();
        }
    }
}
