package com.amannmalik.mcp.server.completion;

public interface CompletionProvider extends AutoCloseable {
    CompleteResult complete(CompleteRequest request);

    @Override
    default void close() {
    }
}
