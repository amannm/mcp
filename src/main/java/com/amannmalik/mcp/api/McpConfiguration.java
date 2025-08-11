package com.amannmalik.mcp.api;

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
            "default", "default",
            "cli", "CLI", "0", List.of("SAMPLING", "ROOTS", "ELICITATION"), "user");

    public McpConfiguration {
        allowedOrigins = List.copyOf(allowedOrigins);
        clientCapabilities = List.copyOf(clientCapabilities);
        if (defaultMs <= 0 || pingMs <= 0 || processWaitSeconds <= 0)
            throw new IllegalArgumentException("system");
        if (toolsPerSecond < 0 || completionsPerSecond < 0 || logsPerSecond < 0 || progressPerSecond < 0)
            throw new IllegalArgumentException("rateLimits");
        if (defaultPageSize <= 0 || maxCompletionValues <= 0 || responseQueueCapacity <= 0)
            throw new IllegalArgumentException("pagination");
        if (rateLimiterWindowMs <= 0 || initialRequestId < 0)
            throw new IllegalArgumentException("runtime");
        if (port < 0 || port > 65_535)
            throw new IllegalArgumentException("port");
        if (rateLimit >= 0)
            throw new IllegalArgumentException("errorCodes.rateLimit must be negative");
    }


    public static McpConfiguration current() {
        return CURRENT;
    }
}

