package com.amannmalik.mcp.example;

import com.amannmalik.mcp.server.tools.DatabaseToolProvider;
import com.amannmalik.mcp.server.tools.ToolServer;
import com.amannmalik.mcp.transport.StdioTransport;
import jakarta.json.Json;

import java.util.Map;

/** Example database tool server using stdio. */
public final class DatabaseServerExample {
    public static void main(String[] args) throws Exception {
        var rows = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("name", "bob"))
                .build();
        try (ToolServer server = ToolServer.create(new DatabaseToolProvider(Map.of("select *", rows)),
                new StdioTransport(System.in, System.out))) {
            server.serve();
        }
    }
}
