package com.amannmalik.mcp.schema.content;

sealed public interface ContentBlock permits TextContent, ImageContent, AudioContent, ResourceLink, EmbeddedResource {}
