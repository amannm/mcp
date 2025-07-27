package com.amannmalik.mcp.schema;

import java.util.Optional;

public sealed interface ContentBlock
        permits ContentBlock.TextContent,
                ContentBlock.ImageContent,
                ContentBlock.AudioContent,
                ContentBlock.ResourceLink,
                ContentBlock.EmbeddedResource {

    Optional<String> annotation();

    record TextContent(String text, Optional<String> annotation) implements ContentBlock {}

    record ImageContent(String uri, Optional<String> annotation) implements ContentBlock {}

    record AudioContent(String uri, Optional<String> annotation) implements ContentBlock {}

    record ResourceLink(String uri, Optional<String> annotation) implements ContentBlock {}

    record EmbeddedResource(String name, byte[] data, String mediaType, Optional<String> annotation)
            implements ContentBlock {}
}
