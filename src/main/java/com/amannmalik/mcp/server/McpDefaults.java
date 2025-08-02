package com.amannmalik.mcp.server;

import com.amannmalik.mcp.content.ContentBlock;
import com.amannmalik.mcp.prompts.InMemoryPromptProvider;
import com.amannmalik.mcp.prompts.Prompt;
import com.amannmalik.mcp.prompts.PromptArgument;
import com.amannmalik.mcp.prompts.PromptMessageTemplate;
import com.amannmalik.mcp.prompts.PromptProvider;
import com.amannmalik.mcp.prompts.PromptTemplate;
import com.amannmalik.mcp.prompts.Role;
import com.amannmalik.mcp.security.PrivacyBoundaryEnforcer;
import com.amannmalik.mcp.security.ResourceAccessController;
import com.amannmalik.mcp.security.SamplingAccessPolicy;
import com.amannmalik.mcp.security.ToolAccessPolicy;
import com.amannmalik.mcp.server.completion.CompleteRequest;
import com.amannmalik.mcp.server.completion.CompletionProvider;
import com.amannmalik.mcp.server.completion.InMemoryCompletionProvider;
import com.amannmalik.mcp.server.resources.InMemoryResourceProvider;
import com.amannmalik.mcp.server.resources.Resource;
import com.amannmalik.mcp.server.resources.ResourceBlock;
import com.amannmalik.mcp.server.resources.ResourceProvider;
import com.amannmalik.mcp.server.resources.ResourceTemplate;
import com.amannmalik.mcp.server.tools.InMemoryToolProvider;
import com.amannmalik.mcp.server.tools.Tool;
import com.amannmalik.mcp.server.tools.ToolProvider;
import com.amannmalik.mcp.server.tools.ToolResult;
import jakarta.json.Json;

import java.util.List;
import java.util.Map;

/** Utility methods for default server components used by {@link McpServer}. */
final class McpDefaults {
    private McpDefaults() {}

    static ResourceProvider resources() {
        Resource r = new Resource("test://example", "example", null, null, "text/plain", 5L, null, null);
        ResourceBlock.Text block = new ResourceBlock.Text("test://example", "text/plain", "hello", null);
        ResourceTemplate t = new ResourceTemplate("test://template", "example_template", null, null, "text/plain", null, null);
        return new InMemoryResourceProvider(List.of(r), Map.of(r.uri(), block), List.of(t));
    }

    static ToolProvider tools() {
        var schema = Json.createObjectBuilder().add("type", "object").build();
        Tool tool = new Tool("test_tool", "Test Tool", null, schema, null, null, null);
        return new InMemoryToolProvider(
                List.of(tool),
                Map.of("test_tool", a -> new ToolResult(
                        Json.createArrayBuilder()
                                .add(Json.createObjectBuilder()
                                        .add("type", "text")
                                        .add("text", "ok")
                                        .build())
                                .build(), null, false, null)));
    }

    static PromptProvider prompts() {
        InMemoryPromptProvider p = new InMemoryPromptProvider();
        PromptArgument arg = new PromptArgument("test_arg", null, null, true, null);
        Prompt prompt = new Prompt("test_prompt", "Test Prompt", null, List.of(arg), null);
        PromptMessageTemplate msg = new PromptMessageTemplate(Role.USER, new ContentBlock.Text("hello", null, null));
        p.add(new PromptTemplate(prompt, List.of(msg)));
        return p;
    }

    static CompletionProvider completions() {
        InMemoryCompletionProvider provider = new InMemoryCompletionProvider();
        provider.add(new CompleteRequest.Ref.PromptRef("test_prompt", null, null), "test_arg", Map.of(), List.of("test_completion"));
        return provider;
    }

    static ToolAccessPolicy toolAccess() {
        return ToolAccessPolicy.PERMISSIVE;
    }

    static SamplingAccessPolicy samplingAccess() {
        return SamplingAccessPolicy.PERMISSIVE;
    }

    static ResourceAccessController privacyBoundary(String principalId) {
        var p = new PrivacyBoundaryEnforcer();
        for (Role a : Role.values()) p.allow(principalId, a);
        return p;
    }
}
