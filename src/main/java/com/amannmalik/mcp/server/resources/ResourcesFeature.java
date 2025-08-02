package com.amannmalik.mcp.server.resources;

import com.amannmalik.mcp.annotations.Annotations;
import com.amannmalik.mcp.auth.Principal;
import com.amannmalik.mcp.client.roots.Root;
import com.amannmalik.mcp.util.CloseUtil;
import com.amannmalik.mcp.util.RootChecker;
import com.amannmalik.mcp.util.Pagination;
import com.amannmalik.mcp.validation.InputSanitizer;
import com.amannmalik.mcp.security.ResourceAccessController;
import com.amannmalik.mcp.wire.NotificationMethod;
import com.amannmalik.mcp.wire.RequestMethod;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import com.amannmalik.mcp.jsonrpc.JsonRpcMessage;
import com.amannmalik.mcp.jsonrpc.JsonRpcNotification;
import com.amannmalik.mcp.jsonrpc.JsonRpcRequest;
import com.amannmalik.mcp.jsonrpc.JsonRpcResponse;
import com.amannmalik.mcp.jsonrpc.JsonRpcError;
import com.amannmalik.mcp.jsonrpc.JsonRpcErrorCode;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Handles server-side resource RPC methods. */
public final class ResourcesFeature implements AutoCloseable {
    private final ResourceProvider resources;
    private final ResourceAccessController access;
    private final Principal principal;
    private final List<Root> roots;
    private final Map<String, ResourceSubscription> subs = new ConcurrentHashMap<>();
    private ResourceListSubscription listSub;

    public ResourcesFeature(ResourceProvider resources,
                            ResourceAccessController access,
                            Principal principal,
                            List<Root> roots,
                            boolean listChangedSupported,
                            Consumer<JsonRpcNotification> notify) {
        this.resources = resources;
        this.access = access;
        this.principal = principal;
        this.roots = roots;
        if (listChangedSupported) {
            try {
                listSub = resources.subscribeList(() -> {
                    JsonObject payload = ResourcesCodec.toJsonObject(new ResourceListChangedNotification());
                    notify.accept(new JsonRpcNotification(NotificationMethod.RESOURCES_LIST_CHANGED.method(), payload));
                });
            } catch (Exception ignore) {
                listSub = null;
            }
        }
    }

    public boolean supportsSubscribe() {
        return resources.supportsSubscribe();
    }

    public JsonRpcMessage handle(RequestMethod method, JsonRpcRequest req, Consumer<JsonRpcNotification> notify) {
        return switch (method) {
            case RESOURCES_LIST -> listResources(req);
            case RESOURCES_READ -> readResource(req);
            case RESOURCES_TEMPLATES_LIST -> listTemplates(req);
            case RESOURCES_SUBSCRIBE -> subscribeResource(req, notify);
            case RESOURCES_UNSUBSCRIBE -> unsubscribeResource(req);
            default -> throw new IllegalArgumentException("Unsupported method: " + method);
        };
    }

    private boolean allowed(Annotations ann) {
        try {
            access.requireAllowed(principal, ann);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    private boolean withinRoots(String uri) {
        return RootChecker.withinRoots(uri, roots);
    }

    private boolean canAccessResource(String uri) {
        if (!withinRoots(uri)) return false;
        return resources.get(uri)
                .map(Resource::annotations)
                .map(this::allowed)
                .orElse(true);
    }

    private static String sanitizeCursor(String cursor) {
        return cursor == null ? null : Pagination.sanitize(InputSanitizer.cleanNullable(cursor));
    }

    private JsonRpcMessage listResources(JsonRpcRequest req) {
        ListResourcesRequest lr = ResourcesCodec.toListResourcesRequest(req.params());
        String cursor;
        try {
            cursor = sanitizeCursor(lr.cursor());
        } catch (IllegalArgumentException e) {
            return invalidParams(req, e);
        }
        var list = resources.list(cursor);
        List<Resource> filtered = list.items().stream()
                .filter(r -> allowed(r.annotations()) && withinRoots(r.uri()))
                .toList();
        ListResourcesResult result = new ListResourcesResult(filtered, list.nextCursor(), null);
        return new JsonRpcResponse(req.id(), ResourcesCodec.toJsonObject(result));
    }

    private JsonRpcMessage readResource(JsonRpcRequest req) {
        ReadResourceRequest rrr;
        try {
            rrr = ResourcesCodec.toReadResourceRequest(req.params());
        } catch (IllegalArgumentException e) {
            return invalidParams(req, e);
        }
        String uri = rrr.uri();
        if (!canAccessResource(uri)) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, "Access denied");
        }
        ResourceBlock block = resources.read(uri);
        if (block == null) {
            return JsonRpcError.of(req.id(), -32002, "Resource not found", Json.createObjectBuilder().add("uri", uri).build());
        }
        ReadResourceResult result = new ReadResourceResult(List.of(block), null);
        return new JsonRpcResponse(req.id(), ResourcesCodec.toJsonObject(result));
    }

