package com.amannmalik.mcp.api;

import java.util.List;

public record McpServerConfiguration(
        // Protocol configuration
        String version,
        String compatibilityVersion,
        long defaultTimeoutMs,
        long initialRequestId,
        
        // Rate limiting configuration
        RateLimitConfig rateLimiting,
        
        // Server identity configuration
        ServerIdentity serverIdentity,
        
        // Error handling configuration
        ErrorMessages errorMessages,
        
        // Logging configuration
        LoggingConfig loggingConfig,
        
        // Default principal for server operations
        String defaultPrincipal,
        String defaultBoundary
) {

    public static McpServerConfiguration defaultConfiguration() {
        return new McpServerConfiguration(
                "2025-06-18",
                "2025-03-26", 
                30_000L,
                1L,
                RateLimitConfig.defaultConfig(),
                ServerIdentity.defaultIdentity(),
                ErrorMessages.defaultMessages(),
                LoggingConfig.defaultConfig(),
                "default",
                "default"
        );
    }

    public record RateLimitConfig(
            int toolsPerSecond,
            int completionsPerSecond, 
            int logsPerSecond,
            int progressPerSecond,
            long windowMs,
            int rateLimitErrorCode
    ) {
        public static RateLimitConfig defaultConfig() {
            return new RateLimitConfig(5, 10, 20, 20, 1_000L, -32_001);
        }
    }

    public record ServerIdentity(
            String name,
            String description,
            String version
    ) {
        public static ServerIdentity defaultIdentity() {
            return new ServerIdentity("mcp-java", "MCP Java Reference", "0.1.0");
        }
    }

    public record ErrorMessages(
            String errorProcessing,
            String errorNotInitialized,
            String errorParse,
            String errorInvalidRequest,
            String errorAccessDenied,
            String errorTimeout
    ) {
        public static ErrorMessages defaultMessages() {
            return new ErrorMessages(
                    "Error processing message",
                    "Server not initialized",
                    "Parse error", 
                    "Invalid request",
                    "Access denied",
                    "Request timed out"
            );
        }
    }

    public record LoggingConfig(
            String serverLoggerName,
            String parserLoggerName,
            String cancellationLoggerName
    ) {
        public static LoggingConfig defaultConfig() {
            return new LoggingConfig("server", "parser", "cancellation");
        }
    }
}