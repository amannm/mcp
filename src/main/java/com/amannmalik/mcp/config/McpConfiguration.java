package com.amannmalik.mcp.config;

import java.util.List;

public record McpConfiguration(SystemConfig system,
                               PerformanceConfig performance,
                               ServerConfig server,
                               SecurityConfig security,
                               ClientConfig client,
                               HostConfig host) {

    private static final McpConfiguration CURRENT = new McpConfiguration(
            new SystemConfig(new ProtocolConfig("2025-06-18", "2025-03-26"),
                    new TimeoutsConfig(30_000, 5_000, 2)),
            new PerformanceConfig(
                    new RateLimitsConfig(5, 10, 20, 20),
                    new PaginationConfig(100, 100, 100, 1),
                    new RuntimeConfig(1_000, 1)),
            new ServerConfig(
                    new ServerInfoConfig("mcp-java", "MCP Java Reference", "0.1.0"),
                    new TransportConfig("stdio", 0, List.of("http://localhost", "http://127.0.0.1")),
                    new MessagingConfig(
                            new ErrorCodesConfig(-32_001),
                            new LoggerNamesConfig("server", "parser", "cancellation"),
                            new ErrorMessagesConfig(
                                    "Error processing message",
                                    "Server not initialized",
                                    "Parse error",
                                    "Invalid request",
                                    "Access denied",
                                    "Request timed out"))),
            new SecurityConfig(
                    new AuthConfig("MCP_JWT_SECRET", "default"),
                    new PrivacyConfig("default")),
            new ClientConfig(
                    new ClientInfoConfig("cli", "CLI", "0"),
                    List.of("SAMPLING", "ROOTS")),
            new HostConfig("user"));

    static {
        validate(CURRENT);
    }

    public static McpConfiguration current() {
        return CURRENT;
    }

    private static void validate(McpConfiguration c) {
        if (c.system().timeouts().defaultMs() <= 0 ||
                c.system().timeouts().pingMs() <= 0 ||
                c.system().timeouts().processWaitSeconds() <= 0)
            throw new IllegalArgumentException("timeouts");
        if (c.performance().rateLimits().toolsPerSecond() < 0 ||
                c.performance().rateLimits().completionsPerSecond() < 0 ||
                c.performance().rateLimits().logsPerSecond() < 0 ||
                c.performance().rateLimits().progressPerSecond() < 0)
            throw new IllegalArgumentException("rateLimits");
        if (c.performance().pagination().defaultPageSize() <= 0 ||
                c.performance().pagination().maxCompletionValues() <= 0 ||
                c.performance().pagination().responseQueueCapacity() <= 0)
            throw new IllegalArgumentException("pagination");
        if (c.performance().runtime().rateLimiterWindowMs() <= 0 ||
                c.performance().runtime().initialRequestId() < 0)
            throw new IllegalArgumentException("runtime");
        if (c.server().transport().port() < 0 || c.server().transport().port() > 65_535)
            throw new IllegalArgumentException("port");
        if (c.server().messaging().errorCodes().rateLimit() >= 0)
            throw new IllegalArgumentException("errorCodes.rateLimit must be negative");
    }

    public record SystemConfig(ProtocolConfig protocol, TimeoutsConfig timeouts) {
    }

    public record ProtocolConfig(String version, String compatibilityVersion) {
    }

    public record TimeoutsConfig(long defaultMs, long pingMs, int processWaitSeconds) {
    }

    public record PerformanceConfig(RateLimitsConfig rateLimits,
                                    PaginationConfig pagination,
                                    RuntimeConfig runtime) {
    }

    public record RateLimitsConfig(int toolsPerSecond,
                                   int completionsPerSecond,
                                   int logsPerSecond,
                                   int progressPerSecond) {
    }

    public record PaginationConfig(int defaultPageSize,
                                   int maxCompletionValues,
                                   int sseHistoryLimit,
                                   int responseQueueCapacity) {
    }

    public record RuntimeConfig(long rateLimiterWindowMs, long initialRequestId) {
    }

    public record ServerConfig(ServerInfoConfig info,
                               TransportConfig transport,
                               MessagingConfig messaging) {
    }

    public record ServerInfoConfig(String name, String description, String version) {
    }

    public record TransportConfig(String type, int port, List<String> allowedOrigins) {
        public TransportConfig {
            allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
        }
    }

    public record MessagingConfig(ErrorCodesConfig errorCodes,
                                  LoggerNamesConfig loggerNames,
                                  ErrorMessagesConfig errorMessages) {
    }

    public record ErrorCodesConfig(int rateLimit) {
    }

    public record LoggerNamesConfig(String server, String parser, String cancellation) {
    }

    public record ErrorMessagesConfig(String processing,
                                      String notInitialized,
                                      String parseError,
                                      String invalidRequest,
                                      String accessDenied,
                                      String timeout) {
    }

    public record SecurityConfig(AuthConfig auth, PrivacyConfig privacy) {
    }

    public record AuthConfig(String jwtSecretEnv, String defaultPrincipal) {
    }

    public record PrivacyConfig(String defaultBoundary) {
    }

    public record ClientConfig(ClientInfoConfig info, List<String> capabilities) {
        public ClientConfig {
            capabilities = capabilities == null ? List.of() : List.copyOf(capabilities);
        }
    }

    public record ClientInfoConfig(String name, String displayName, String version) {
    }

    public record HostConfig(String principal) {
    }
}
