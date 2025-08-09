package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.annotations.Annotations;
import com.amannmalik.mcp.auth.Principal;
import com.amannmalik.mcp.core.JsonRpcEndpoint;
import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.lifecycle.LifecycleState;
import com.amannmalik.mcp.core.NotificationMethod;
import com.amannmalik.mcp.core.RequestMethod;
import com.amannmalik.mcp.roots.RootsManager;
import com.amannmalik.mcp.security.ResourceAccessController;
import com.amannmalik.mcp.util.RootChecker;
import com.amannmalik.mcp.util.*;
import com.amannmalik.mcp.validation.ValidationUtil;
import jakarta.json.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ResourceFeature implements AutoCloseable {
    private final ResourceProvider resources;
    private final ResourceAccessController access;
    private final Principal principal;
    private final RootsManager roots;
    private final Supplier<LifecycleState> state;
    private final Sender sender;
    private final ProgressManager progress;
    private final Map<String, ChangeSubscription> subscriptions = new ConcurrentHashMap<>();
    private final ChangeSubscription listSubscription;

    public ResourceFeature(ResourceProvider resources,
                           ResourceAccessController access,
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
                        ResourceListChangedNotification.CODEC.toJson(new ResourceListChangedNotification())) : null;
    }

    public void register(JsonRpcEndpoint endpoint) {
        endpoint.registerRequest(RequestMethod.RESOURCES_LIST.method(), this::listResources);
        endpoint.registerRequest(RequestMethod.RESOURCES_READ.method(), this::readResource);
        endpoint.registerRequest(RequestMethod.RESOURCES_TEMPLATES_LIST.method(), this::listTemplates);
        if (resources.supportsSubscribe()) {
            endpoint.registerRequest(RequestMethod.RESOURCES_SUBSCRIBE.method(), this::subscribeResource);
            endpoint.registerRequest(RequestMethod.RESOURCES_UNSUBSCRIBE.method(), this::unsubscribeResource);
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
        Optional<ProgressToken> progressToken = ProgressNotification.fromMeta(req.params());
        try {
            ListResourcesRequest lr = ListResourcesRequest.CODEC.fromJson(req.params());
            String cursor = sanitizeCursor(lr.cursor());
            progressToken.ifPresent(t -> {
                try {
                    progress.send(new ProgressNotification(t, 0.0, null, "Starting resource list"), sender::send);
                } catch (IOException ignore) {
                }
            });
            Pagination.Page<Resource> list = resources.list(cursor);
            progressToken.ifPresent(t -> {
                try {
                    progress.send(new ProgressNotification(t, 0.5, null, "Filtering resources"), sender::send);
                } catch (IOException ignore) {
                }
            });
            List<Resource> filtered = list.items().stream()
                    .filter(r -> allowed(r.annotations()) && withinRoots(r.uri()))
                    .toList();
            progressToken.ifPresent(t -> {
                try {
                    progress.send(new ProgressNotification(t, 1.0, null, "Completed resource list"), sender::send);
                } catch (IOException ignore) {
                }
            });
            ListResourcesResult result = new ListResourcesResult(filtered, list.nextCursor(), null);
            return new JsonRpcResponse(req.id(), ListResourcesResult.CODEC.toJson(result));
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        }
    }

    private JsonRpcMessage readResource(JsonRpcRequest req) {
        ReadResourceRequest rrr;
        try {
            rrr = ReadResourceRequest.CODEC.fromJson(req.params());
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        }
        return withExistingResource(req, rrr.uri(), block -> {
            ReadResourceResult result = new ReadResourceResult(List.of(block), null);
            return new JsonRpcResponse(req.id(), ReadResourceResult.CODEC.toJson(result));
        });
    }

    private JsonRpcMessage listTemplates(JsonRpcRequest req) {
        try {
            ListResourceTemplatesRequest request =
                    ListResourceTemplatesRequest.CODEC.fromJson(req.params());
            String cursor = sanitizeCursor(request.cursor());
            Pagination.Page<ResourceTemplate> page = resources.listTemplates(cursor);
            List<ResourceTemplate> filtered = page.items().stream()
                    .filter(t -> allowed(t.annotations()))
                    .toList();
            ListResourceTemplatesResult result = new ListResourceTemplatesResult(filtered, page.nextCursor(), null);
            return new JsonRpcResponse(req.id(), ListResourceTemplatesResult.CODEC.toJson(result));
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        }
    }

    private JsonRpcMessage subscribeResource(JsonRpcRequest req) {
        SubscribeRequest sr;
        try {
            sr = SubscribeRequest.CODEC.fromJson(req.params());
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
                ChangeSubscription sub = resources.subscribe(uri, update -> {
                    try {
                        ResourceUpdatedNotification n = new ResourceUpdatedNotification(update.uri(), update.title());
                        sender.send(new JsonRpcNotification(
                                NotificationMethod.RESOURCES_UPDATED.method(),
                                ResourceUpdatedNotification.CODEC.toJson(n)));
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
            ur = UnsubscribeRequest.CODEC.fromJson(req.params());
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        }
        String uri = ur.uri();
        return withAccessibleUri(req, uri, () -> {
            if (!subscriptions.containsKey(uri)) {
                return JsonRpcError.of(req.id(), -32602, "No active subscription for resource",
                        Json.createObjectBuilder().add("uri", uri).build());
            }
            ChangeSubscription sub = subscriptions.remove(uri);
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

    private String sanitizeCursor(String cursor) {
        return cursor == null ? null : Pagination.sanitize(ValidationUtil.cleanNullable(cursor));
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

    private <S extends ChangeSubscription> S subscribeListChanges(
            SubscriptionFactory<S> factory,
            NotificationMethod method,
            JsonObject payload) {
        try {
            return factory.subscribe(ignored -> {
                if (state.get() != LifecycleState.OPERATION) return;
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
    private interface SubscriptionFactory<S extends ChangeSubscription> {
        S subscribe(ChangeListener<Change> listener);
    }

    @FunctionalInterface
    public interface Sender {
        void send(JsonRpcMessage msg) throws IOException;
    }
}
