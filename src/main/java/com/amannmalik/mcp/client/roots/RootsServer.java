package com.amannmalik.mcp.client.roots;

import com.amannmalik.mcp.jsonrpc.JsonRpcMessage;
import com.amannmalik.mcp.jsonrpc.JsonRpcRequest;
import com.amannmalik.mcp.jsonrpc.JsonRpcResponse;
import com.amannmalik.mcp.jsonrpc.JsonRpcError;
import com.amannmalik.mcp.jsonrpc.JsonRpcErrorCode;
import com.amannmalik.mcp.jsonrpc.JsonRpcNotification;
import com.amannmalik.mcp.server.McpServer;
import com.amannmalik.mcp.lifecycle.ServerCapability;
import com.amannmalik.mcp.transport.Transport;
import jakarta.json.JsonObject;

import java.io.IOException;
import java.util.EnumSet;

/** McpServer extension providing roots/list support. */
public class RootsServer extends McpServer {
    private final RootsProvider provider;
    private RootsSubscription subscription;

    public RootsServer(RootsProvider provider, Transport transport) {
        super(EnumSet.noneOf(ServerCapability.class), transport);
        this.provider = provider;
        registerRequestHandler("roots/list", this::listRoots);
    }

    @Override
    public void close() throws IOException {
        if (subscription != null) {
            try { subscription.close(); } catch (Exception ignore) {}
        }
        super.close();
    }

    /** Begin sending notifications when roots change. */
    public void watch() throws IOException {
        if (subscription != null) return;
        subscription = provider.subscribe(() -> {
            try {
                send(new JsonRpcNotification("notifications/roots/list_changed", null));
            } catch (IOException ignore) {}
        });
    }

    private JsonRpcMessage listRoots(JsonRpcRequest req) {
        try {
            var list = provider.list();
            JsonObject result = RootsCodec.toJsonObject(new ListRootsResponse(list));
            return new JsonRpcResponse(req.id(), result);
        } catch (IOException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INTERNAL_ERROR.code(), e.getMessage(), null));
        }
    }
}
