package com.amannmalik.mcp.server;

import com.amannmalik.mcp.content.ContentBlock;
import com.amannmalik.mcp.prompts.*;
import com.amannmalik.mcp.resources.*;
import com.amannmalik.mcp.security.*;
import com.amannmalik.mcp.server.completion.*;
import com.amannmalik.mcp.server.tools.*;
import jakarta.json.Json;

import java.util.List;
import java.util.Map;

public final class ServerDefaults {
    private ServerDefaults() {
    }

    public static ResourceProvider resources() {
        Resource r = new Resource("test://example", "example", null, null, "text/plain", 5L, null, null);
        ResourceBlock.Text block = new ResourceBlock.Text("test://example", "text/plain", "hello", null);
        ResourceTemplate t = new ResourceTemplate("test://template", "example_template", null, null, "text/plain", null, null);
        return new InMemoryResourceProvider(List.of(r), Map.of(r.uri(), block), List.of(t));
    }

    public static ToolProvider tools() {
        var schema = Json.createObjectBuilder().add("type", "object").build();
        Tool tool = new Tool("test_tool", "Test Tool", null, schema, null, null, null);
        var eschema = Json.createObjectBuilder()
                .add("type", "object")
                .add("properties", Json.createObjectBuilder()
                        .add("msg", Json.createObjectBuilder().add("type", "string")))
                .add("required", Json.createArrayBuilder().add("msg"))
                .build();
        Tool eliciting = new Tool("echo_tool", "Echo Tool", null, eschema, null, null, null);
        return new InMemoryToolProvider(
                List.of(tool, eliciting),
                Map.of(
                        "test_tool", a -> new ToolResult(
                                Json.createArrayBuilder()
                                        .add(Json.createObjectBuilder()
                                                .add("type", "text")
                                                .add("text", "ok")
                                                .build())
                                        .build(), null, false, null),
                        "echo_tool", a -> new ToolResult(
                                Json.createArrayBuilder()
                                        .add(Json.createObjectBuilder()
                                                .add("type", "text")
                                                .add("text", a.getString("msg"))
                                                .build())
                                        .build(), null, false, null)));
    }

    public static PromptProvider prompts() {
        InMemoryPromptProvider p = new InMemoryPromptProvider();
        PromptArgument arg = new PromptArgument("test_arg", null, null, true, null);
        Prompt prompt = new Prompt("test_prompt", "Test Prompt", null, List.of(arg), null);
        PromptMessageTemplate msg = new PromptMessageTemplate(Role.USER, new ContentBlock.Text("hello", null, null));
        p.add(new PromptTemplate(prompt, List.of(msg)));
        return p;
    }

    public static CompletionProvider completions() {
        InMemoryCompletionProvider provider = new InMemoryCompletionProvider();
        provider.add(new CompleteRequest.Ref.PromptRef("test_prompt", null, null), "test_arg", Map.of(), List.of("test_completion"));
        return provider;
    }

    public static ToolAccessPolicy toolAccess() {
        return ToolAccessPolicy.PERMISSIVE;
    }

    public static SamplingAccessPolicy samplingAccess() {
        return SamplingAccessPolicy.PERMISSIVE;
    }

    public static ResourceAccessController privacyBoundary(String principalId) {
        var p = new PrivacyBoundaryEnforcer();
        for (Role a : Role.values()) p.allow(principalId, a);
        return p;
    }
}
