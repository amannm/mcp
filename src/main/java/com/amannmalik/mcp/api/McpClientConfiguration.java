package com.amannmalik.mcp.api;

import java.util.*;

public record McpClientConfiguration(
        // Client connection identity
        String clientId,
        
        // MCP server identity (that we're connecting to)
        String serverName,
        String serverDisplayName,
        String serverVersion,

        // Client capabilities (what this client connection supports)
        Set<ClientCapability> clientCapabilities,

        // Transport configuration
        String commandSpec,

        Long timeoutMs,
        Long pingTimeoutMs,
        Integer progressPerSecond,
        Long rateLimiterWindowMs,

        // Client-specific behavior
        boolean verbose,
        boolean interactiveSampling,
        List<String> rootDirectories
) {

    public McpClientConfiguration {
        clientCapabilities = Set.copyOf(clientCapabilities);
        rootDirectories = List.copyOf(rootDirectories);
        if (timeoutMs == null || timeoutMs <= 0)
            throw new IllegalArgumentException("Invalid timeout configuration");
        if (pingTimeoutMs == null || pingTimeoutMs <= 0)
            throw new IllegalArgumentException("Invalid ping timeout configuration");
        if (progressPerSecond == null || progressPerSecond < 0)
            throw new IllegalArgumentException("Invalid progress rate configuration");
        if (rateLimiterWindowMs == null || rateLimiterWindowMs <= 0)
            throw new IllegalArgumentException("Invalid rate limiter window");
    }
}