    private JsonRpcMessage listTemplates(JsonRpcRequest req) {
        ListResourceTemplatesRequest request;
        try {
            request = ResourcesCodec.toListResourceTemplatesRequest(req.params());
        } catch (IllegalArgumentException e) {
            return invalidParams(req, e);
        }
        String cursor;
        try {
            cursor = sanitizeCursor(request.cursor());
        } catch (IllegalArgumentException e) {
            return invalidParams(req, e);
        }
        var page = resources.listTemplates(cursor);
        List<ResourceTemplate> filtered = page.items().stream()
                .filter(t -> allowed(t.annotations()))
                .toList();
        ListResourceTemplatesResult result = new ListResourceTemplatesResult(filtered, page.nextCursor(), null);
        return new JsonRpcResponse(req.id(), ResourcesCodec.toJsonObject(result));
    }

    private JsonRpcMessage subscribeResource(JsonRpcRequest req, Consumer<JsonRpcNotification> notify) {
        SubscribeRequest sr;
        try {
            sr = ResourcesCodec.toSubscribeRequest(req.params());
        } catch (IllegalArgumentException e) {
            return invalidParams(req, e);
        }
        String uri = sr.uri();
        if (!canAccessResource(uri)) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, "Access denied");
        }
        ResourceBlock existing = resources.read(uri);
        if (existing == null) {
            return JsonRpcError.of(req.id(), -32002, "Resource not found", Json.createObjectBuilder().add("uri", uri).build());
        }
        try {
            ResourceSubscription sub = resources.subscribe(uri, update -> {
                JsonObject payload = ResourcesCodec.toJsonObject(new ResourceUpdatedNotification(update.uri(), update.title()));
                notify.accept(new JsonRpcNotification(NotificationMethod.RESOURCES_UPDATED.method(), payload));
            });
            ResourceSubscription prev = subs.put(uri, sub);
            CloseUtil.closeQuietly(prev);
        } catch (Exception e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage());
        }
        return new JsonRpcResponse(req.id(), JsonValue.EMPTY_JSON_OBJECT);
    }

    private JsonRpcMessage unsubscribeResource(JsonRpcRequest req) {
        UnsubscribeRequest ur;
        try {
            ur = ResourcesCodec.toUnsubscribeRequest(req.params());
        } catch (IllegalArgumentException e) {
            return invalidParams(req, e);
        }
        String uri = ur.uri();
        if (!canAccessResource(uri)) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, "Access denied");
        }
        ResourceSubscription sub = subs.remove(uri);
        CloseUtil.closeQuietly(sub);
        return new JsonRpcResponse(req.id(), JsonValue.EMPTY_JSON_OBJECT);
    }

    private static JsonRpcError invalidParams(JsonRpcRequest req, IllegalArgumentException e) {
        return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
    }

    @Override
    public void close() {
        for (ResourceSubscription sub : subs.values()) CloseUtil.closeQuietly(sub);
        subs.clear();
        if (listSub != null) {
            CloseUtil.closeQuietly(listSub);
            listSub = null;
        }
    }
}
