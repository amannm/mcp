package com.amannmalik.mcp.server.completion;

import java.io.IOException;


public interface CompletionProvider extends AutoCloseable {
    CompleteResult complete(CompleteRequest request);

    @Override
    default void close() {}
}
