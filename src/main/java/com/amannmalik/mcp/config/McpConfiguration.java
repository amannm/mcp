package com.amannmalik.mcp.config;

import java.util.List;

public record McpConfiguration(SystemConfig system,
                               PerformanceConfig performance,
                               ServerConfig server,
                               SecurityConfig security,
                               ClientConfig client,
                               HostConfig host) {

    private static final McpConfiguration CURRENT = new McpConfiguration(
            new SystemConfig("2025-06-18", "2025-03-26", 30_000, 5_000, 2),
            new PerformanceConfig(5, 10, 20, 20, 100, 100, 100, 1, 1_000, 1),
            new ServerConfig("mcp-java", "MCP Java Reference", "0.1.0",
                    "stdio", 0, List.of("http://localhost", "http://127.0.0.1"),
                    -32_001,
                    "server", "parser", "cancellation",
                    "Error processing message",
                    "Server not initialized",
                    "Parse error",
                    "Invalid request",
                    "Access denied",
                    "Request timed out"),
            new SecurityConfig("MCP_JWT_SECRET", "default", "default"),
            new ClientConfig("cli", "CLI", "0", List.of("SAMPLING", "ROOTS")),
            new HostConfig("user"));

    static {
        validate(CURRENT);
    }

    public static McpConfiguration current() {
        return CURRENT;
    }

    private static void validate(McpConfiguration c) {
        if (c.system().defaultMs() <= 0 ||
                c.system().pingMs() <= 0 ||
                c.system().processWaitSeconds() <= 0)
            throw new IllegalArgumentException("system");
        if (c.performance().toolsPerSecond() < 0 ||
                c.performance().completionsPerSecond() < 0 ||
                c.performance().logsPerSecond() < 0 ||
                c.performance().progressPerSecond() < 0)
            throw new IllegalArgumentException("rateLimits");
        if (c.performance().defaultPageSize() <= 0 ||
                c.performance().maxCompletionValues() <= 0 ||
                c.performance().responseQueueCapacity() <= 0)
            throw new IllegalArgumentException("pagination");
        if (c.performance().rateLimiterWindowMs() <= 0 ||
                c.performance().initialRequestId() < 0)
            throw new IllegalArgumentException("runtime");
        if (c.server().port() < 0 || c.server().port() > 65_535)
            throw new IllegalArgumentException("port");
        if (c.server().rateLimit() >= 0)
            throw new IllegalArgumentException("errorCodes.rateLimit must be negative");
    }

    public record SystemConfig(String version,
                               String compatibilityVersion,
                               long defaultMs,
                               long pingMs,
                               int processWaitSeconds) {
    }

    public record PerformanceConfig(int toolsPerSecond,
                                    int completionsPerSecond,
                                    int logsPerSecond,
                                    int progressPerSecond,
                                    int defaultPageSize,
                                    int maxCompletionValues,
                                    int sseHistoryLimit,
                                    int responseQueueCapacity,
                                    long rateLimiterWindowMs,
                                    long initialRequestId) {
    }

    public record ServerConfig(String name,
                               String description,
                               String version,
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
                               String errorTimeout) {
        public ServerConfig {
            allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
        }
    }

    public record SecurityConfig(String jwtSecretEnv,
                                 String defaultPrincipal,
                                 String defaultBoundary) {
    }

    public record ClientConfig(String name,
                               String displayName,
                               String version,
                               List<String> capabilities) {
        public ClientConfig {
            capabilities = capabilities == null ? List.of() : List.copyOf(capabilities);
        }
    }

    public record HostConfig(String principal) {
    }
}
