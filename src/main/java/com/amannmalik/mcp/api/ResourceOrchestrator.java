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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;

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
    private final Map<String, AutoCloseable> subscriptions = new ConcurrentHashMap<>();
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
                subscribeListChanges(
                        resources::subscribe,
                        NotificationMethod.RESOURCES_LIST_CHANGED,
                        RESOURCE_LIST_CHANGED_NOTIFICATION_JSON_CODEC.toJson(new ResourceListChangedNotification())) : null;
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
        subscriptions.values().forEach(CloseUtil::closeQuietly);
        CloseUtil.closeQuietly(listSubscription);
        resources.close();
    }

    private JsonRpcMessage listResources(JsonRpcRequest req) {
        if (state.get() != LifecycleState.OPERATION) {
            return JsonRpcError.of(req.id(), -32002, "Server not initialized");
        }
        Optional<ProgressToken> progressToken = ProgressToken.fromMeta(req.params());
        try {
            ListResourcesRequest lr = AbstractEntityCodec.paginatedRequest(
                    ListResourcesRequest::cursor,
                    ListResourcesRequest::_meta,
                    ListResourcesRequest::new).fromJson(req.params());
            Cursor cursor = sanitizeCursor(lr.cursor());
            progressToken.ifPresent(t -> {
                try {
                    progress.send(new ProgressNotification(t, 0.0, null, "Starting resource list"), sender::assNotify);
                } catch (IOException ignore) {
                }
            });
            Pagination.Page<Resource> list = resources.list(cursor);
            progressToken.ifPresent(t -> {
                try {
                    progress.send(new ProgressNotification(t, 0.5, null, "Filtering resources"), sender::assNotify);
                } catch (IOException ignore) {
                }
            });
            List<Resource> filtered = list.items().stream()
                    .filter(r -> allowed(r.annotations()) && withinRoots(r.uri()))
                    .toList();
            progressToken.ifPresent(t -> {
                try {
                    progress.send(new ProgressNotification(t, 1.0, null, "Completed resource list"), sender::assNotify);
                } catch (IOException ignore) {
                }
            });
            ListResourcesResult result = new ListResourcesResult(filtered, list.nextCursor(), null);
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
            rrr = ((JsonCodec<ReadResourceRequest>) new ReadResourceRequestAbstractEntityCodec()).fromJson(req.params());
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        }
        return withExistingResource(req, rrr.uri(), block -> {
            ReadResourceResult result = new ReadResourceResult(List.of(block), null);
            return new JsonRpcResponse(req.id(), new ReadResourceResultJsonCodec().toJson(result));
        });
    }

    private JsonRpcMessage listTemplates(JsonRpcRequest req) {
        try {
            ListResourceTemplatesRequest request =
                    AbstractEntityCodec.paginatedRequest(
                            ListResourceTemplatesRequest::cursor,
                            ListResourceTemplatesRequest::_meta,
                            ListResourceTemplatesRequest::new).fromJson(req.params());
            Cursor cursor = sanitizeCursor(request.cursor());
            Pagination.Page<ResourceTemplate> page = resources.listTemplates(cursor);
            List<ResourceTemplate> filtered = page.items().stream()
                    .filter(t -> allowed(t.annotations()))
                    .toList();
            ListResourceTemplatesResult result = new ListResourceTemplatesResult(filtered, page.nextCursor(), null);
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
            sr = ((JsonCodec<SubscribeRequest>) new SubscribeRequestAbstractEntityCodec()).fromJson(req.params());
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        }
        String uri = sr.uri();
        return withExistingResource(req, uri, block -> {
            if (subscriptions.containsKey(uri)) {
                return JsonRpcError.of(req.id(), -32602, "Already subscribed to resource",
                        Json.createObjectBuilder().add("uri", uri).build());
            }
            try {
                AutoCloseable sub = resources.subscribe(uri, update -> {
                    try {
                        ResourceUpdatedNotification n = new ResourceUpdatedNotification(update.uri(), update.title());
                        sender.assNotify(NotificationMethod.RESOURCES_UPDATED,
                                RESOURCE_UPDATED_NOTIFICATION_JSON_CODEC.toJson(n));
                    } catch (IOException ignore) {
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
            ur = ((JsonCodec<UnsubscribeRequest>) new UnsubscribeRequestAbstractEntityCodec()).fromJson(req.params());
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        }
        String uri = ur.uri();
        return withAccessibleUri(req, uri, () -> {
            if (!subscriptions.containsKey(uri)) {
                return JsonRpcError.of(req.id(), -32602, "No active subscription for resource",
                        Json.createObjectBuilder().add("uri", uri).build());
            }
            AutoCloseable sub = subscriptions.remove(uri);
            CloseUtil.closeQuietly(sub);
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

    private boolean withinRoots(String uri) {
        return RootChecker.withinRoots(uri, roots.roots());
    }

    private boolean canAccessResource(String uri) {
        if (!withinRoots(uri)) return false;
        return resources.get(uri)
                .map(Resource::annotations)
                .map(this::allowed)
                .orElse(true);
    }

    private Cursor sanitizeCursor(String cursor) {
        if (cursor == null) return Cursor.Start.INSTANCE;
        String clean = ValidationUtil.cleanNullable(cursor);
        return new Cursor.Token(clean);
    }

    private JsonRpcMessage withAccessibleUri(JsonRpcRequest req, String uri, Supplier<JsonRpcMessage> action) {
        if (!canAccessResource(uri)) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, "Access denied");
        }
        return action.get();
    }

    private JsonRpcMessage withExistingResource(JsonRpcRequest req, String uri, Function<ResourceBlock, JsonRpcMessage> action) {
        return withAccessibleUri(req, uri, () -> {
            ResourceBlock block = resources.read(uri);
            if (block == null) {
                return JsonRpcError.of(req.id(), -32002, "Resource not found",
                        Json.createObjectBuilder().add("uri", uri).build());
            }
            return action.apply(block);
        });
    }

    private AutoCloseable subscribeListChanges(
            SubscriptionFactory<AutoCloseable> factory,
            NotificationMethod method,
            JsonObject payload) {
        try {
            return factory.subscribe(ignored -> {
                if (state.get() != LifecycleState.OPERATION) return;
                try {
                    sender.assNotify(method, payload);
                } catch (IOException ignore) {
                }
            });
        } catch (RuntimeException ignore) {
            return null;
        }
    }

    @FunctionalInterface
    private interface SubscriptionFactory<S extends AutoCloseable> {
        S subscribe(Consumer<Change> listener);
    }

    @FunctionalInterface
    public interface Sender {
        void assNotify(NotificationMethod method, JsonObject payload) throws IOException;
    }
}
