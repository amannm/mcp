package com.amannmalik.mcp.util;

import com.amannmalik.mcp.api.McpServerConfiguration;
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

public final class ServerDefaults {
    private static final ResourceProvider RESOURCES;
    private static final ToolProvider TOOLS;
    private static final PromptProvider PROMPTS;
    private static final CompletionProvider COMPLETIONS;
    private static final SamplingProvider SAMPLING;

    static {
        var ann = new Annotations(Set.of(Role.USER), 0.5, Instant.parse("2024-01-01T00:00:00Z"));
        var r0 = new Resource(URI.create("file:///sample/example.txt"), "example", null, null, "text/plain", 5L, ann, null);
        var r1 = new Resource(URI.create("https://example.com/resource"), "web", null, null, "text/plain", 6L, ann, null);
        var r2 = new Resource(URI.create("git://repo/file"), "repo", null, null, "text/plain", 7L, ann, null);
        Map<URI, ResourceBlock> content = Map.of(
                r0.uri(), new ResourceBlock.Text(r0.uri(), "text/plain", "hello", null),
                r1.uri(), new ResourceBlock.Text(r1.uri(), "text/plain", "web", null),
                r2.uri(), new ResourceBlock.Text(r2.uri(), "text/plain", "repo", null)
        );
        var template = new ResourceTemplate("file:///sample/template", "example_template", null, null, "text/plain", null, null);
        RESOURCES = new InMemoryResourceProvider(List.of(r0, r1, r2), content, List.of(template));

        var schema = Json.createObjectBuilder().add("type", "object").build();
        var outSchema = Json.createObjectBuilder()
                .add("type", "object")
                .add("properties", Json.createObjectBuilder()
                        .add("message", Json.createObjectBuilder().add("type", "string")))
                .add("required", Json.createArrayBuilder().add("message"))
                .build();
        var tool = new Tool("test_tool", "Test Tool", "Demonstrates successful execution", schema, outSchema,
                new ToolAnnotations("Annotated Tool", true, null, null, null), null);
        var errorTool = new Tool("error_tool", "Error Tool", "Always fails", schema, null, null, null);
        var eschema = Json.createObjectBuilder()
                .add("type", "object")
                .add("properties", Json.createObjectBuilder()
                        .add("msg", Json.createObjectBuilder().add("type", "string")))
                .add("required", Json.createArrayBuilder().add("msg"))
                .build();
        var eliciting = new Tool("echo_tool", "Echo Tool", "Echoes the provided message", eschema, null, null, null);
        var slow = new Tool("slow_tool", "Slow Tool", "Delays before responding", schema, null, null, null);
        var img = new Tool("image_tool", "Image Tool", "Returns image content", schema, null, null, null);
        var audio = new Tool("audio_tool", "Audio Tool", "Returns audio content", schema, null, null, null);
        var link = new Tool("link_tool", "Link Tool", "Returns resource link", schema, null, null, null);
        var embedded = new Tool("embedded_tool", "Embedded Resource Tool", "Returns embedded resource", schema, null, null, null);
        TOOLS = new InMemoryToolProvider(
                List.of(tool, errorTool, eliciting, slow, img, audio, link, embedded),
                Map.of(
                        "test_tool", a -> new ToolResult(
                                Json.createArrayBuilder()
                                        .add(Json.createObjectBuilder()
                                                .add("type", "text")
                                                .add("text", "ok")
                                                .build())
                                        .build(),
                                Json.createObjectBuilder().add("message", "ok").build(),
                                false, null),
                        "error_tool", a -> new ToolResult(
                                Json.createArrayBuilder()
                                        .add(Json.createObjectBuilder()
                                                .add("type", "text")
                                                .add("text", "fail")
                                                .build())
                                        .build(), null, true, null),
                        "echo_tool", a -> new ToolResult(
                                Json.createArrayBuilder()
                                        .add(Json.createObjectBuilder()
                                                .add("type", "text")
                                                .add("text", a.getString("msg"))
                                                .build())
                                        .build(), null, false, null),
                        "slow_tool", a -> {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                            }
                            return new ToolResult(
                                    Json.createArrayBuilder()
                                            .add(Json.createObjectBuilder()
                                                    .add("type", "text")
                                                    .add("text", "ok")
                                                    .build())
                                            .build(),
                                    null, false, null);
                        },
                        "image_tool", a -> new ToolResult(
                                Json.createArrayBuilder()
                                        .add(Json.createObjectBuilder()
                                                .add("type", "image")
                                                .add("data", "")
                                                .add("encoding", "base64")
                                                .build())
                                        .build(), null, false, null),
                        "audio_tool", a -> new ToolResult(
                                Json.createArrayBuilder()
                                        .add(Json.createObjectBuilder()
                                                .add("type", "audio")
                                                .add("data", "")
                                                .add("encoding", "base64")
                                                .build())
                                        .build(), null, false, null),
                        "link_tool", a -> new ToolResult(
                                Json.createArrayBuilder()
                                        .add(Json.createObjectBuilder()
                                                .add("type", "resource_link")
                                                .add("uri", r0.uri().toString())
                                                .build())
                                        .build(), null, false, null),
                        "embedded_tool", a -> new ToolResult(
                                Json.createArrayBuilder()
                                        .add(Json.createObjectBuilder()
                                                .add("type", "resource")
                                                .add("resource", Json.createObjectBuilder()
                                                        .add("uri", r0.uri().toString())
                                                        .add("name", r0.name())
                                                        .build())
                                                .build())
                                        .build(), null, false, null)
                ));

        var promptProvider = new InMemoryPromptProvider();
        var arg = new PromptArgument("test_arg", null, null, true, null);
        var prompt = new Prompt("test_prompt", "Test Prompt", null, List.of(arg), null);
        var msg = new PromptMessageTemplate(Role.USER, new ContentBlock.Text("hello", null, null));
        promptProvider.add(new PromptTemplate(prompt, List.of(msg)));
        PROMPTS = promptProvider;

        var completionProvider = new InMemoryCompletionProvider();
        completionProvider.add(new Ref.PromptRef("test_prompt", null, null), "test_arg", Map.of(), List.of("test_completion"));
        COMPLETIONS = completionProvider;

        SAMPLING = new InteractiveSamplingProvider(true);
    }

    private ServerDefaults() {
    }

    public static ResourceProvider resources() {
        /// Return a defensive delegate to avoid exposing internal mutable provider instance.
        final var delegate = RESOURCES;
        return new ResourceProvider() {
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
        };
    }

    public static ToolProvider tools() {
        /// Return a defensive delegate to avoid exposing internal mutable provider instance.
        final var delegate = TOOLS;
        return new ToolProvider() {
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
        };
    }

    public static PromptProvider prompts() {
        /// Return a defensive delegate to avoid exposing internal mutable provider instance.
        final var delegate = PROMPTS;
        return new PromptProvider() {
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
        };
    }

    public static CompletionProvider completions() {
        /// Return a defensive delegate to avoid exposing internal mutable provider instance.
        final var delegate = COMPLETIONS;
        return new CompletionProvider() {
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
        };
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
}
