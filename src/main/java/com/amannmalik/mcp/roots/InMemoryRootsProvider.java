package com.amannmalik.mcp.roots;

import com.amannmalik.mcp.util.ListChangeSubscription;
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
    public ListChangeSubscription subscribe(RootsListener listener) {
        return listChangeSupport.subscribe(listener);
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
