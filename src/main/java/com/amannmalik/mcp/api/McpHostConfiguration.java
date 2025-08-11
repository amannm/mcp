package com.amannmalik.mcp.api;

import java.util.*;

public record McpHostConfiguration(
        // Protocol configuration
        String version,
        String compatibilityVersion,
        long defaultTimeoutMs,
        long pingTimeoutMs,

        // Client identity configuration
        String clientName,
        String clientDisplayName,
        String clientVersion,

        // Client capabilities configuration
        Set<ClientCapability> clientCapabilities,

        // Host principal configuration
        String hostPrincipal,

        // Rate limiting configuration (for client operations)
        int progressPerSecond,
        long rateLimiterWindowMs,

        // Transport configuration
        String transportType,
        int port,
        List<String> allowedOrigins,
        int sseHistoryLimit,
        int responseQueueCapacity,

        // Process configuration
        int processWaitSeconds,
        int defaultPageSize,
        int maxCompletionValues,

        // Client specifications
        Map<String, String> clientSpecs,

        // Logging configuration
        boolean verbose
) {

    public McpHostConfiguration {
        clientCapabilities = Set.copyOf(clientCapabilities);
        allowedOrigins = List.copyOf(allowedOrigins);
        clientSpecs = Map.copyOf(clientSpecs);
        if (defaultTimeoutMs <= 0 || pingTimeoutMs <= 0)
            throw new IllegalArgumentException("Invalid timeout configuration");
        if (progressPerSecond < 0)
            throw new IllegalArgumentException("Invalid progress rate configuration");
        if (rateLimiterWindowMs <= 0)
            throw new IllegalArgumentException("Invalid rate limiter window");
        if (processWaitSeconds <= 0)
            throw new IllegalArgumentException("Invalid process wait seconds");
        if (port < 0 || port > 65_535)
            throw new IllegalArgumentException("Invalid port number");
        if (defaultPageSize <= 0 || maxCompletionValues <= 0 || responseQueueCapacity <= 0)
            throw new IllegalArgumentException("Invalid pagination configuration");
        if (sseHistoryLimit < 0)
            throw new IllegalArgumentException("Invalid SSE history limit");
    }

    public static McpHostConfiguration defaultConfiguration() {
        return new McpHostConfiguration(
                "2025-06-18",
                "2025-03-26",
                30_000L,
                5_000L,
                "cli",
                "CLI",
                "0",
                defaultClientCapabilities(),
                "user",
                20,
                1_000L,
                "stdio",
                0,
                List.of("http://localhost", "http://127.0.0.1"),
                100,
                1,
                2,
                100,
                100,
                Map.of(),
                false
        );
    }

    private static Set<ClientCapability> defaultClientCapabilities() {
        return EnumSet.of(
                ClientCapability.SAMPLING,
                ClientCapability.ROOTS,
                ClientCapability.ELICITATION
        );
    }

    public static McpHostConfiguration withClientSpecs(Map<String, String> clientSpecs, boolean verbose) {
        return new McpHostConfiguration(
                "2025-06-18",
                "2025-03-26",
                30_000L,
                5_000L,
                "cli",
                "CLI",
                "0",
                defaultClientCapabilities(),
                "user",
                20,
                1_000L,
                "stdio",
                0,
                List.of("http://localhost", "http://127.0.0.1"),
                100,
                1,
                2,
                100,
                100,
                clientSpecs,
                verbose
        );
    }
}