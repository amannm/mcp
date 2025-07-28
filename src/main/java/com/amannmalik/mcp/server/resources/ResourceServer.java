package com.amannmalik.mcp.server.resources;

import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.lifecycle.ServerCapability;
import com.amannmalik.mcp.server.McpServer;
import com.amannmalik.mcp.transport.Transport;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;

import java.io.IOException;
import java.util.EnumSet;

/** McpServer extension that exposes resource operations. */
public class ResourceServer extends McpServer {
    private final ResourceProvider provider;

    public ResourceServer(ResourceProvider provider, Transport transport) {
        super(EnumSet.of(ServerCapability.RESOURCES), transport);
        this.provider = provider;
        registerRequestHandler("resources/list", this::listResources);
        registerRequestHandler("resources/read", this::readResource);
        registerRequestHandler("resources/templates/list", this::listTemplates);
        registerRequestHandler("resources/subscribe", this::subscribe);
    }

    private JsonRpcMessage listResources(JsonRpcRequest req) {
        String cursor = req.params() == null ? null : req.params().getString("cursor", null);
        ResourceList list;
        try {
            list = provider.list(cursor);
        } catch (IOException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INTERNAL_ERROR.code(), e.getMessage(), null));
        }
        JsonArrayBuilder arr = Json.createArrayBuilder();
        for (Resource r : list.resources()) arr.add(ResourcesCodec.toJsonObject(r));
        var builder = Json.createObjectBuilder().add("resources", arr.build());
        if (list.nextCursor() != null) builder.add("nextCursor", list.nextCursor());
        return new JsonRpcResponse(req.id(), builder.build());
    }

    private JsonRpcMessage readResource(JsonRpcRequest req) {
        JsonObject params = req.params();
        if (params == null || !params.containsKey("uri")) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), "uri required", null));
        }
        String uri = params.getString("uri");
        ResourceBlock block;
        try {
            block = provider.read(uri);
        } catch (IOException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INTERNAL_ERROR.code(), e.getMessage(), null));
        }
        if (block == null) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), "unknown resource", null));
        }
        JsonObject result = Json.createObjectBuilder()
                .add("contents", Json.createArrayBuilder().add(ResourcesCodec.toJsonObject(block)).build())
                .build();
        return new JsonRpcResponse(req.id(), result);
    }

    private JsonRpcMessage listTemplates(JsonRpcRequest req) {
        JsonArrayBuilder arr = Json.createArrayBuilder();
        try {
            for (ResourceTemplate t : provider.templates()) arr.add(ResourcesCodec.toJsonObject(t));
        } catch (IOException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INTERNAL_ERROR.code(), e.getMessage(), null));
        }
        JsonObject result = Json.createObjectBuilder().add("resourceTemplates", arr.build()).build();
        return new JsonRpcResponse(req.id(), result);
    }

    private JsonRpcMessage subscribe(JsonRpcRequest req) {
        JsonObject params = req.params();
        if (params == null || !params.containsKey("uri")) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), "uri required", null));
        }
        String uri = params.getString("uri");
        try {
            provider.subscribe(uri, update -> {
                try {
                    var b = Json.createObjectBuilder().add("uri", update.uri());
                    if (update.title() != null) b.add("title", update.title());
                    send(new JsonRpcNotification("notifications/resources/updated", b.build()));
                } catch (IOException ignored) {}
            });
            return new JsonRpcResponse(req.id(), Json.createObjectBuilder().build());
        } catch (IOException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INTERNAL_ERROR.code(), e.getMessage(), null));
        }
    }

    /** Notify clients that the list of resources has changed. */
    public void listChanged() throws IOException {
        send(new JsonRpcNotification("notifications/resources/list_changed", null));
    }
}
