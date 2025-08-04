package com.amannmalik.mcp.roots;

import com.amannmalik.mcp.util.*;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InMemoryRootsProvider implements RootsProvider {
    private final List<Root> roots = new CopyOnWriteArrayList<>();
    private final ChangeSupport<Change> listChangeSupport = new ChangeSupport<>();

    public InMemoryRootsProvider(List<Root> initial) {
        if (initial != null) roots.addAll(initial);
    }

    @Override
    public Pagination.Page<Root> list(String cursor) {
        return Pagination.page(roots, cursor, Pagination.DEFAULT_PAGE_SIZE);
    }

    @Override
    public ChangeSubscription subscribe(ChangeListener<Change> listener) {
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
        listChangeSupport.notifyListeners(Change.INSTANCE);
    }
}
