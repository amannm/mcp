package com.amannmalik.mcp.client.roots;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public final class InMemoryRootsProvider implements RootsProvider {
    private final List<Root> roots = new CopyOnWriteArrayList<>();
    private final List<RootsListener> listeners = new CopyOnWriteArrayList<>();

    public InMemoryRootsProvider(List<Root> initial) {
        if (initial != null) roots.addAll(initial);
    }

    @Override
    public List<Root> list() {
        return List.copyOf(roots);
    }

    @Override
    public RootsSubscription subscribe(RootsListener listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public void add(Root root) {
        roots.add(root);
        notifyListeners();
    }

    public void remove(String uri) {
        roots.removeIf(r -> r.uri().equals(uri));
        notifyListeners();
    }

    private void notifyListeners() {
        listeners.forEach(RootsListener::listChanged);
    }
}
