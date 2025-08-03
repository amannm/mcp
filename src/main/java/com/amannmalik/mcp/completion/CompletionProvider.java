package com.amannmalik.mcp.completion;

/// - [Completion](specification/2025-06-18/server/utilities/completion.mdx)
public interface CompletionProvider extends AutoCloseable {
    CompleteResult complete(CompleteRequest request);

    @Override
    default void close() {
    }
}
