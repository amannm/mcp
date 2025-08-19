package com.amannmalik.mcp;

import com.amannmalik.mcp.api.McpServerConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class McpServerConfigurationSecurityTest {

    private static McpServerConfiguration copy(McpServerConfiguration cfg,
                                               List<String> protocols,
                                               List<String> suites) {
        return new McpServerConfiguration(
                cfg.version(),
                cfg.compatibilityVersion(),
                cfg.defaultTimeoutMs(),
                cfg.initialRequestId(),
                cfg.supportedVersions(),
                cfg.toolsPerSecond(),
                cfg.completionsPerSecond(),
                cfg.logsPerSecond(),
                cfg.progressPerSecond(),
                cfg.rateLimiterWindowMs(),
                cfg.rateLimitErrorCode(),
                cfg.serverName(),
                cfg.serverDescription(),
                cfg.serverVersion(),
                cfg.errorProcessing(),
                cfg.errorNotInitialized(),
                cfg.errorParse(),
                cfg.errorInvalidRequest(),
                cfg.errorAccessDenied(),
                cfg.errorTimeout(),
                cfg.serverLoggerName(),
                cfg.parserLoggerName(),
                cfg.cancellationLoggerName(),
                cfg.initialLogLevel(),
                cfg.toolAccessPolicy(),
                cfg.samplingAccessPolicy(),
                cfg.defaultPrincipal(),
                cfg.defaultBoundary(),
                cfg.transportType(),
                cfg.serverPort(),
                cfg.allowedOrigins(),
                cfg.expectedAudience(),
                cfg.resourceMetadataUrl(),
                cfg.authServers(),
                cfg.insecure(),
                cfg.verbose(),
                cfg.httpsPort(),
                cfg.keystorePath(),
                cfg.keystorePassword(),
                cfg.keystoreType(),
                cfg.truststorePath(),
                cfg.truststorePassword(),
                cfg.truststoreType(),
                protocols,
                suites,
                cfg.requireClientAuth(),
                cfg.bindAddress(),
                cfg.servletPaths(),
                cfg.resourceMetadataPath(),
                cfg.resourceMetadataUrlTemplate(),
                cfg.sessionIdByteLength(),
                cfg.initializeRequestTimeout(),
                cfg.strictVersionValidation(),
                cfg.sseClientPrefixByteLength(),
                cfg.sseEnableHistoryReplay(),
                cfg.sseClientTimeout(),
                cfg.servletAcceptedContentTypes(),
                cfg.servletProducedContentTypes(),
                cfg.servletEnableAsyncProcessing()
        );
    }

    @Test
    void rejectsWeakProtocols() {
        var cfg = McpServerConfiguration.defaultConfiguration();
        assertThrows(IllegalArgumentException.class,
                () -> copy(cfg, List.of("TLSv1"), cfg.cipherSuites()));
    }

    @Test
    void rejectsNonPfsCipherSuite() {
        var cfg = McpServerConfiguration.defaultConfiguration();
        assertThrows(IllegalArgumentException.class,
                () -> copy(cfg, cfg.tlsProtocols(), List.of("TLS_RSA_WITH_AES_128_CBC_SHA")));
    }
}
