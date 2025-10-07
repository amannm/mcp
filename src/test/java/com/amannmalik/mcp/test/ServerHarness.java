package com.amannmalik.mcp.test;

import com.amannmalik.mcp.api.*;
import com.amannmalik.mcp.spi.*;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;

/**
 * In-process HTTP server using minimal providers, MIXED HTTPS mode.
 */
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

        var resources = new ResourceProvider() {
            @Override
            public ResourceBlock read(URI uri) {
                return null;
            }

            @Override
            public Pagination.Page<Resource> list(Cursor cursor) {
                return new Pagination.Page<>(List.of(), Cursor.End.INSTANCE);
            }

            @Override
            public Pagination.Page<ResourceTemplate> listTemplates(Cursor cursor) {
                return new Pagination.Page<>(List.of(), Cursor.End.INSTANCE);
            }

            @Override
            public AutoCloseable subscribe(URI uri, Consumer<ResourceUpdate> listener) {
                return () -> {
                };
            }

        };

        var tools = new ToolProvider() {
            final Tool echo = new Tool("echo", "Echo", "Echo tool", Json.createObjectBuilder().add("type", "object").build(), null, null, null);

            @Override
            public Pagination.Page<Tool> list(Cursor cursor) {
                return new Pagination.Page<>(List.of(echo), Cursor.End.INSTANCE);
            }

            @Override
            public boolean supportsListChanged() {
                return true;
            }

            @Override
            public Optional<Tool> find(String name) {
                return Optional.of(echo);
            }

            @Override
            public ToolResult call(String name, JsonObject arguments) {
                return new ToolResult(Json.createArrayBuilder().build(), null, false, null);
            }
        };

        var prompts = new PromptProvider() {
            @Override
            public Pagination.Page<Prompt> list(Cursor cursor) {
                return new Pagination.Page<>(List.of(), Cursor.End.INSTANCE);
            }

            @Override
            public boolean supportsListChanged() {
                return true;
            }

            @Override
            public Optional<Prompt> find(String name) {
                return Optional.empty();
            }

            @Override
            public PromptInstance get(String name, Map<String, String> arguments) {
                return new PromptInstance("desc", List.of());
            }
        };

        var completions = new CompletionProvider() {
            @Override
            public Pagination.Page<Ref> list(Cursor cursor) {
                return new Pagination.Page<>(List.of(), Cursor.End.INSTANCE);
            }

            @Override
            public boolean supportsListChanged() {
                return true;
            }

            @Override
            public CompleteResult execute(String name, JsonObject args) {
                return new CompleteResult(new Completion(List.of(), 0, false), null);
            }
        };

        var sampling = new SamplingProvider() {

            @Override
            public CreateMessageResponse createMessage(CreateMessageRequest request, Duration timeoutMillis) {
                return new CreateMessageResponse(Role.ASSISTANT, new ContentBlock.Text("ok", null, null), "model", "stop", null);
            }
        };

        var principal = new Principal(base.defaultPrincipal(), Set.of());

        var resourceAccess = loadSingleton(ResourceAccessPolicy.class);
        var toolAccess = loadSingleton(ToolAccessPolicy.class);
        var samplingAccessPolicy = loadSingleton(SamplingAccessPolicy.class);

        var server = new McpServer(
                config,
                resources,
                tools,
                prompts,
                completions,
                sampling,
                resourceAccess,
                toolAccess,
                samplingAccessPolicy,
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

    private static <T> T loadSingleton(Class<T> type) {
        Objects.requireNonNull(type, "type");
        var loader = ServiceLoader.load(type);
        var iterator = loader.iterator();
        if (!iterator.hasNext()) {
            throw new IllegalStateException("No implementation of " + type.getName()
                    + " is registered as a JPMS service for tests.");
        }
        var service = iterator.next();
        if (iterator.hasNext()) {
            throw new IllegalStateException("Multiple implementations of " + type.getName()
                    + " detected in tests.");
        }
        return service;
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
