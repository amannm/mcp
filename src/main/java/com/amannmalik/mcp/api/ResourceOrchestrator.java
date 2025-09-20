package com.amannmalik.mcp.api;

import com.amannmalik.mcp.codec.*;
import com.amannmalik.mcp.core.LifecycleState;
import com.amannmalik.mcp.core.ProgressManager;
import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.resources.ReadResourceResult;
import com.amannmalik.mcp.resources.ResourceListChangedNotification;
import com.amannmalik.mcp.roots.RootsManager;
import com.amannmalik.mcp.spi.*;
import com.amannmalik.mcp.util.*;
import jakarta.json.*;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

final class ResourceOrchestrator implements AutoCloseable {
    private static final JsonCodec<ResourceUpdatedNotification> RESOURCE_UPDATED_NOTIFICATION_JSON_CODEC = new ResourceUpdatedNotificationAbstractEntityCodec();
    private static final JsonCodec<ResourceListChangedNotification> RESOURCE_LIST_CHANGED_NOTIFICATION_JSON_CODEC = new ResourceListChangedNotificationJsonCodec();
    private final ResourceProvider resources;
    private final ResourceAccessPolicy access;
    private final Principal principal;
    private final RootsManager roots;
    private final Supplier<LifecycleState> state;
    private final Sender sender;
    private final ProgressManager progress;
    private final Map<URI, AutoCloseable> subscriptions = new ConcurrentHashMap<>();
    private final AutoCloseable listSubscription;

    public ResourceOrchestrator(ResourceProvider resources,
                                ResourceAccessPolicy access,
                                Principal principal,
                                RootsManager roots,
                                Supplier<LifecycleState> state,
                                Sender sender,
                                ProgressManager progress) {
        this.resources = resources;
        this.access = access;
        this.principal = principal;
        this.roots = roots;
        this.state = state;
        this.sender = sender;
        this.progress = progress;
        this.listSubscription = resources.supportsListChanged() ?
                SubscriptionUtil.subscribeListChanges(
                        state,
                        resources::onListChanged,
                        () -> sender.sendNotification(
                                NotificationMethod.RESOURCES_LIST_CHANGED,
                                RESOURCE_LIST_CHANGED_NOTIFICATION_JSON_CODEC.toJson(new ResourceListChangedNotification()))) : null;
    }

    public void register(JsonRpcEndpoint endpoint) {
        endpoint.registerRequest(RequestMethod.RESOURCES_LIST, this::listResources);
        endpoint.registerRequest(RequestMethod.RESOURCES_READ, this::readResource);
        endpoint.registerRequest(RequestMethod.RESOURCES_TEMPLATES_LIST, this::listTemplates);
        if (resources.supportsSubscribe()) {
            endpoint.registerRequest(RequestMethod.RESOURCES_SUBSCRIBE, this::subscribeResource);
            endpoint.registerRequest(RequestMethod.RESOURCES_UNSUBSCRIBE, this::unsubscribeResource);
        }
    }

    public boolean supportsSubscribe() {
        return resources.supportsSubscribe();
    }

    public boolean supportsListChanged() {
        return resources.supportsListChanged();
    }

    @Override
    public void close() {
        subscriptions.values().forEach(CloseUtil::close);
        CloseUtil.close(listSubscription);
        resources.close();
    }

    private JsonRpcMessage listResources(JsonRpcRequest req) {
        if (state.get() != LifecycleState.OPERATION) {
            return JsonRpcError.of(req.id(), -32002, "Server not initialized");
        }
        var progressToken = ProgressToken.fromMeta(req.params());
        try {
            var pageReq = PaginatedRequest.CODEC.fromJson(req.params());
            var cursor = CursorUtil.sanitize(pageReq.cursor());
            progressToken.ifPresent(t -> sendProgress(t, 0.0, "Starting resource list"));
            var list = resources.list(cursor);
            progressToken.ifPresent(t -> sendProgress(t, 0.5, "Filtering resources"));
            var filtered = list.items().stream()
                    .filter(r -> allowed(r.annotations()) && withinRoots(r.uri()))
                    .toList();
            progressToken.ifPresent(t -> sendProgress(t, 1.0, "Completed resource list"));
            var result = new ListResourcesResult(filtered, list.nextCursor(), null);
            return new JsonRpcResponse(req.id(), AbstractEntityCodec.paginatedResult(
                    "resources",
                    "resource",
                    r -> new Pagination.Page<>(r.resources(), r.nextCursor()),
                    ListResourcesResult::_meta,
                    new ResourceAbstractEntityCodec(),
                    (page, meta) -> new ListResourcesResult(page.items(), page.nextCursor(), meta)).toJson(result));
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        }
    }

