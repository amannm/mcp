package com.amannmalik.mcp.server.logging;

import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.lifecycle.ServerCapability;
import com.amannmalik.mcp.server.McpServer;
import com.amannmalik.mcp.transport.Transport;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.EnumSet;

/** McpServer extension providing structured logging. */
public class LoggingServer extends McpServer {
    private volatile LoggingLevel minLevel = LoggingLevel.INFO;
    private final Logger slf4j = LoggerFactory.getLogger(LoggingServer.class);

    public LoggingServer(Transport transport) {
        super(EnumSet.of(ServerCapability.LOGGING), transport);
    }

    public static LoggingServer create(Transport transport) {
        LoggingServer server = new LoggingServer(transport);
        server.registerRequestHandler("logging/setLevel", server::setLevel);
        return server;
    }

    private JsonRpcMessage setLevel(JsonRpcRequest req) {
        JsonObject params = req.params();
        if (params == null) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), "Missing params", null));
        }
        SetLevelRequest sl = LoggingCodec.toSetLevelRequest(params);
        minLevel = sl.level();
        return new JsonRpcResponse(req.id(), Json.createObjectBuilder().build());
    }

    public void log(LoggingLevel level, String loggerName, JsonValue data) throws IOException {
        if (level.ordinal() < minLevel.ordinal()) return;
        Logger log = loggerName == null ? slf4j : LoggerFactory.getLogger(loggerName);
        switch (level) {
            case DEBUG -> log.debug(data.toString());
            case INFO, NOTICE -> log.info(data.toString());
            case WARNING -> log.warn(data.toString());
            default -> log.error(data.toString());
        }
        LoggingNotification note = new LoggingNotification(level, loggerName, data);
        send(new JsonRpcNotification("notifications/message", LoggingCodec.toJsonObject(note)));
    }
}
