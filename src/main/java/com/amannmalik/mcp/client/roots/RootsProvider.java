package com.amannmalik.mcp.client.roots;

import java.io.IOException;
import java.util.List;

/** Supplies filesystem roots to a server. */
public interface RootsProvider extends AutoCloseable {
    /** Current roots. */
    List<Root> list() throws IOException;

    /** Subscribe to root list changes. */
    RootsSubscription subscribe(RootsListener listener) throws IOException;

    @Override
    default void close() throws IOException {}
}
