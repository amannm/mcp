package com.amannmalik.mcp.transport;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StdioTransportTest {
    @Test
    void roundTrip() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StdioTransport client = new StdioTransport(new ByteArrayInputStream(new byte[0]), out);

        JsonObject msg = Json.createObjectBuilder().add("hello", "world").build();
        client.send(msg);
        String written = out.toString(StandardCharsets.UTF_8);

        ByteArrayInputStream in = new ByteArrayInputStream(written.getBytes(StandardCharsets.UTF_8));
        StdioTransport server = new StdioTransport(in, new ByteArrayOutputStream());
        JsonObject read = server.receive();
        assertEquals(msg, read);
    }
}
