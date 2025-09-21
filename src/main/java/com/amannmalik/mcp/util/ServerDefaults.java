package com.amannmalik.mcp.util;

import com.amannmalik.mcp.api.config.McpServerConfiguration;
import com.amannmalik.mcp.completion.InMemoryCompletionProvider;
import com.amannmalik.mcp.prompts.*;
import com.amannmalik.mcp.resources.InMemoryResourceProvider;
import com.amannmalik.mcp.sampling.InteractiveSamplingProvider;
import com.amannmalik.mcp.security.ResourceAccessController;
import com.amannmalik.mcp.spi.*;
import com.amannmalik.mcp.tools.InMemoryToolProvider;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public final class ServerDefaults {
    private static final ResourceDefaults RESOURCE_DEFAULTS = createResourceDefaults();
    private static final ResourceProvider RESOURCES = RESOURCE_DEFAULTS.provider();
    private static final ToolProvider TOOLS = createToolProvider(RESOURCE_DEFAULTS.sampleResource());
    private static final PromptProvider PROMPTS = createPromptProvider();
    private static final CompletionProvider COMPLETIONS = createCompletionProvider();
    private static final SamplingProvider SAMPLING = new InteractiveSamplingProvider(true);

    private ServerDefaults() {
    }

    public static ResourceProvider resources() {
        return new DelegatingResourceProvider(RESOURCES);
    }

    public static ToolProvider tools() {
        return new DelegatingToolProvider(TOOLS);
    }

    public static PromptProvider prompts() {
        return new DelegatingPromptProvider(PROMPTS);
    }

    public static CompletionProvider completions() {
        return new DelegatingCompletionProvider(COMPLETIONS);
    }

    public static SamplingProvider sampling() {
        return SAMPLING;
    }

    public static ResourceAccessPolicy privacyBoundary(String principalId) {
        var p = new ResourceAccessController();
        for (var a : Role.values()) {
            p.allow(principalId, a);
        }
        return p;
    }

    public static Principal principal() {
        return new Principal(McpServerConfiguration.defaultConfiguration().defaultPrincipal(), Set.of());
    }

    private static ResourceDefaults createResourceDefaults() {
        var annotations = new Annotations(Set.of(Role.USER), 0.5, Instant.parse("2024-01-01T00:00:00Z"));
        var sampleFile = new Resource(
                URI.create("file:///sample/example.txt"),
                "example",
                null,
                null,
                "text/plain",
                5L,
                annotations,
                null);
        var webResource = new Resource(
                URI.create("https://example.com/resource"),
                "web",
                null,
                null,
                "text/plain",
                6L,
                annotations,
                null);
        var gitResource = new Resource(
                URI.create("git://repo/file"),
                "repo",
                null,
                null,
                "text/plain",
                7L,
                annotations,
                null);

        var content = Map.<URI, ResourceBlock>of(
                sampleFile.uri(), new ResourceBlock.Text(sampleFile.uri(), "text/plain", "hello", null),
                webResource.uri(), new ResourceBlock.Text(webResource.uri(), "text/plain", "web", null),
                gitResource.uri(), new ResourceBlock.Text(gitResource.uri(), "text/plain", "repo", null));

        var template = new ResourceTemplate(
                "file:///sample/template",
                "example_template",
                null,
                null,
                "text/plain",
                null,
                null);

        var provider = new InMemoryResourceProvider(
                List.of(sampleFile, webResource, gitResource),
                content,
                List.of(template));

        return new ResourceDefaults(provider, sampleFile);
    }

    private static ToolProvider createToolProvider(Resource sampleResource) {
        Objects.requireNonNull(sampleResource, "sampleResource");

        var schema = Json.createObjectBuilder()
                .add("type", "object")
                .build();

        var outputSchema = Json.createObjectBuilder()
                .add("type", "object")
                .add("properties", Json.createObjectBuilder()
                        .add("message", Json.createObjectBuilder().add("type", "string")))
                .add("required", Json.createArrayBuilder().add("message"))
                .build();

        var echoSchema = Json.createObjectBuilder()
                .add("type", "object")
                .add("properties", Json.createObjectBuilder()
                        .add("msg", Json.createObjectBuilder().add("type", "string")))
                .add("required", Json.createArrayBuilder().add("msg"))
                .build();

        var tools = List.of(
                new Tool(
                        "test_tool",
                        "Test Tool",
                        "Demonstrates successful execution",
                        schema,
                        outputSchema,
                        new ToolAnnotations("Annotated Tool", true, null, null, null),
                        null),
                new Tool("error_tool", "Error Tool", "Always fails", schema, null, null, null),
                new Tool("echo_tool", "Echo Tool", "Echoes the provided message", echoSchema, null, null, null),
                new Tool("slow_tool", "Slow Tool", "Delays before responding", schema, null, null, null),
                new Tool("image_tool", "Image Tool", "Returns image content", schema, null, null, null),
                new Tool("audio_tool", "Audio Tool", "Returns audio content", schema, null, null, null),
                new Tool("link_tool", "Link Tool", "Returns resource link", schema, null, null, null),
                new Tool("embedded_tool", "Embedded Resource Tool", "Returns embedded resource", schema, null, null, null));

        var handlers = Map.<String, Function<JsonObject, ToolResult>>ofEntries(
                Map.entry("test_tool", args -> structuredMessageResult("ok")),
                Map.entry("error_tool", args -> errorResult("fail")),
                Map.entry("echo_tool", args -> textResult(args.getString("msg"))),
                Map.entry("slow_tool", args -> slowResult()),
                Map.entry("image_tool", args -> binaryResult("image")),
                Map.entry("audio_tool", args -> binaryResult("audio")),
                Map.entry("link_tool", args -> linkResult(sampleResource.uri().toString())),
                Map.entry("embedded_tool", args -> embeddedResult(sampleResource)));

        return new InMemoryToolProvider(tools, handlers);
    }

    private static PromptProvider createPromptProvider() {
        var provider = new InMemoryPromptProvider();
        var argument = new PromptArgument("test_arg", null, null, true, null);
        var prompt = new Prompt("test_prompt", "Test Prompt", null, List.of(argument), null);
        var message = new PromptMessageTemplate(Role.USER, new ContentBlock.Text("hello", null, null));
        provider.add(new PromptTemplate(prompt, List.of(message)));
        return provider;
    }

    private static CompletionProvider createCompletionProvider() {
        var provider = new InMemoryCompletionProvider();
        provider.add(new Ref.PromptRef("test_prompt", null, null), "test_arg", Map.of(), List.of("test_completion"));
        return provider;
    }

    private static ToolResult structuredMessageResult(String message) {
        return new ToolResult(
                Json.createArrayBuilder()
                        .add(textBlock(message))
                        .build(),
                Json.createObjectBuilder().add("message", message).build(),
                false,
                null);
    }

    private static ToolResult errorResult(String message) {
        return new ToolResult(
                Json.createArrayBuilder()
                        .add(textBlock(message))
                        .build(),
                null,
                true,
                null);
    }

    private static ToolResult textResult(String message) {
        return new ToolResult(
                Json.createArrayBuilder()
                        .add(textBlock(message))
                        .build(),
                null,
                false,
                null);
    }

    private static ToolResult slowResult() {
        delay();
        return textResult("ok");
    }

    private static void delay() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private static ToolResult binaryResult(String type) {
        return new ToolResult(
                Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("type", type)
                                .add("data", "")
                                .add("encoding", "base64")
                                .build())
                        .build(),
                null,
                false,
                null);
    }

    private static ToolResult linkResult(String uri) {
        return new ToolResult(
                Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("type", "resource_link")
                                .add("uri", uri)
                                .build())
                        .build(),
                null,
                false,
                null);
    }

    private static ToolResult embeddedResult(Resource resource) {
        return new ToolResult(
                Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("type", "resource")
                                .add("resource", Json.createObjectBuilder()
                                        .add("uri", resource.uri().toString())
                                        .add("name", resource.name())
                                        .build())
                                .build())
                        .build(),
                null,
                false,
                null);
    }

    private static JsonObject textBlock(String message) {
        return Json.createObjectBuilder()
                .add("type", "text")
                .add("text", message)
                .build();
    }

    private record DelegatingResourceProvider(ResourceProvider delegate) implements ResourceProvider {
        private DelegatingResourceProvider(ResourceProvider delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
        }

        @Override
        public ResourceBlock read(URI uri) {
            return delegate.read(uri);
        }

        @Override
        public Optional<Resource> get(URI uri) {
            return delegate.get(uri);
        }

        @Override
        public Pagination.Page<Resource> list(Cursor cursor) {
            return delegate.list(cursor);
        }

        @Override
        public Pagination.Page<ResourceTemplate> listTemplates(Cursor cursor) {
            return delegate.listTemplates(cursor);
        }

        @Override
        public AutoCloseable subscribe(URI uri, Consumer<ResourceUpdate> listener) {
            return delegate.subscribe(uri, listener);
        }

        @Override
        public boolean supportsSubscribe() {
            return delegate.supportsSubscribe();
        }

        @Override
        public AutoCloseable onListChanged(Runnable listener) {
            return delegate.onListChanged(listener);
        }

        @Override
        public boolean supportsListChanged() {
            return delegate.supportsListChanged();
        }

        @Override
        public void close() {
            delegate.close();
        }
    }

    private record DelegatingToolProvider(ToolProvider delegate) implements ToolProvider {
        private DelegatingToolProvider(ToolProvider delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
        }

        @Override
        public Pagination.Page<Tool> list(Cursor cursor) {
            return delegate.list(cursor);
        }

        @Override
        public AutoCloseable onListChanged(Runnable listener) {
            return delegate.onListChanged(listener);
        }

        @Override
        public boolean supportsListChanged() {
            return delegate.supportsListChanged();
        }

        @Override
        public void close() {
            delegate.close();
        }

        @Override
        public Optional<Tool> find(String name) {
            return delegate.find(name);
        }

        @Override
        public ToolResult call(String name, JsonObject arguments) {
            return delegate.call(name, arguments);
        }
    }

    private record DelegatingPromptProvider(PromptProvider delegate) implements PromptProvider {
        private DelegatingPromptProvider(PromptProvider delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
        }

        @Override
        public Pagination.Page<Prompt> list(Cursor cursor) {
            return delegate.list(cursor);
        }

        @Override
        public AutoCloseable onListChanged(Runnable listener) {
            return delegate.onListChanged(listener);
        }

        @Override
        public boolean supportsListChanged() {
            return delegate.supportsListChanged();
        }

        @Override
        public void close() {
            delegate.close();
        }

        @Override
        public Optional<Prompt> find(String name) {
            return delegate.find(name);
        }

        @Override
        public PromptInstance get(String name, Map<String, String> arguments) {
            return delegate.get(name, arguments);
        }
    }

    private record DelegatingCompletionProvider(CompletionProvider delegate) implements CompletionProvider {
        private DelegatingCompletionProvider(CompletionProvider delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
        }

        @Override
        public Pagination.Page<Ref> list(Cursor cursor) {
            return delegate.list(cursor);
        }

        @Override
        public AutoCloseable onListChanged(Runnable listener) {
            return delegate.onListChanged(listener);
        }

        @Override
        public boolean supportsListChanged() {
            return delegate.supportsListChanged();
        }

        @Override
        public void close() {
            delegate.close();
        }

        @Override
        public CompleteResult execute(String name, JsonObject args) throws InterruptedException {
            return delegate.execute(name, args);
        }
    }

    private record ResourceDefaults(ResourceProvider provider, Resource sampleResource) {
    }
}
