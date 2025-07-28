package com.amannmalik.mcp.example;

import com.amannmalik.mcp.server.resources.FileSystemResourceProvider;
import com.amannmalik.mcp.server.resources.ResourceServer;
import com.amannmalik.mcp.transport.StdioTransport;

import java.nio.file.Paths;

/** Example standalone file resource server using stdio. */
public final class FileServerExample {
    public static void main(String[] args) throws Exception {
        try (ResourceServer server = new ResourceServer(
                new FileSystemResourceProvider(Paths.get(args.length > 0 ? args[0] : ".")),
                new StdioTransport(System.in, System.out))) {
            server.serve();
        }
    }
}
