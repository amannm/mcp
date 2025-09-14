package com.amannmalik.mcp.roots;

import com.amannmalik.mcp.core.InMemoryProvider;
import com.amannmalik.mcp.spi.Root;
import com.amannmalik.mcp.spi.RootsProvider;

import java.net.URI;
import java.util.List;

public final class InMemoryRootsProvider extends InMemoryProvider<Root> implements RootsProvider {
    public InMemoryRootsProvider(List<Root> initial) {
        // Defensive copy to avoid storing a caller-provided mutable list.
        super(initial == null ? null : List.copyOf(initial));
    }

    public void add(Root root) {
        items.add(root);
        notifyListChanged();
    }

    public void remove(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException("uri required");
        }
        items.removeIf(r -> r.uri().equals(uri));
        notifyListChanged();
    }
}
