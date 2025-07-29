package com.amannmalik.mcp.server.resources;

import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.lifecycle.ServerCapability;
import com.amannmalik.mcp.server.McpServer;
import com.amannmalik.mcp.transport.Transport;
import com.amannmalik.mcp.security.ResourceAccessController;
import com.amannmalik.mcp.auth.Principal;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

/** McpServer extension that exposes resource operations. */
public class ResourceServer extends McpServer {
    private static final Principal DEFAULT_PRINCIPAL = new Principal("system", java.util.Set.of());
    private final ResourceProvider provider;
    private final ResourceAccessController access;
    private final Principal principal;
    private final java.util.Map<String, ResourceSubscription> subscriptions = new java.util.concurrent.ConcurrentHashMap<>();

    public ResourceServer(ResourceProvider provider, Transport transport) {
        this(provider, transport, ResourceAccessController.ALLOW_ALL, DEFAULT_PRINCIPAL);
    }

    public ResourceServer(ResourceProvider provider, Transport transport, ResourceAccessController access, Principal principal) {
        super(EnumSet.of(ServerCapability.RESOURCES), transport);
        this.provider = provider;
        this.access = access;
        this.principal = principal;
        registerRequestHandler("resources/list", this::listResources);
        registerRequestHandler("resources/read", this::readResource);
        registerRequestHandler("resources/templates/list", this::listTemplates);
        registerRequestHandler("resources/subscribe", this::subscribe);
        registerRequestHandler("resources/unsubscribe", this::unsubscribe);
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
        for (Resource r : list.resources()) {
            try {
                access.requireAllowed(principal, r.annotations());
                arr.add(ResourcesCodec.toJsonObject(r));
            } catch (SecurityException ignored) {}
        }
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
        try {
            access.requireAllowed(principal, block.annotations());
        } catch (SecurityException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
        }
        JsonObject result = Json.createObjectBuilder()
                .add("contents", Json.createArrayBuilder().add(ResourcesCodec.toJsonObject(block)).build())
                .build();
        return new JsonRpcResponse(req.id(), result);
    }

    private JsonRpcMessage listTemplates(JsonRpcRequest req) {
        JsonArrayBuilder arr = Json.createArrayBuilder();
        try {
            for (ResourceTemplate t : provider.templates()) {
                try {
                    access.requireAllowed(principal, t.annotations());
                    arr.add(ResourcesCodec.toJsonObject(t));
                } catch (SecurityException ignored) {}
            }
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
            ResourceBlock block = provider.read(uri);
            if (block != null) {
                access.requireAllowed(principal, block.annotations());
            } else {
                return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                        JsonRpcErrorCode.INVALID_PARAMS.code(), "unknown resource", null));
            }
            ResourceSubscription sub = provider.subscribe(uri, update -> {
                try {
                    var b = Json.createObjectBuilder().add("uri", update.uri());
                    if (update.title() != null) b.add("title", update.title());
                    send(new JsonRpcNotification("notifications/resources/updated", b.build()));
                } catch (IOException ignored) {}
            });
            ResourceSubscription prev = subscriptions.put(uri, sub);
            if (prev != null) try { prev.close(); } catch (Exception ignore) {}
            return new JsonRpcResponse(req.id(), Json.createObjectBuilder().build());
        } catch (SecurityException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
        } catch (IOException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INTERNAL_ERROR.code(), e.getMessage(), null));
        }
    }

    private JsonRpcMessage unsubscribe(JsonRpcRequest req) {
        JsonObject params = req.params();
        if (params == null || !params.containsKey("uri")) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), "uri required", null));
        }
        String uri = params.getString("uri");
        ResourceSubscription sub = subscriptions.remove(uri);
        if (sub != null) {
            try { sub.close(); } catch (Exception ignore) {}
        }
        return new JsonRpcResponse(req.id(), Json.createObjectBuilder().build());
    }

    /** Notify clients that the list of resources has changed. */
    public void listChanged() throws IOException {
        send(new JsonRpcNotification("notifications/resources/list_changed", null));
    }
}
