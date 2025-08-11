package com.amannmalik.mcp.api;

public record McpServerConfiguration(
        // Protocol configuration
        String version,
        String compatibilityVersion,
        long defaultTimeoutMs,
        long initialRequestId,

        // Rate limiting configuration
        int toolsPerSecond,
        int completionsPerSecond,
        int logsPerSecond,
        int progressPerSecond,
        long rateLimiterWindowMs,
        int rateLimitErrorCode,

        // Server identity configuration
        String serverName,
        String serverDescription,
        String serverVersion,

        // Error handling configuration
        String errorProcessing,
        String errorNotInitialized,
        String errorParse,
        String errorInvalidRequest,
        String errorAccessDenied,
        String errorTimeout,

        // Logging configuration
        String serverLoggerName,
        String parserLoggerName,
        String cancellationLoggerName,

        // Default principal for server operations
        String defaultPrincipal,
        String defaultBoundary
) {

    public McpServerConfiguration {
        if (defaultTimeoutMs <= 0 || initialRequestId < 0)
            throw new IllegalArgumentException("Invalid protocol configuration");
        if (toolsPerSecond < 0 || completionsPerSecond < 0 || logsPerSecond < 0 || progressPerSecond < 0)
            throw new IllegalArgumentException("Invalid rate limit configuration");
        if (rateLimiterWindowMs <= 0)
            throw new IllegalArgumentException("Invalid rate limiter window");
        if (rateLimitErrorCode >= 0)
            throw new IllegalArgumentException("Rate limit error code must be negative");
    }

    public static McpServerConfiguration defaultConfiguration() {
        return new McpServerConfiguration(
                "2025-06-18",
                "2025-03-26",
                30_000L,
                1L,
                5,
                10,
                20,
                20,
                1_000L,
                -32_001,
                "mcp-java",
                "MCP Java Reference",
                "0.1.0",
                "Error processing message",
                "Server not initialized",
                "Parse error",
                "Invalid request",
                "Access denied",
                "Request timed out",
                "server",
                "parser",
                "cancellation",
                "default",
                "default"
        );
    }
}