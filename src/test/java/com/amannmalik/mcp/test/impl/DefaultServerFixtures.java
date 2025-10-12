package com.amannmalik.mcp.test.impl;

import com.amannmalik.mcp.spi.*;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

final class DefaultServerFixtures {
    static final Resource SAMPLE_RESOURCE;
    static final List<Resource> RESOURCES;
    static final Map<URI, ResourceBlock> RESOURCE_CONTENT;
    static final List<ResourceTemplate> RESOURCE_TEMPLATES;
    static final List<Tool> TOOLS;
    static final Map<String, Function<JsonObject, ToolResult>> TOOL_HANDLERS;
    static final List<Prompt> PROMPTS;
    static final Map<String, PromptInstance> PROMPT_INSTANCES;
    static final List<Ref> COMPLETION_REFS;
    static final List<CompletionEntry> COMPLETION_ENTRIES;
    private static final int MAX_COMPLETION_VALUES = 100;

    static {
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
        var projectFile = new Resource(
                URI.create("file:///project/src/main.rs"),
                "main_rs",
                null,
                null,
                "text/plain",
                7L,
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
        SAMPLE_RESOURCE = sampleFile;
        RESOURCES = List.of(sampleFile, projectFile, webResource, gitResource);
        RESOURCE_CONTENT = Map.of(
                sampleFile.uri(), new ResourceBlock.Text(sampleFile.uri(), "text/plain", "hello", null),
                projectFile.uri(), new ResourceBlock.Text(projectFile.uri(), "text/plain", "fn main() {}", null),
                webResource.uri(), new ResourceBlock.Text(webResource.uri(), "text/plain", "web", null),
                gitResource.uri(), new ResourceBlock.Text(gitResource.uri(), "text/plain", "repo", null));
        var template = new ResourceTemplate(
                "file:///{path}",
                "example_template",
                null,
                null,
                "text/plain",
                null,
                null);
        RESOURCE_TEMPLATES = List.of(template);
        TOOLS = createTools();
        TOOL_HANDLERS = createToolHandlers();
        var promptTemplates = createPrompts();
        PROMPTS = promptTemplates.stream().map(PromptTemplateData::prompt).toList();
        PROMPT_INSTANCES = promptTemplates.stream()
                .collect(Collectors.toUnmodifiableMap(PromptTemplateData::name, PromptTemplateData::instance));
        var completionData = createCompletionEntries();
        COMPLETION_ENTRIES = completionData.entries();
        COMPLETION_REFS = completionData.refs();
    }

    private DefaultServerFixtures() {
    }

    static ToolResult structuredMessageResult(String message) {
        return new ToolResult(
                Json.createArrayBuilder()
                        .add(textBlock(message))
                        .build(),
                Json.createObjectBuilder().add("message", message).build(),
                false,
                null);
    }

    static ToolResult errorResult(String message) {
        return new ToolResult(
                Json.createArrayBuilder()
                        .add(textBlock(message))
                        .build(),
                null,
                true,
                null);
    }

    static ToolResult textResult(String message) {
        return new ToolResult(
                Json.createArrayBuilder()
                        .add(textBlock(message))
                        .build(),
                null,
                false,
                null);
    }

    static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        var normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.contains("<script")) {
            throw new IllegalArgumentException("disallowed input");
        }
        return value.replace("<", "&lt;").replace(">", "&gt;");
    }

    static ToolResult slowResult() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        return textResult("ok");
    }

    static ToolResult binaryResult(String type) {
        return new ToolResult(
                Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("type", type)
                                .add("data", Base64.getEncoder().encodeToString(type.getBytes(StandardCharsets.UTF_8)))
                                .add("encoding", "base64")
                                .build())
                        .build(),
                null,
                false,
                null);
    }

    static ToolResult linkResult(String uri) {
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

    static ToolResult embeddedResult(Resource resource) {
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

    static ToolResult structuredError(String message) {
        return new ToolResult(
                Json.createArrayBuilder()
                        .add(textBlock(message))
                        .build(),
                Json.createObjectBuilder().add("message", message).build(),
                true,
                null);
    }

    static List<String> limitValues(List<String> values) {
        if (values.size() <= MAX_COMPLETION_VALUES) {
            return values;
        }
        return values.subList(0, MAX_COMPLETION_VALUES);
    }

    private static JsonObject textBlock(String message) {
        return Json.createObjectBuilder()
                .add("type", "text")
                .add("text", message)
                .build();
    }

    private static List<Tool> createTools() {
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
        return List.of(
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
    }

    private static Map<String, Function<JsonObject, ToolResult>> createToolHandlers() {
        return Map.ofEntries(
                Map.entry("test_tool", args -> structuredMessageResult("ok")),
                Map.entry("error_tool", args -> structuredError("fail")),
                Map.entry("echo_tool", args -> textResult(sanitize(args.getString("msg")))),
                Map.entry("slow_tool", args -> slowResult()),
                Map.entry("image_tool", args -> binaryResult("image")),
                Map.entry("audio_tool", args -> binaryResult("audio")),
                Map.entry("link_tool", args -> linkResult(SAMPLE_RESOURCE.uri().toString())),
                Map.entry("embedded_tool", args -> embeddedResult(SAMPLE_RESOURCE)));
    }

    private static List<PromptTemplateData> createPrompts() {
        var templates = new ArrayList<PromptTemplateData>();
        var testArgument = new PromptArgument("test_arg", null, null, true, null);
        var testPrompt = new Prompt("test_prompt", "Test Prompt", null, List.of(testArgument), null);
        var testInstance = new PromptInstance(
                null,
                List.of(new PromptMessage(Role.USER, new ContentBlock.Text("hello", null, null))));
        templates.add(new PromptTemplateData(testPrompt.name(), testPrompt, testInstance));
        var codeArg = new PromptArgument("code", null, null, true, null);
        var langArg = new PromptArgument("language", null, null, false, null);
        var codeReview = new Prompt("code_review", "Code Review", null, List.of(codeArg, langArg), null);
        var codeInstance = new PromptInstance(
                null,
                List.of(new PromptMessage(Role.USER, new ContentBlock.Text("Review the code in the given language.", null, null))));
        templates.add(new PromptTemplateData(codeReview.name(), codeReview, codeInstance));
        var multiLang = new PromptArgument("language", null, null, true, null);
        var multiFramework = new PromptArgument("framework", null, null, false, null);
        var multiPrompt = new Prompt("multi", "Multi Arg", null, List.of(multiLang, multiFramework), null);
        var multiInstance = new PromptInstance(
                null,
                List.of(new PromptMessage(Role.USER, new ContentBlock.Text("Choose a framework.", null, null))));
        templates.add(new PromptTemplateData(multiPrompt.name(), multiPrompt, multiInstance));
        var textPrompt = new Prompt("text_prompt", "Text Prompt", null, List.of(), null);
        var textInstance = new PromptInstance(
                null,
                List.of(new PromptMessage(Role.USER, new ContentBlock.Text("Sample text", null, null))));
        templates.add(new PromptTemplateData(textPrompt.name(), textPrompt, textInstance));
        var imagePrompt = new Prompt("image_prompt", "Image Prompt", null, List.of(), null);
        var imageInstance = new PromptInstance(
                null,
                List.of(new PromptMessage(Role.USER,
                        new ContentBlock.Image("sample".getBytes(StandardCharsets.UTF_8), "image/png", null, null))));
        templates.add(new PromptTemplateData(imagePrompt.name(), imagePrompt, imageInstance));
        var audioPrompt = new Prompt("audio_prompt", "Audio Prompt", null, List.of(), null);
        var audioInstance = new PromptInstance(
                null,
                List.of(new PromptMessage(Role.USER,
                        new ContentBlock.Audio("sound".getBytes(StandardCharsets.UTF_8), "audio/wav", null, null))));
        templates.add(new PromptTemplateData(audioPrompt.name(), audioPrompt, audioInstance));
        if (RESOURCE_CONTENT.containsKey(SAMPLE_RESOURCE.uri())) {
            var resourcePrompt = new Prompt("resource_prompt", "Resource Prompt", null, List.of(), null);
            var resourceBlock = RESOURCE_CONTENT.get(SAMPLE_RESOURCE.uri());
            var resourceInstance = new PromptInstance(
                    null,
                    List.of(new PromptMessage(Role.USER,
                            new ContentBlock.EmbeddedResource(resourceBlock, null, null))));
            templates.add(new PromptTemplateData(resourcePrompt.name(), resourcePrompt, resourceInstance));
        }
        return List.copyOf(templates);
    }

    private static CompletionData createCompletionEntries() {
        var entries = new ArrayList<CompletionEntry>();
        var refs = new ArrayList<Ref>();
        var testRef = new Ref.PromptRef("test_prompt", null, null);
        entries.add(new CompletionEntry(testRef, "test_arg", Map.of(), List.of("test_completion")));
        refs.add(testRef);
        var codeRef = new Ref.PromptRef("code_review", null, null);
        entries.add(new CompletionEntry(codeRef, "language", Map.of(), List.of("python", "java", "rust")));
        refs.add(codeRef);
        var resourceRef = new Ref.ResourceRef("file:///{path}");
        entries.add(new CompletionEntry(resourceRef, "path", Map.of(), List.of("src/", "build/", "specification/")));
        refs.add(resourceRef);
        var multiRef = new Ref.PromptRef("multi", null, null);
        entries.add(new CompletionEntry(multiRef, "framework", Map.of("language", "python"), List.of("flask", "fastapi", "falcon")));
        refs.add(multiRef);
        var manyRef = new Ref.PromptRef("many", null, null);
        var manyValues = IntStream.range(0, 120)
                .mapToObj(i -> "item" + i)
                .toList();
        entries.add(new CompletionEntry(manyRef, "value", Map.of(), manyValues));
        refs.add(manyRef);
        return new CompletionData(List.copyOf(entries), List.copyOf(new LinkedHashSet<>(refs)));
    }

    record CompletionEntry(Ref ref, String argumentName, Map<String, String> context, List<String> values) {
    }

    private record PromptTemplateData(String name, Prompt prompt, PromptInstance instance) {
    }

    private record CompletionData(List<CompletionEntry> entries, List<Ref> refs) {
    }
}
