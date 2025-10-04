package com.amannmalik.mcp.test;

import com.amannmalik.mcp.api.McpServer;
import com.amannmalik.mcp.api.config.*;
import com.amannmalik.mcp.spi.*;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/** In-process HTTP server using minimal providers, MIXED HTTPS mode. */
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
                ToolAccessPolicy.PERMISSIVE,
                SamplingAccessPolicy.PERMISSIVE,
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
            public ResourceBlock read(URI uri) { return null; }
            @Override
            public Optional<Resource> get(URI uri) { return Optional.empty(); }
            @Override
            public Pagination.Page<Resource> list(Cursor cursor) { return new Pagination.Page<>(List.of(), Cursor.End.INSTANCE); }
            @Override
            public Pagination.Page<ResourceTemplate> listTemplates(Cursor cursor) { return new Pagination.Page<>(List.of(), Cursor.End.INSTANCE); }
            @Override
            public AutoCloseable subscribe(URI uri, Consumer<ResourceUpdate> listener) { return () -> {}; }
            @Override
            public boolean supportsSubscribe() { return false; }
            @Override
            public AutoCloseable onListChanged(Runnable listener) { return () -> {}; }
            @Override
            public boolean supportsListChanged() { return false; }
            @Override
            public void close() { }
        };

        var tools = new ToolProvider() {
            final Tool echo = new Tool("echo", "Echo", "Echo tool", Json.createObjectBuilder().add("type","object").build(), null, null, null);
            @Override
            public Pagination.Page<Tool> list(Cursor cursor) { return new Pagination.Page<>(List.of(echo), Cursor.End.INSTANCE); }
            @Override
            public AutoCloseable onListChanged(Runnable listener) { return () -> {}; }
            @Override
            public boolean supportsListChanged() { return true; }
            @Override
            public void close() { }
            @Override
            public Optional<Tool> find(String name) { return Optional.of(echo); }
            @Override
            public ToolResult call(String name, JsonObject arguments) { return new ToolResult(Json.createArrayBuilder().build(), null, false, null); }
        };

        var prompts = new PromptProvider() {
            @Override
            public Pagination.Page<Prompt> list(Cursor cursor) { return new Pagination.Page<>(List.of(), Cursor.End.INSTANCE); }
            @Override
            public AutoCloseable onListChanged(Runnable listener) { return () -> {}; }
            @Override
            public boolean supportsListChanged() { return true; }
            @Override
            public void close() { }
            @Override
            public Optional<Prompt> find(String name) { return Optional.empty(); }
            @Override
            public PromptInstance get(String name, Map<String, String> arguments) { return new PromptInstance("desc", List.of()); }
        };

        var completions = new CompletionProvider() {
            @Override
            public Pagination.Page<Ref> list(Cursor cursor) { return new Pagination.Page<>(List.of(), Cursor.End.INSTANCE); }
            @Override
            public AutoCloseable onListChanged(Runnable listener) { return () -> {}; }
            @Override
            public boolean supportsListChanged() { return true; }
            @Override
            public void close() { }
            @Override
            public CompleteResult execute(String name, JsonObject args) { return new CompleteResult(new Completion(List.of(), 0, false), null); }
        };

        var sampling = new SamplingProvider() {
            @Override
            public void close() { }
            @Override
            public CreateMessageResponse createMessage(CreateMessageRequest request, Duration timeoutMillis) { return new CreateMessageResponse(Role.ASSISTANT, new ContentBlock.Text("ok", null, null), "model", "stop", null); }
        };

        var access = (ResourceAccessPolicy) (principal1, annotations) -> { /* allow all */ };

        var principal = new Principal(base.defaultPrincipal(), Set.of());

        var server = new McpServer(config, resources, tools, prompts, completions, sampling, access, principal, null);
        Thread.ofVirtual().start(() -> {
            try { server.serve(); } catch (IOException ignore) { }
        });
        return new ServerHarness(server, port);
    }

    public URI endpoint() { return URI.create("http://127.0.0.1:" + port + "/"); }
    public int port() { return port; }

    @Override
    public void close() throws IOException { server.close(); }

    private static int freePort() throws IOException { try (var s = new ServerSocket(0)) { return s.getLocalPort(); } }
}
