package com.amannmalik.mcp.api;

import com.amannmalik.mcp.spi.SamplingAccessPolicy;
import com.amannmalik.mcp.spi.ToolAccessPolicy;
import com.amannmalik.mcp.transport.Protocol;
import java.util.*;

public record McpServerConfiguration(
        // Protocol configuration
        String version,
        String compatibilityVersion,
        long defaultTimeoutMs,
        long initialRequestId,
        List<String> supportedVersions,

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
        LoggingLevel initialLogLevel,

        // Access policy configuration
        ToolAccessPolicy toolAccessPolicy,
        SamplingAccessPolicy samplingAccessPolicy,

        // Default principal for server operations
        String defaultPrincipal,
        String defaultBoundary,

        // Transport configuration
        String transportType,
        int serverPort,
        List<String> allowedOrigins,
        String expectedAudience,
        String resourceMetadataUrl,
        List<String> authServers,
        boolean insecure,
        boolean verbose
) {

    public McpServerConfiguration {
        supportedVersions = List.copyOf(supportedVersions);
        allowedOrigins = List.copyOf(allowedOrigins);
        authServers = List.copyOf(authServers);
        if (supportedVersions.isEmpty())
            throw new IllegalArgumentException("Supported versions required");
        if (defaultTimeoutMs <= 0 || initialRequestId < 0)
            throw new IllegalArgumentException("Invalid protocol configuration");
        if (toolsPerSecond < 0 || completionsPerSecond < 0 || logsPerSecond < 0 || progressPerSecond < 0)
            throw new IllegalArgumentException("Invalid rate limit configuration");
        if (rateLimiterWindowMs <= 0)
            throw new IllegalArgumentException("Invalid rate limiter window");
        if (rateLimitErrorCode >= 0)
            throw new IllegalArgumentException("Rate limit error code must be negative");
        if (initialLogLevel == null || toolAccessPolicy == null || samplingAccessPolicy == null)
            throw new IllegalArgumentException("Invalid policy configuration");
        if (serverPort < 0 || serverPort > 65_535)
            throw new IllegalArgumentException("Invalid port number");
    }

    public static McpServerConfiguration defaultConfiguration() {
        return new McpServerConfiguration(
                "2025-06-18",
                "2025-03-26",
                30_000L,
                1L,
                List.of(Protocol.LATEST_VERSION, Protocol.PREVIOUS_VERSION),
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
                LoggingLevel.INFO,
                ToolAccessPolicy.PERMISSIVE,
                SamplingAccessPolicy.PERMISSIVE,
                "default",
                "default",
                "stdio",
                3000,
                List.of("http://localhost", "http://127.0.0.1"),
                null,
                null,
                List.of(),
                false,
                false
        );
    }

    public McpServerConfiguration withTransport(String transportType,
                                                int serverPort,
                                                List<String> allowedOrigins,
                                                String expectedAudience,
                                                String resourceMetadataUrl,
                                                List<String> authServers,
                                                boolean insecure,
                                                boolean verbose) {
        return new McpServerConfiguration(
                version,
                compatibilityVersion,
                defaultTimeoutMs,
                initialRequestId,
                supportedVersions,
                toolsPerSecond,
                completionsPerSecond,
                logsPerSecond,
                progressPerSecond,
                rateLimiterWindowMs,
                rateLimitErrorCode,
                serverName,
                serverDescription,
                serverVersion,
                errorProcessing,
                errorNotInitialized,
                errorParse,
                errorInvalidRequest,
                errorAccessDenied,
                errorTimeout,
                serverLoggerName,
                parserLoggerName,
                cancellationLoggerName,
                initialLogLevel,
                toolAccessPolicy,
                samplingAccessPolicy,
                defaultPrincipal,
                defaultBoundary,
                transportType,
                serverPort,
                allowedOrigins,
                expectedAudience,
                resourceMetadataUrl,
                authServers,
                insecure,
                verbose
        );
    }
}