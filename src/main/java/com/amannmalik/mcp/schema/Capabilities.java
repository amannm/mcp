package com.amannmalik.mcp.schema;

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
            permits PromptsCapability, ResourcesCapability {}

    public record RootsCapability() implements ClientCapability {}
    public record SamplingCapability() implements ClientCapability {}
    public record PromptsCapability() implements ServerCapability {}
    public record ResourcesCapability() implements ServerCapability {}

    public record ClientCapabilities(Optional<RootsCapability> roots,
                                     Optional<SamplingCapability> sampling) {
        public ClientCapabilities {
            Objects.requireNonNull(roots);
            Objects.requireNonNull(sampling);
        }
    }

    public record ServerCapabilities(Optional<PromptsCapability> prompts,
                                     Optional<ResourcesCapability> resources) {
        public ServerCapabilities {
            Objects.requireNonNull(prompts);
            Objects.requireNonNull(resources);
        }
    }
}
