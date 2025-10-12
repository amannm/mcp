package com.amannmalik.mcp.spi;

import java.io.Closeable;

public sealed interface Provider<T> extends Closeable permits NamedProvider, RootsProvider, ExecutingProvider {
    Pagination.Page<T> list(Cursor cursor);

    Closeable onListChanged(Runnable listener);

    boolean supportsListChanged();

}