    private JsonRpcMessage readResource(JsonRpcRequest req) {
        ReadResourceRequest rrr;
        try {
            rrr = (new ReadResourceRequestAbstractEntityCodec()).fromJson(req.params());
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, "Invalid params");
        }
        return withExistingResource(req, rrr.uri(), block -> {
            var result = new ReadResourceResult(List.of(block), null);
            return new JsonRpcResponse(req.id(), new ReadResourceResultJsonCodec().toJson(result));
        });
    }

    private JsonRpcMessage listTemplates(JsonRpcRequest req) {
        try {
            var pageReq = PaginatedRequest.CODEC.fromJson(req.params());
            var cursor = CursorUtil.sanitize(pageReq.cursor());
            var page = resources.listTemplates(cursor);
            var filtered = page.items().stream()
                    .filter(t -> allowed(t.annotations()))
                    .toList();
            var result = new ListResourceTemplatesResult(filtered, page.nextCursor(), null);
            return new JsonRpcResponse(req.id(), AbstractEntityCodec.paginatedResult(
                    "resourceTemplates",
                    "resourceTemplate",
                    r -> new Pagination.Page<>(r.resourceTemplates(), r.nextCursor()),
                    ListResourceTemplatesResult::_meta,
                    new ResourceTemplateAbstractEntityCodec(),
                    (page1, meta) -> new ListResourceTemplatesResult(page1.items(), page1.nextCursor(), meta)).toJson(result));
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        }
    }

    private JsonRpcMessage subscribeResource(JsonRpcRequest req) {
        SubscribeRequest sr;
        try {
            sr = (new SubscribeRequestAbstractEntityCodec()).fromJson(req.params());
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        }
        var uri = sr.uri();
        return withAccessibleUri(req, uri, () -> {
            if (resources.get(uri).isEmpty()) {
                return JsonRpcError.of(req.id(), -32002, "Resource not found",
                        Json.createObjectBuilder().add("uri", uri.toString()).build());
            }
            if (subscriptions.containsKey(uri)) {
                return JsonRpcError.of(req.id(), -32602, "Already subscribed to resource",
                        Json.createObjectBuilder().add("uri", uri.toString()).build());
            }
            try {
                var sub = resources.subscribe(uri, update -> {
                    try {
                        var n = new ResourceUpdatedNotification(update.uri(), update.title());
                        sender.sendNotification(NotificationMethod.RESOURCES_UPDATED,
                                RESOURCE_UPDATED_NOTIFICATION_JSON_CODEC.toJson(n));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                subscriptions.put(uri, sub);
            } catch (Exception e) {
                return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage());
            }
            return new JsonRpcResponse(req.id(), JsonValue.EMPTY_JSON_OBJECT);
        });
    }

    private JsonRpcMessage unsubscribeResource(JsonRpcRequest req) {
        UnsubscribeRequest ur;
        try {
            ur = (new UnsubscribeRequestAbstractEntityCodec()).fromJson(req.params());
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        }
        var uri = ur.uri();
        return withAccessibleUri(req, uri, () -> {
            if (!subscriptions.containsKey(uri)) {
                return JsonRpcError.of(req.id(), -32602, "No active subscription for resource",
                        Json.createObjectBuilder().add("uri", uri.toString()).build());
            }
            var sub = subscriptions.remove(uri);
            CloseUtil.close(sub);
            return new JsonRpcResponse(req.id(), JsonValue.EMPTY_JSON_OBJECT);
        });
    }

    private boolean allowed(Annotations ann) {
        try {
            access.requireAllowed(principal, ann);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    private boolean withinRoots(URI uri) {
        return RootChecker.withinRoots(uri, roots.roots());
    }

    private boolean canAccessResource(URI uri) {
        if (!withinRoots(uri)) {
            return false;
        }
        return resources.get(uri)
                .map(Resource::annotations)
                .map(this::allowed)
                .orElse(true);
    }

    private void sendProgress(ProgressToken token, double current, String message) {
        try {
            progress.send(new ProgressNotification(token, current, null, message), sender::sendNotification);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonRpcMessage withAccessibleUri(JsonRpcRequest req, URI uri, Supplier<JsonRpcMessage> action) {
        if (!canAccessResource(uri)) {
            return JsonRpcError.of(req.id(), -32002, "Resource not found",
                    Json.createObjectBuilder().add("uri", uri.toString()).build());
        }
        return action.get();
    }

    private JsonRpcMessage withExistingResource(JsonRpcRequest req, URI uri, Function<ResourceBlock, JsonRpcMessage> action) {
        var block = resources.read(uri);
        if (block == null) {
            return JsonRpcError.of(req.id(), -32002, "Resource not found",
                    Json.createObjectBuilder().add("uri", uri.toString()).build());
        }
        if (!canAccessResource(uri)) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, "Access denied");
        }
        return action.apply(block);
    }

    @FunctionalInterface
    public interface Sender {
        void sendNotification(NotificationMethod method, JsonObject payload) throws IOException;
    }
}
