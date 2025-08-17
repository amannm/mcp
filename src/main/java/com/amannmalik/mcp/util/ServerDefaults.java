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

import java.time.Instant;
import java.util.*;

public final class ServerDefaults {
    private static final ResourceProvider RESOURCES;
    private static final ToolProvider TOOLS;
    private static final PromptProvider PROMPTS;
    private static final CompletionProvider COMPLETIONS;
    private static final SamplingProvider SAMPLING;

    static {
        Annotations ann = new Annotations(Set.of(Role.USER), 0.5, Instant.parse("2024-01-01T00:00:00Z"));
        Resource r0 = new Resource("test://example", "example", null, null, "text/plain", 5L, ann, null);
        Resource r1 = new Resource("test://example1", "example1", null, null, "text/plain", 5L, ann, null);
        Resource r2 = new Resource("test://example2", "example2", null, null, "text/plain", 5L, ann, null);
        Map<String, ResourceBlock> content = Map.of(
                r0.uri(), new ResourceBlock.Text(r0.uri(), "text/plain", "hello", null),
                r1.uri(), new ResourceBlock.Text(r1.uri(), "text/plain", "hello", null),
                r2.uri(), new ResourceBlock.Text(r2.uri(), "text/plain", "hello", null)
        );
        ResourceTemplate template = new ResourceTemplate("test://template", "example_template", null, null, "text/plain", null, null);
        RESOURCES = new InMemoryResourceProvider(List.of(r0), content, List.of(template));

        var schema = Json.createObjectBuilder().add("type", "object").build();
        var outSchema = Json.createObjectBuilder()
                .add("type", "object")
                .add("properties", Json.createObjectBuilder()
                        .add("message", Json.createObjectBuilder().add("type", "string")))
                .add("required", Json.createArrayBuilder().add("message"))
                .build();
        Tool tool = new Tool("test_tool", "Test Tool", "Demonstrates successful execution", schema, outSchema,
                new ToolAnnotations("Annotated Tool", true, null, null, null), null);
        Tool errorTool = new Tool("error_tool", "Error Tool", "Always fails", schema, null, null, null);
        var eschema = Json.createObjectBuilder()
                .add("type", "object")
                .add("properties", Json.createObjectBuilder()
                        .add("msg", Json.createObjectBuilder().add("type", "string")))
                .add("required", Json.createArrayBuilder().add("msg"))
                .build();
        Tool eliciting = new Tool("echo_tool", "Echo Tool", "Echoes the provided message", eschema, null, null, null);
        Tool slow = new Tool("slow_tool", "Slow Tool", "Delays before responding", schema, null, null, null);
        Tool img = new Tool("image_tool", "Image Tool", "Returns image content", schema, null, null, null);
        Tool audio = new Tool("audio_tool", "Audio Tool", "Returns audio content", schema, null, null, null);
        Tool link = new Tool("link_tool", "Link Tool", "Returns resource link", schema, null, null, null);
        Tool embedded = new Tool("embedded_tool", "Embedded Resource Tool", "Returns embedded resource", schema, null, null, null);
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
                                                .add("uri", r0.uri())
                                                .build())
                                        .build(), null, false, null),
                        "embedded_tool", a -> new ToolResult(
                                Json.createArrayBuilder()
                                        .add(Json.createObjectBuilder()
                                                .add("type", "embedded_resource")
                                                .add("resource", Json.createObjectBuilder()
                                                        .add("uri", r0.uri())
                                                        .add("name", r0.name())
                                                        .build())
                                                .build())
                                        .build(), null, false, null)
                ));

        InMemoryPromptProvider promptProvider = new InMemoryPromptProvider();
        PromptArgument arg = new PromptArgument("test_arg", null, null, true, null);
        Prompt prompt = new Prompt("test_prompt", "Test Prompt", null, List.of(arg), null);
        PromptMessageTemplate msg = new PromptMessageTemplate(Role.USER, new ContentBlock.Text("hello", null, null));
        promptProvider.add(new PromptTemplate(prompt, List.of(msg)));
        PROMPTS = promptProvider;

        InMemoryCompletionProvider completionProvider = new InMemoryCompletionProvider();
        completionProvider.add(new Ref.PromptRef("test_prompt", null, null), "test_arg", Map.of(), List.of("test_completion"));
        COMPLETIONS = completionProvider;

        SAMPLING = new InteractiveSamplingProvider(true);
    }

    private ServerDefaults() {
    }

    public static ResourceProvider resources() {
        return RESOURCES;
    }

    public static ToolProvider tools() {
        return TOOLS;
    }

    public static PromptProvider prompts() {
        return PROMPTS;
    }

    public static CompletionProvider completions() {
        return COMPLETIONS;
    }

    public static SamplingProvider sampling() {
        return SAMPLING;
    }

    public static ResourceAccessPolicy privacyBoundary(String principalId) {
        var p = new ResourceAccessController();
        for (Role a : Role.values()) p.allow(principalId, a);
        return p;
    }

    public static Principal principal() {
        return new Principal(McpServerConfiguration.defaultConfiguration().defaultPrincipal(), Set.of());
    }
}
