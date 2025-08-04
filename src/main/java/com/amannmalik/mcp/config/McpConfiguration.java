package com.amannmalik.mcp.config;

import java.util.List;

public record McpConfiguration(String version,
                               String compatibilityVersion,
                               long defaultMs,
                               long pingMs,
                               int processWaitSeconds,
                               int toolsPerSecond,
                               int completionsPerSecond,
                               int logsPerSecond,
                               int progressPerSecond,
                               int defaultPageSize,
                               int maxCompletionValues,
                               int sseHistoryLimit,
                               int responseQueueCapacity,
                               long rateLimiterWindowMs,
                               long initialRequestId,
                               String serverName,
                               String serverDescription,
                               String serverVersion,
                               String transportType,
                               int port,
                               List<String> allowedOrigins,
                               int rateLimit,
                               String loggerServer,
                               String loggerParser,
                               String loggerCancellation,
                               String errorProcessing,
                               String errorNotInitialized,
                               String errorParse,
                               String errorInvalidRequest,
                               String errorAccessDenied,
                               String errorTimeout,
                               String jwtSecretEnv,
                               String defaultPrincipal,
                               String defaultBoundary,
                               String clientName,
                               String clientDisplayName,
                               String clientVersion,
                               List<String> clientCapabilities,
                               String hostPrincipal) {

    private static final McpConfiguration CURRENT = new McpConfiguration(
            "2025-06-18", "2025-03-26", 30_000, 5_000, 2,
            5, 10, 20, 20, 100, 100, 100, 1, 1_000, 1,
            "mcp-java", "MCP Java Reference", "0.1.0", "stdio", 0,
            List.of("http://localhost", "http://127.0.0.1"), -32_001,
            "server", "parser", "cancellation",
            "Error processing message",
            "Server not initialized",
            "Parse error",
            "Invalid request",
            "Access denied",
            "Request timed out",
            "MCP_JWT_SECRET", "default", "default",
            "cli", "CLI", "0", List.of("SAMPLING", "ROOTS"), "user");

    public McpConfiguration {
        allowedOrigins = List.copyOf(allowedOrigins);
        clientCapabilities = List.copyOf(clientCapabilities);
    }

    static { validate(CURRENT); }

    public static McpConfiguration current() { return CURRENT; }

    private static void validate(McpConfiguration c) {
        if (c.defaultMs <= 0 || c.pingMs <= 0 || c.processWaitSeconds <= 0)
            throw new IllegalArgumentException("system");
        if (c.toolsPerSecond < 0 || c.completionsPerSecond < 0 || c.logsPerSecond < 0 || c.progressPerSecond < 0)
            throw new IllegalArgumentException("rateLimits");
        if (c.defaultPageSize <= 0 || c.maxCompletionValues <= 0 || c.responseQueueCapacity <= 0)
            throw new IllegalArgumentException("pagination");
        if (c.rateLimiterWindowMs <= 0 || c.initialRequestId < 0)
            throw new IllegalArgumentException("runtime");
        if (c.port < 0 || c.port > 65_535)
            throw new IllegalArgumentException("port");
        if (c.rateLimit >= 0)
            throw new IllegalArgumentException("errorCodes.rateLimit must be negative");
    }
}

