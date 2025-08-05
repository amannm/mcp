package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.util.ChangeListener;
import com.amannmalik.mcp.util.ChangeSubscription;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ResourceSubscriptionManager {
    private final ResourceProvider provider;
    private final Map<String, ChangeSubscription> subscriptions = new ConcurrentHashMap<>();

    public ResourceSubscriptionManager(ResourceProvider provider) {
        if (provider == null) throw new IllegalArgumentException("provider required");
        this.provider = provider;
    }

    public void subscribe(String uri, ChangeListener<ResourceUpdate> listener) {
        if (uri == null || listener == null) {
            throw new IllegalArgumentException("uri and listener required");
        }
        unsubscribe(uri);
        subscriptions.put(uri, provider.subscribe(uri, listener));
    }

    public void unsubscribe(String uri) {
        ChangeSubscription sub = subscriptions.remove(uri);
        if (sub != null) sub.close();
    }

    public void clear() {
        subscriptions.values().forEach(ChangeSubscription::close);
        subscriptions.clear();
    }
}
