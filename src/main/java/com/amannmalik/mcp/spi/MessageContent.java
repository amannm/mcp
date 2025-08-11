package com.amannmalik.mcp.spi;

/**
 * Represents content that can appear in a {@link SamplingMessage}.
 *
 * <p>Only text, image, and audio blocks are valid message content as defined by the
 * <a href="specification/2025-06-18/index.mdx">MCP specification</a>.
 * Resource links and embedded resources are intentionally excluded to ensure message
 * payloads remain self-contained and serializable without additional context.</p>
 */
public sealed interface MessageContent permits
        ContentBlock.Text,
        ContentBlock.Image,
        ContentBlock.Audio {
}
