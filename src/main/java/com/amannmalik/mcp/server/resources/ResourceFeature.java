package com.amannmalik.mcp.server.resources;

import com.amannmalik.mcp.annotations.Annotations;
import com.amannmalik.mcp.auth.Principal;
import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.lifecycle.*;
import com.amannmalik.mcp.resources.*;
import com.amannmalik.mcp.security.ResourceAccessController;
import com.amannmalik.mcp.server.roots.RootsManager;
import com.amannmalik.mcp.util.*;
import com.amannmalik.mcp.validation.InputSanitizer;
import com.amannmalik.mcp.wire.NotificationMethod;
import com.amannmalik.mcp.wire.RequestMethod;
import jakarta.json.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class ResourceFeature implements AutoCloseable {
    private final ResourceProvider resources;
    private final ResourceAccessController access;
    private final Principal principal;
    private final RootsManager roots;
    private final ProtocolLifecycle lifecycle;
    private final Sender sender;
    private final Map<String, ResourceSubscription> subscriptions = new ConcurrentHashMap<>();
    private final ListChangeSubscription listSubscription;

    public ResourceFeature(ResourceProvider resources,
                           ResourceAccessController access,
                           Principal principal,
                           RootsManager roots,
                           ProtocolLifecycle lifecycle,
                           Sender sender) {
        this.resources = resources;
        this.access = access;
        this.principal = principal;
        this.roots = roots;
        this.lifecycle = lifecycle;
        this.sender = sender;
        this.listSubscription = resources.supportsListChanged() ?
                subscribeListChanges(
                        l -> resources.subscribeList(() -> l.listChanged()),
                        NotificationMethod.RESOURCES_LIST_CHANGED,
                        ResourcesCodec.toJsonObject(new ResourceListChangedNotification())) : null;
    }

    public void register(RpcHandlerRegistry handlers) {
        handlers.register(RequestMethod.RESOURCES_LIST, this::listResources);
        handlers.register(RequestMethod.RESOURCES_READ, this::readResource);
        handlers.register(RequestMethod.RESOURCES_TEMPLATES_LIST, this::listTemplates);
        if (resources.supportsSubscribe()) {
            handlers.register(RequestMethod.RESOURCES_SUBSCRIBE, this::subscribeResource);
            handlers.register(RequestMethod.RESOURCES_UNSUBSCRIBE, this::unsubscribeResource);
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
        try {
            ListResourcesRequest lr = valid(() -> ResourcesCodec.toListResourcesRequest(req.params()));
            String cursor = valid(() -> sanitizeCursor(lr.cursor()));
            Pagination.Page<Resource> list = valid(() -> resources.list(cursor));
            List<Resource> filtered = list.items().stream()
                    .filter(r -> allowed(r.annotations()) && withinRoots(r.uri()))
                    .toList();
            ListResourcesResult result = new ListResourcesResult(filtered, list.nextCursor(), null);
            return new JsonRpcResponse(req.id(), ResourcesCodec.toJsonObject(result));
        } catch (InvalidParams e) {
            return invalidParams(req, e.getMessage());
        }
    }

    private JsonRpcMessage readResource(JsonRpcRequest req) {
        ReadResourceRequest rrr;
        try {
            rrr = valid(() -> ResourcesCodec.toReadResourceRequest(req.params()));
        } catch (InvalidParams e) {
            return invalidParams(req, e.getMessage());
        }
        String uri = rrr.uri();
        if (!canAccessResource(uri)) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, "Access denied");
        }
        ResourceBlock block = resources.read(uri);
        if (block == null) {
            return JsonRpcError.of(req.id(), -32002, "Resource not found",
                    Json.createObjectBuilder().add("uri", uri).build());
        }
        ReadResourceResult result = new ReadResourceResult(List.of(block), null);
        return new JsonRpcResponse(req.id(), ResourcesCodec.toJsonObject(result));
    }

    private JsonRpcMessage listTemplates(JsonRpcRequest req) {
        try {
            ListResourceTemplatesRequest request =
                    valid(() -> ResourcesCodec.toListResourceTemplatesRequest(req.params()));
            String cursor = valid(() -> sanitizeCursor(request.cursor()));
            Pagination.Page<ResourceTemplate> page = valid(() -> resources.listTemplates(cursor));
            List<ResourceTemplate> filtered = page.items().stream()
                    .filter(t -> allowed(t.annotations()))
                    .toList();
            ListResourceTemplatesResult result = new ListResourceTemplatesResult(filtered, page.nextCursor(), null);
            return new JsonRpcResponse(req.id(), ResourcesCodec.toJsonObject(result));
        } catch (InvalidParams e) {
            return invalidParams(req, e.getMessage());
        }
    }

    private JsonRpcMessage subscribeResource(JsonRpcRequest req) {
        SubscribeRequest sr;
        try {
            sr = valid(() -> ResourcesCodec.toSubscribeRequest(req.params()));
        } catch (InvalidParams e) {
            return invalidParams(req, e.getMessage());
        }
        String uri = sr.uri();
        if (!canAccessResource(uri)) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, "Access denied");
        }
        ResourceBlock existing = resources.read(uri);
        if (existing == null) {
            return JsonRpcError.of(req.id(), -32002, "Resource not found",
                    Json.createObjectBuilder().add("uri", uri).build());
        }
        try {
            ResourceSubscription sub = resources.subscribe(uri, update -> {
                try {
                    ResourceUpdatedNotification n = new ResourceUpdatedNotification(update.uri(), update.title());
                    sender.send(new JsonRpcNotification(
                            NotificationMethod.RESOURCES_UPDATED.method(),
                            ResourcesCodec.toJsonObject(n)));
                } catch (IOException ignore) {
                }
            });
            ResourceSubscription prev = subscriptions.put(uri, sub);
            CloseUtil.closeQuietly(prev);
        } catch (Exception e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage());
        }
        return new JsonRpcResponse(req.id(), JsonValue.EMPTY_JSON_OBJECT);
    }

    private JsonRpcMessage unsubscribeResource(JsonRpcRequest req) {
        UnsubscribeRequest ur;
        try {
            ur = valid(() -> ResourcesCodec.toUnsubscribeRequest(req.params()));
        } catch (InvalidParams e) {
            return invalidParams(req, e.getMessage());
        }
        String uri = ur.uri();
        if (!canAccessResource(uri)) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, "Access denied");
        }
        ResourceSubscription sub = subscriptions.remove(uri);
        CloseUtil.closeQuietly(sub);
        return new JsonRpcResponse(req.id(), JsonValue.EMPTY_JSON_OBJECT);
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

    private String sanitizeCursor(String cursor) {
        return cursor == null ? null : Pagination.sanitize(InputSanitizer.cleanNullable(cursor));
    }

    private JsonRpcError invalidParams(JsonRpcRequest req, String message) {
        return JsonRpcError.invalidParams(req.id(), message);
    }

    private <T> T valid(Supplier<T> s) {
        try {
            return s.get();
        } catch (IllegalArgumentException e) {
            throw new InvalidParams(e.getMessage());
        }
    }

    private <S extends ListChangeSubscription> S subscribeListChanges(
            SubscriptionFactory<S> factory,
            NotificationMethod method,
            JsonObject payload) {
        try {
            return factory.subscribe(() -> {
                if (lifecycle.state() != LifecycleState.OPERATION) return;
                try {
                    sender.send(new JsonRpcNotification(method.method(), payload));
                } catch (IOException ignore) {
                }
            });
        } catch (RuntimeException ignore) {
            return null;
        }
    }

    @FunctionalInterface
    private interface SubscriptionFactory<S extends ListChangeSubscription> {
        S subscribe(ListChangeListener listener);
    }

    private static final class InvalidParams extends RuntimeException {
        InvalidParams(String message) { super(message); }
    }

    @FunctionalInterface
    public interface Sender {
        void send(JsonRpcMessage msg) throws IOException;
    }
}
