package com.amannmalik.mcp;

import com.amannmalik.mcp.completion.CompletionProvider;
import com.amannmalik.mcp.prompts.PromptProvider;
import com.amannmalik.mcp.resources.ResourceAccessController;
import com.amannmalik.mcp.resources.ResourceProvider;
import com.amannmalik.mcp.sampling.SamplingAccessPolicy;
import com.amannmalik.mcp.sampling.SamplingProvider;
import com.amannmalik.mcp.tools.ToolAccessPolicy;
import com.amannmalik.mcp.tools.ToolProvider;

import java.util.Objects;

public record McpComponents(ResourceProvider resources,
                            ToolProvider tools,
                            PromptProvider prompts,
                            CompletionProvider completions,
                            SamplingProvider sampling,
                            ToolAccessPolicy toolAccess,
                            SamplingAccessPolicy samplingAccess,
                            ResourceAccessController privacyBoundary) {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private ResourceProvider resources;
        private ToolProvider tools;
        private PromptProvider prompts;
        private CompletionProvider completions;
        private SamplingProvider sampling;
        private ToolAccessPolicy toolAccess;
        private SamplingAccessPolicy samplingAccess;
        private ResourceAccessController privacyBoundary;

        public Builder withResources(ResourceProvider provider) {
            resources = Objects.requireNonNull(provider);
            return this;
        }

        public Builder withTools(ToolProvider provider) {
            tools = Objects.requireNonNull(provider);
            return this;
        }

        public Builder withPrompts(PromptProvider provider) {
            prompts = Objects.requireNonNull(provider);
            return this;
        }

        public Builder withCompletions(CompletionProvider provider) {
            completions = Objects.requireNonNull(provider);
            return this;
        }

        public Builder withSampling(SamplingProvider provider) {
            sampling = Objects.requireNonNull(provider);
            return this;
        }

        public Builder withToolAccess(ToolAccessPolicy policy) {
            toolAccess = Objects.requireNonNull(policy);
            return this;
        }

        public Builder withSamplingAccess(SamplingAccessPolicy policy) {
            samplingAccess = Objects.requireNonNull(policy);
            return this;
        }

        public Builder withPrivacyBoundary(ResourceAccessController controller) {
            privacyBoundary = Objects.requireNonNull(controller);
            return this;
        }

        public McpComponents build() {
            return new McpComponents(
                    Objects.requireNonNull(resources),
                    Objects.requireNonNull(tools),
                    Objects.requireNonNull(prompts),
                    Objects.requireNonNull(completions),
                    Objects.requireNonNull(sampling),
                    Objects.requireNonNull(toolAccess),
                    Objects.requireNonNull(samplingAccess),
                    Objects.requireNonNull(privacyBoundary));
        }
    }
}
