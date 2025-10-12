package com.amannmalik.mcp.test;

import com.amannmalik.mcp.api.*;
import com.amannmalik.mcp.spi.Principal;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Set;

public final class ServerHarness implements Closeable {
    private final McpServer server;
    private final int port;

    private ServerHarness(McpServer server, int port) {
        this.server = server;
        this.port = port;
    }

    public static ServerHarness start() throws Exception {
        int port = freePort();
        var base = McpServerConfiguration.defaultConfiguration();
        var config = new McpServerConfiguration(
                base.version(),
                base.compatibilityVersion(),
                Duration.ofSeconds(5),
                1L,
                base.supportedVersions(),
                5, 10, 20, 20,
                1_000L,
                -32001,
                "mcp-it",
                "MCP Test Server",
                base.serverVersion(),
                base.errorProcessing(),
                base.errorNotInitialized(),
                base.errorParse(),
                base.errorInvalidRequest(),
                base.errorAccessDenied(),
                base.errorTimeout(),
                base.serverLoggerName(),
                base.parserLoggerName(),
                base.cancellationLoggerName(),
                LoggingLevel.INFO,
                base.defaultPrincipal(),
                base.defaultBoundary(),
                "http",
                port,
                base.allowedOrigins(),
                "", // expectedAudience disabled
                "",
                "",
                List.of(),
                true, // insecure test
                false,
                0,
                base.tlsConfiguration(),
                false,
                HttpsMode.MIXED,
                "127.0.0.1",
                base.servletPaths(),
                base.resourceMetadataPath(),
                base.resourceMetadataUrlTemplate(),
                base.sessionIdByteLength(),
                base.initializeRequestTimeout(),
                true,
                base.sseClientPrefixByteLength(),
                base.sseEnableHistoryReplay(),
                base.sseClientTimeout(),
                base.sseHistoryLimit(),
                base.httpResponseQueueCapacity(),
                base.servletAcceptedContentTypes(),
                base.servletProducedContentTypes(),
                base.servletEnableAsyncProcessing()
        );
        var principal = new Principal(base.defaultPrincipal(), Set.of());
        var server = McpServer.create(
                config,
                principal,
                null);
        Thread.ofVirtual().start(() -> {
            try {
                server.serve();
            } catch (IOException e) {
                // ignored: server shutdown
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
        return new ServerHarness(server, port);
    }

    private static int freePort() throws IOException {
        try (var s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }

    public URI endpoint() {
        return URI.create("http://127.0.0.1:" + port + "/");
    }

    public int port() {
        return port;
    }

    @Override
    public void close() throws IOException {
        server.close();
    }
}
