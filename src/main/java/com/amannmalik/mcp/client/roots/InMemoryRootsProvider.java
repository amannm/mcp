package com.amannmalik.mcp.client.roots;

import com.amannmalik.mcp.util.ListChangeSupport;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InMemoryRootsProvider implements RootsProvider {
    private final List<Root> roots = new CopyOnWriteArrayList<>();
    private final ListChangeSupport<RootsListener> listChangeSupport = new ListChangeSupport<>();

    public InMemoryRootsProvider(List<Root> initial) {
        if (initial != null) roots.addAll(initial);
    }

    @Override
    public List<Root> list() {
        return List.copyOf(roots);
    }

    @Override
    public RootsSubscription subscribe(RootsListener listener) {
        var sub = listChangeSupport.subscribe(listener);
        return sub::close;
    }

    @Override
    public boolean supportsListChanged() {
        return true;
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
        listChangeSupport.notifyListeners();
    }
}
