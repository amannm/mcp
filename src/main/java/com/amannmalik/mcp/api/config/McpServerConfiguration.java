package com.amannmalik.mcp.api.config;

import com.amannmalik.mcp.api.Protocol;
import com.amannmalik.mcp.util.ValidationUtil;

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
        String defaultPrincipal,
        String defaultBoundary,
        String transportType,
        int serverPort,
        List<String> allowedOrigins,
        String expectedAudience,
        String jwtSecret,
        String resourceMetadataUrl,
        List<String> authServers,
        boolean insecure,
        boolean verbose,
        int httpsPort,
        TlsConfiguration tlsConfiguration,
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
        long sseHistoryLimit,
        int httpResponseQueueCapacity,
        List<String> servletAcceptedContentTypes,
        List<String> servletProducedContentTypes,
        boolean servletEnableAsyncProcessing
) {


    public McpServerConfiguration {
        supportedVersions = List.copyOf(supportedVersions);
        allowedOrigins = List.copyOf(allowedOrigins);
        authServers = List.copyOf(authServers);
        servletPaths = Set.copyOf(servletPaths);
        servletAcceptedContentTypes = List.copyOf(servletAcceptedContentTypes);
        servletProducedContentTypes = List.copyOf(servletProducedContentTypes);
        if (supportedVersions.isEmpty()) {
            throw new IllegalArgumentException("Supported versions required");
        }
        if (defaultTimeoutMs.isNegative() || initialRequestId < 0) {
            throw new IllegalArgumentException("Invalid protocol configuration");
        }
        if (toolsPerSecond < 0 || completionsPerSecond < 0 || logsPerSecond < 0 || progressPerSecond < 0) {
            throw new IllegalArgumentException("Invalid rate limit configuration");
        }
        if (rateLimiterWindowMs <= 0) {
            throw new IllegalArgumentException("Invalid rate limiter window");
        }
        if (rateLimitErrorCode >= 0) {
            throw new IllegalArgumentException("Rate limit error code must be negative");
        }
        if (initialLogLevel == null) {
            throw new IllegalArgumentException("Invalid policy configuration");
        }
        if (serverPort < 0 || serverPort > 65_535) {
            throw new IllegalArgumentException("Invalid port number");
        }
        if (httpsPort < 0 || httpsPort > 65_535) {
            throw new IllegalArgumentException("Invalid HTTPS port number");
        }
        if (tlsConfiguration == null) {
            throw new IllegalArgumentException("TLS configuration required");
        }
        if (httpsPort > 0) {
            if (!tlsConfiguration.hasKeystore()) {
                throw new IllegalArgumentException("Keystore configuration required");
            }
            if (requireClientAuth && !tlsConfiguration.hasTruststore()) {
                throw new IllegalArgumentException("Truststore configuration required");
            }
            if (resourceMetadataUrl != null && !resourceMetadataUrl.isBlank() && resourceMetadataUrl.startsWith("http://")) {
                throw new IllegalArgumentException("HTTPS required for resource metadata URL");
            }
            if (authServers.stream().anyMatch(u -> u.startsWith("http://"))) {
                throw new IllegalArgumentException("HTTPS required for authorization server URLs");
            }
        }
        if (expectedAudience == null) {
            throw new IllegalArgumentException("expectedAudience cannot be null (use blank to disable)");
        }
        if (jwtSecret == null) {
            throw new IllegalArgumentException("jwtSecret cannot be null (use blank for none)");
        }
        if (httpsMode == null) {
            throw new IllegalArgumentException("HTTPS mode required");
        }
        if (httpsMode != HttpsMode.MIXED && httpsPort <= 0) {
            throw new IllegalArgumentException("HTTPS mode requires HTTPS port");
        }
        if (bindAddress == null || bindAddress.isBlank()) {
            throw new IllegalArgumentException("Bind address required");
        }
        if (sessionIdByteLength <= 0) {
            throw new IllegalArgumentException("Session ID byte length must be positive");
        }
        ValidationUtil.requirePositive(initializeRequestTimeout, "Initialize request timeout");
        if (sseClientPrefixByteLength <= 0) {
            throw new IllegalArgumentException("Client prefix byte length must be positive");
        }
        if (sseHistoryLimit < 0) {
            throw new IllegalArgumentException("SSE history limit must be non-negative");
        }
        if (httpResponseQueueCapacity <= 0) {
            throw new IllegalArgumentException("HTTP response queue capacity must be positive");
        }
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
                "default",
                "default",
                "stdio",
                3000,
                List.of("http://localhost", "http://127.0.0.1", "https://localhost", "https://127.0.0.1"),
                "https://mcp.example.com",
                "",
                "https://mcp.example.com/.well-known/oauth-protected-resource",
                List.of("https://auth.example.com"),
                false,
                false,
                3443,
                new TlsConfiguration(
                        "server.p12",
                        "changeit",
                        "PKCS12",
                        "",
                        "",
                        "PKCS12",
                        List.of("TLSv1.3", "TLSv1.2"),
                        List.of(
                                "TLS_AES_128_GCM_SHA256",
                                "TLS_AES_256_GCM_SHA384",
                                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384")),
                false,
                HttpsMode.REDIRECT,
                "127.0.0.1",
                Set.of("/", "/.well-known/oauth-protected-resource"),
                "/.well-known/oauth-protected-resource",
                "%s://%s:%d/.well-known/oauth-protected-resource",
                32,
                Duration.ofSeconds(30),
                true,
                8,
                true,
                Duration.ofMinutes(5),
                1L,
                10,
                List.of("application/json", "text/event-stream"),
                List.of("application/json", "text/event-stream"),
                true
        );
    }

    public McpServerConfiguration withTransport(String transportType,
                                                int serverPort,
                                                List<String> allowedOrigins,
                                                String expectedAudience,
                                                String jwtSecret,
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
                defaultPrincipal,
                defaultBoundary,
                transportType,
                serverPort,
                allowedOrigins,
                expectedAudience,
                jwtSecret,
                resourceMetadataUrl,
                authServers,
                insecure,
                verbose,
                httpsPort,
                tlsConfiguration,
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
                sseHistoryLimit,
                httpResponseQueueCapacity,
                servletAcceptedContentTypes,
                servletProducedContentTypes,
                servletEnableAsyncProcessing
        );
    }

    public McpServerConfiguration withTls(int httpsPort,
                                          TlsConfiguration tlsConfiguration,
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
                defaultPrincipal,
                defaultBoundary,
                transportType,
                serverPort,
                allowedOrigins,
                expectedAudience,
                jwtSecret,
                resourceMetadataUrl,
                authServers,
                insecure,
                verbose,
                httpsPort,
                tlsConfiguration,
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
                sseHistoryLimit,
                httpResponseQueueCapacity,
                servletAcceptedContentTypes,
                servletProducedContentTypes,
                servletEnableAsyncProcessing
        );
    }

    public String keystorePath() {
        return tlsConfiguration.keystorePath();
    }

    public String keystorePassword() {
        return tlsConfiguration.keystorePassword();
    }

    public String keystoreType() {
        return tlsConfiguration.keystoreType();
    }

    public String truststorePath() {
        return tlsConfiguration.truststorePath();
    }

    public String truststorePassword() {
        return tlsConfiguration.truststorePassword();
    }

    public String truststoreType() {
        return tlsConfiguration.truststoreType();
    }

    public List<String> tlsProtocols() {
        return tlsConfiguration.tlsProtocols();
    }

    public List<String> cipherSuites() {
        return tlsConfiguration.cipherSuites();
    }
}
