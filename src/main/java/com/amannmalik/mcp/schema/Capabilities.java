package com.amannmalik.mcp.schema;

import java.util.Optional;

/** Basic capability holder types. Actual capability contents are defined in later phases. */
public final class Capabilities {
    private Capabilities() {}

    public record ClientCapabilities(Optional<RootsCapability> roots,
                                     Optional<SamplingCapability> sampling) {}

    public record ServerCapabilities(Optional<PromptsCapability> prompts,
                                     Optional<ResourcesCapability> resources) {}

    public sealed interface RootsCapability permits BasicRootsCapability {}
    public sealed interface SamplingCapability permits BasicSamplingCapability {}
    public sealed interface PromptsCapability permits BasicPromptsCapability {}
    public sealed interface ResourcesCapability permits BasicResourcesCapability {}

    public record BasicRootsCapability() implements RootsCapability {}
    public record BasicSamplingCapability() implements SamplingCapability {}
    public record BasicPromptsCapability() implements PromptsCapability {}
    public record BasicResourcesCapability() implements ResourcesCapability {}
}
