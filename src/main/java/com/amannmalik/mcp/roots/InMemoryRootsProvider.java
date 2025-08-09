package com.amannmalik.mcp.roots;

import com.amannmalik.mcp.transport.InMemoryProvider;

import java.util.List;

public final class InMemoryRootsProvider extends InMemoryProvider<Root> implements RootsProvider {
    public InMemoryRootsProvider(List<Root> initial) {
        super(initial);
    }

    public void add(Root root) {
        items.add(root);
        notifyListeners();
    }

    public void remove(String uri) {
        items.removeIf(r -> r.uri().equals(uri));
        notifyListeners();
    }
}
