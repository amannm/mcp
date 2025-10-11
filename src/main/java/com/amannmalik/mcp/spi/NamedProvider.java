package com.amannmalik.mcp.spi;

import java.util.Optional;

public sealed interface NamedProvider<T extends DisplayNameProvider> extends Provider<T>
        permits PromptProvider, ResourceProvider, ToolProvider {
    Optional<T> find(String name);
}
