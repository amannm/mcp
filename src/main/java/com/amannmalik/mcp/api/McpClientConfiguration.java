package com.amannmalik.mcp.api;

import com.amannmalik.mcp.spi.SamplingAccessPolicy;

import java.util.List;
import java.util.Set;

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

        long timeoutMs,
        long pingTimeoutMs,
        int progressPerSecond,
        long rateLimiterWindowMs,

        // Client-specific behavior
        boolean verbose,
        boolean interactiveSampling,
        List<String> rootDirectories,
        long pingIntervalMs,
        SamplingAccessPolicy samplingAccessPolicy,
        String principal
) {

    public McpClientConfiguration {
        clientCapabilities = Set.copyOf(clientCapabilities);
        rootDirectories = List.copyOf(rootDirectories);
        if (timeoutMs <= 0)
            throw new IllegalArgumentException("Invalid timeout configuration");
        if (pingTimeoutMs <= 0)
            throw new IllegalArgumentException("Invalid ping timeout configuration");
        if (progressPerSecond < 0)
            throw new IllegalArgumentException("Invalid progress rate configuration");
        if (rateLimiterWindowMs <= 0)
            throw new IllegalArgumentException("Invalid rate limiter window");
        if (pingIntervalMs < 0)
            throw new IllegalArgumentException("Invalid ping interval configuration");
        if (samplingAccessPolicy == null)
            throw new IllegalArgumentException("Invalid sampling access configuration");
        if (principal == null || principal.isBlank())
            throw new IllegalArgumentException("Invalid principal configuration");
    }
}