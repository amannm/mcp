package com.amannmalik.mcp.spi;

public sealed interface PromptContent permits
        ContentBlock.Audio,
        ContentBlock.EmbeddedResource,
        ContentBlock.Image,
        ContentBlock.ResourceLink,
        ContentBlock.Text {
}
