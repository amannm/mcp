package com.amannmalik.mcp.core;

import com.amannmalik.mcp.api.ChangeSubscription;
import com.amannmalik.mcp.api.Pagination;
import com.amannmalik.mcp.util.Change;
import com.amannmalik.mcp.util.ChangeSupport;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class InMemoryProvider<T> implements Provider<T> {
    protected final List<T> items;
    private final ChangeSupport<Change> changeSupport = new ChangeSupport<>();

    public InMemoryProvider() {
        this(null);
    }

    public InMemoryProvider(Collection<T> initial) {
        items = initial == null ? new CopyOnWriteArrayList<>() : new CopyOnWriteArrayList<>(initial);
    }

    @Override
    public Pagination.Page<T> list(String cursor) {
        return Pagination.page(items, cursor, Pagination.DEFAULT_PAGE_SIZE);
    }

    @Override
    public ChangeSubscription subscribe(Consumer<Change> listener) {
        return changeSupport.subscribe(listener);
    }

    @Override
    public boolean supportsListChanged() {
        return true;
    }

    protected void notifyListeners() {
        changeSupport.notifyListeners(Change.INSTANCE);
    }
}
