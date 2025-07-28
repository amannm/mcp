package com.amannmalik.mcp.server.tools;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseToolProviderTest {
    @Test
    void listAndCall() {
        JsonArray rows = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("name", "bob"))
                .build();
        DatabaseToolProvider p = new DatabaseToolProvider(Map.of("select *", rows));
        ToolPage page = p.list(null);
        assertEquals(1, page.tools().size());
        ToolResult result = p.call("query", Json.createObjectBuilder().add("sql", "select *").build());
        assertFalse(result.isError());
        assertEquals(rows, result.content());
    }
}
