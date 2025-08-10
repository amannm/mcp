package com.amannmalik.mcp.api;

public sealed interface PromptContent permits ContentBlock.Audio, ContentBlock.EmbeddedResource, ContentBlock.Image, ContentBlock.ResourceLink, ContentBlock.Text {
}
