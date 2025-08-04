package com.amannmalik.mcp;

import com.amannmalik.mcp.annotations.Annotations;
import com.amannmalik.mcp.completion.*;
import com.amannmalik.mcp.config.McpConfiguration;
import com.amannmalik.mcp.content.ContentBlock;
import com.amannmalik.mcp.core.McpComponents;
import com.amannmalik.mcp.host.PrivacyBoundaryEnforcer;
import com.amannmalik.mcp.prompts.*;
import com.amannmalik.mcp.resources.*;
import com.amannmalik.mcp.sampling.*;
import com.amannmalik.mcp.tools.*;
import jakarta.json.Json;

import java.time.Instant;
import java.util.*;

public final class Locator {
    private static final McpComponents COMPONENTS = init();
    private Locator() {
    }

    private static McpComponents init() {
        Annotations ann = new Annotations(Set.of(Role.USER), 0.5, Instant.parse("2024-01-01T00:00:00Z"));
        Resource r0 = new Resource("test://example", "example", null, null, "text/plain", 5L, ann, null);
        Resource r1 = new Resource("test://example1", "example1", null, null, "text/plain", 5L, ann, null);
        Resource r2 = new Resource("test://example2", "example2", null, null, "text/plain", 5L, ann, null);
        Map<String, ResourceBlock> c = Map.of(
                r0.uri(), new ResourceBlock.Text(r0.uri(), "text/plain", "hello", null),
                r1.uri(), new ResourceBlock.Text(r1.uri(), "text/plain", "hello", null),
                r2.uri(), new ResourceBlock.Text(r2.uri(), "text/plain", "hello", null)
        );
        ResourceTemplate t = new ResourceTemplate("test://template", "example_template", null, null, "text/plain", null, null);
        ResourceProvider res = new InMemoryResourceProvider(List.of(r0), c, List.of(t));

        var schema = Json.createObjectBuilder().add("type", "object").build();
        var outSchema = Json.createObjectBuilder()
                .add("type", "object")
                .add("properties", Json.createObjectBuilder()
                        .add("message", Json.createObjectBuilder().add("type", "string")))
                .add("required", Json.createArrayBuilder().add("message"))
                .build();
        Tool tool = new Tool("test_tool", "Test Tool", null, schema, outSchema,
                new ToolAnnotations("Annotated Tool", true, null, null, null), null);
        Tool errorTool = new Tool("error_tool", "Error Tool", null, schema, null, null, null);
        var eschema = Json.createObjectBuilder()
                .add("type", "object")
                .add("properties", Json.createObjectBuilder()
                        .add("msg", Json.createObjectBuilder().add("type", "string")))
                .add("required", Json.createArrayBuilder().add("msg"))
                .build();
        Tool eliciting = new Tool("echo_tool", "Echo Tool", null, eschema, null, null, null);
        Tool slow = new Tool("slow_tool", "Slow Tool", null, schema, null, null, null);
        ToolProvider tp = new InMemoryToolProvider(
                List.of(tool, errorTool, eliciting, slow),
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
                        }
                ));

        InMemoryPromptProvider p = new InMemoryPromptProvider();
        PromptArgument arg = new PromptArgument("test_arg", null, null, true, null);
        Prompt prompt = new Prompt("test_prompt", "Test Prompt", null, List.of(arg), null);
        PromptMessageTemplate msg = new PromptMessageTemplate(Role.USER, new ContentBlock.Text("hello", null, null));
        p.add(new PromptTemplate(prompt, List.of(msg)));

        InMemoryCompletionProvider cp = new InMemoryCompletionProvider();
        cp.add(new CompleteRequest.Ref.PromptRef("test_prompt", null, null), "test_arg", Map.of(), List.of("test_completion"));

        SamplingProvider sp = new InteractiveSamplingProvider(true);

        return McpComponents.builder()
                .withResources(res)
                .withTools(tp)
                .withPrompts(p)
                .withCompletions(cp)
                .withSampling(sp)
                .withToolAccess(ToolAccessPolicy.PERMISSIVE)
                .withSamplingAccess(SamplingAccessPolicy.PERMISSIVE)
                .withPrivacyBoundary(privacyBoundary(McpConfiguration.current().defaultBoundary()))
                .build();
    }

    public static ResourceProvider resources() { return COMPONENTS.resources(); }
    public static ToolProvider tools() { return COMPONENTS.tools(); }
    public static PromptProvider prompts() { return COMPONENTS.prompts(); }
    public static CompletionProvider completions() { return COMPONENTS.completions(); }
    public static SamplingProvider sampling() { return COMPONENTS.sampling(); }
    public static ToolAccessPolicy toolAccess() { return COMPONENTS.toolAccess(); }
    public static SamplingAccessPolicy samplingAccess() { return COMPONENTS.samplingAccess(); }

    public static ResourceAccessController privacyBoundary(String principalId) {
        var p = new PrivacyBoundaryEnforcer();
        for (Role a : Role.values()) p.allow(principalId, a);
        return p;
    }
}
