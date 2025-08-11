package com.amannmalik.mcp.core;

import com.amannmalik.mcp.api.Change;
import com.amannmalik.mcp.completion.InMemoryCompletionProvider;
import com.amannmalik.mcp.prompts.InMemoryPromptProvider;
import com.amannmalik.mcp.resources.InMemoryResourceProvider;
import com.amannmalik.mcp.roots.InMemoryRootsProvider;
import com.amannmalik.mcp.spi.Pagination;
import com.amannmalik.mcp.spi.Provider;
import com.amannmalik.mcp.tools.InMemoryToolProvider;
import com.amannmalik.mcp.util.ChangeSupport;

import java.io.Closeable;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public sealed class InMemoryProvider<T> implements Provider<T> permits
        InMemoryCompletionProvider,
        InMemoryPromptProvider,
        InMemoryResourceProvider,
        InMemoryRootsProvider,
        InMemoryToolProvider {
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
    public Closeable subscribe(Consumer<Change> listener) {
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
