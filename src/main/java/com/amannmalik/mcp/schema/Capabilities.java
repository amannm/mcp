package com.amannmalik.mcp.schema;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Capability negotiation structures.
 */
public final class Capabilities {
    private Capabilities() {}

    public sealed interface ClientCapability
            permits RootsCapability, SamplingCapability {}

    public sealed interface ServerCapability
            permits PromptsCapability, ResourcesCapability, ToolsCapability, LoggingCapability {}

    public record RootsCapability() implements ClientCapability {}
    public record SamplingCapability() implements ClientCapability {}
    public record PromptsCapability() implements ServerCapability {}
    public record ResourcesCapability() implements ServerCapability {}
    public record ToolsCapability() implements ServerCapability {}
    public record LoggingCapability() implements ServerCapability {}

    public record ClientCapabilities(Optional<RootsCapability> roots,
                                     Optional<SamplingCapability> sampling,
                                     Optional<Map<String, Object>> experimental) {
        public ClientCapabilities {
            Objects.requireNonNull(roots);
            Objects.requireNonNull(sampling);
            Objects.requireNonNull(experimental);
        }
    }

    public record ServerCapabilities(Optional<PromptsCapability> prompts,
                                     Optional<ResourcesCapability> resources,
                                     Optional<ToolsCapability> tools,
                                     Optional<LoggingCapability> logging,
                                     Optional<Map<String, Object>> experimental) {
        public ServerCapabilities {
            Objects.requireNonNull(prompts);
            Objects.requireNonNull(resources);
            Objects.requireNonNull(tools);
            Objects.requireNonNull(logging);
            Objects.requireNonNull(experimental);
        }
    }
}
