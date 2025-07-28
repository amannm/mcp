package com.amannmalik.mcp.server.logging;

import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.transport.StdioTransport;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class LoggingServerTest {
    @Test
    void setLevelAndLog() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StdioTransport transport = new StdioTransport(new ByteArrayInputStream(new byte[0]), out);
        TestServer server = new TestServer(transport);

        SetLevelRequest req = new SetLevelRequest(LoggingLevel.DEBUG);
        JsonRpcRequest rpc = new JsonRpcRequest(new RequestId.NumericId(1), "logging/setLevel", LoggingCodec.toJsonObject(req));
        server.handle(rpc);

        JsonObject respJson = Json.createReader(new ByteArrayInputStream(out.toByteArray())).readObject();
        JsonRpcResponse resp = (JsonRpcResponse) JsonRpcCodec.fromJsonObject(respJson);
        assertEquals(new RequestId.NumericId(1), resp.id());
        out.reset();

        server.log(LoggingLevel.DEBUG, "test", JsonValue.TRUE);
        JsonObject noteJson = Json.createReader(new ByteArrayInputStream(out.toByteArray())).readObject();
        JsonRpcNotification note = (JsonRpcNotification) JsonRpcCodec.fromJsonObject(noteJson);
        assertEquals("notifications/message", note.method());
        LoggingNotification ln = LoggingCodec.toLoggingNotification(note.params());
        assertEquals(LoggingLevel.DEBUG, ln.level());
        assertEquals("test", ln.logger());
        assertEquals(JsonValue.TRUE, ln.data());
    }

    @Test
    void ignoresBelowMinLevel() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StdioTransport transport = new StdioTransport(new ByteArrayInputStream(new byte[0]), out);
        TestServer server = new TestServer(transport);

        server.log(LoggingLevel.DEBUG, null, JsonValue.TRUE);
        assertEquals(0, out.size());
    }

    private static class TestServer extends LoggingServer {
        TestServer(StdioTransport transport) {
            super(transport);
        }
        void handle(JsonRpcRequest req) throws IOException {
            onRequest(req);
        }
    }
}
