package com.amannmalik.mcp.api;

import com.amannmalik.mcp.spi.SamplingAccessPolicy;
import com.amannmalik.mcp.spi.ToolAccessPolicy;

import java.time.Duration;
import java.util.List;
import java.util.Set;

public record McpServerConfiguration(
        String version,
        String compatibilityVersion,
        Duration defaultTimeoutMs,
        long initialRequestId,
        List<String> supportedVersions,
        int toolsPerSecond,
        int completionsPerSecond,
        int logsPerSecond,
        int progressPerSecond,
        long rateLimiterWindowMs,
        int rateLimitErrorCode,
        String serverName,
        String serverDescription,
        String serverVersion,
        String errorProcessing,
        String errorNotInitialized,
        String errorParse,
        String errorInvalidRequest,
        String errorAccessDenied,
        String errorTimeout,
        String serverLoggerName,
        String parserLoggerName,
        String cancellationLoggerName,
        LoggingLevel initialLogLevel,
        ToolAccessPolicy toolAccessPolicy,
        SamplingAccessPolicy samplingAccessPolicy,
        String defaultPrincipal,
        String defaultBoundary,
        String transportType,
        int serverPort,
        List<String> allowedOrigins,
        String expectedAudience,
        String resourceMetadataUrl,
        List<String> authServers,
        boolean insecure,
        boolean verbose,
        int httpsPort,
        String keystorePath,
        String keystorePassword,
        String keystoreType,
        String truststorePath,
        String truststorePassword,
        String truststoreType,
        List<String> tlsProtocols,
        List<String> cipherSuites,
        boolean requireClientAuth,
        HttpsMode httpsMode,
        String bindAddress,
        Set<String> servletPaths,
        String resourceMetadataPath,
        String resourceMetadataUrlTemplate,
        int sessionIdByteLength,
        Duration initializeRequestTimeout,
        boolean strictVersionValidation,
        int sseClientPrefixByteLength,
        boolean sseEnableHistoryReplay,
        Duration sseClientTimeout,
        List<String> servletAcceptedContentTypes,
        List<String> servletProducedContentTypes,
        boolean servletEnableAsyncProcessing
) {

    public McpServerConfiguration {
        supportedVersions = List.copyOf(supportedVersions);
        allowedOrigins = List.copyOf(allowedOrigins);
        authServers = List.copyOf(authServers);
        tlsProtocols = List.copyOf(tlsProtocols);
        cipherSuites = List.copyOf(cipherSuites);
        servletPaths = Set.copyOf(servletPaths);
        servletAcceptedContentTypes = List.copyOf(servletAcceptedContentTypes);
        servletProducedContentTypes = List.copyOf(servletProducedContentTypes);
        if (supportedVersions.isEmpty())
            throw new IllegalArgumentException("Supported versions required");
        if (defaultTimeoutMs.isNegative() || initialRequestId < 0)
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
        if (httpsPort < 0 || httpsPort > 65_535)
            throw new IllegalArgumentException("Invalid HTTPS port number");
        if (tlsProtocols.isEmpty() || tlsProtocols.stream().anyMatch(String::isBlank))
            throw new IllegalArgumentException("Invalid TLS protocols");
        if (cipherSuites.isEmpty() || cipherSuites.stream().anyMatch(String::isBlank))
            throw new IllegalArgumentException("Invalid cipher suites");
        if (httpsPort > 0) {
            if (keystorePath.isBlank() || keystorePassword.isBlank() || keystoreType.isBlank())
                throw new IllegalArgumentException("Keystore configuration required");
            if (requireClientAuth && (truststorePath.isBlank() || truststorePassword.isBlank() || truststoreType.isBlank()))
                throw new IllegalArgumentException("Truststore configuration required");
        }
        if (httpsMode == null)
            throw new IllegalArgumentException("HTTPS mode required");
        if (httpsMode != HttpsMode.MIXED && httpsPort <= 0)
            throw new IllegalArgumentException("HTTPS mode requires HTTPS port");
        if (bindAddress == null || bindAddress.isBlank())
            throw new IllegalArgumentException("Bind address required");
        if (sessionIdByteLength <= 0)
            throw new IllegalArgumentException("Session ID byte length must be positive");
        if (initializeRequestTimeout.isNegative() || initializeRequestTimeout.isZero())
            throw new IllegalArgumentException("Initialize request timeout must be positive");
        if (sseClientPrefixByteLength <= 0)
            throw new IllegalArgumentException("Client prefix byte length must be positive");
    }

    public static McpServerConfiguration defaultConfiguration() {
        return new McpServerConfiguration(
                Protocol.LATEST_VERSION,
                Protocol.PREVIOUS_VERSION,
                Duration.ofSeconds(30),
                1L,
                Protocol.SUPPORTED_VERSIONS,
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
                List.of(
                        "http://localhost",
                        "http://127.0.0.1",
                        "https://localhost",
                        "https://127.0.0.1"),
                "https://mcp.example.com",
                "https://mcp.example.com/.well-known/oauth-protected-resource",
                List.of("https://auth.example.com"),
                false,
                false,
                3443,
                "server.p12",
                "changeit",
                "PKCS12",
                "",
                "",
                "PKCS12",
                List.of("TLSv1.3", "TLSv1.2"),
                List.of("TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384"),
                false,
                HttpsMode.REDIRECT,
                "127.0.0.1",
                Set.of("/", "/.well-known/oauth-protected-resource"),
                "/.well-known/oauth-protected-resource",
                "http://%s:%d/.well-known/oauth-protected-resource",
                32,
                Duration.ofSeconds(30),
                true,
                8,
                true,
                Duration.ofMinutes(5),
                List.of("application/json", "text/event-stream"),
                List.of("application/json", "text/event-stream"),
                true
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
                verbose,
                httpsPort,
                keystorePath,
                keystorePassword,
                keystoreType,
                truststorePath,
                truststorePassword,
                truststoreType,
                tlsProtocols,
                cipherSuites,
                requireClientAuth,
                httpsMode,
                bindAddress,
                servletPaths,
                resourceMetadataPath,
                resourceMetadataUrlTemplate,
                sessionIdByteLength,
                initializeRequestTimeout,
                strictVersionValidation,
                sseClientPrefixByteLength,
                sseEnableHistoryReplay,
                sseClientTimeout,
                servletAcceptedContentTypes,
                servletProducedContentTypes,
                servletEnableAsyncProcessing
        );
    }

    public McpServerConfiguration withTls(int httpsPort,
                                         String keystorePath,
                                         String keystorePassword,
                                         String keystoreType,
                                         String truststorePath,
                                         String truststorePassword,
                                         String truststoreType,
                                         List<String> tlsProtocols,
                                         List<String> cipherSuites,
                                         boolean requireClientAuth) {
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
                verbose,
                httpsPort,
                keystorePath,
                keystorePassword,
                keystoreType,
                truststorePath,
                truststorePassword,
                truststoreType,
                tlsProtocols,
                cipherSuites,
                requireClientAuth,
                httpsMode,
                bindAddress,
                servletPaths,
                resourceMetadataPath,
                resourceMetadataUrlTemplate,
                sessionIdByteLength,
                initializeRequestTimeout,
                strictVersionValidation,
                sseClientPrefixByteLength,
                sseEnableHistoryReplay,
                sseClientTimeout,
                servletAcceptedContentTypes,
                servletProducedContentTypes,
                servletEnableAsyncProcessing
        );
    }
}