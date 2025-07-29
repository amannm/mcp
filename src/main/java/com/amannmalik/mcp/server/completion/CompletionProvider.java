package com.amannmalik.mcp.server.completion;

import java.io.IOException;


public interface CompletionProvider extends AutoCloseable {
    CompleteResult complete(CompleteRequest request) throws IOException;

    @Override
    default void close() throws IOException {}
}
