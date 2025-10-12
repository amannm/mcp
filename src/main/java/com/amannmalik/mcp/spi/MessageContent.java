package com.amannmalik.mcp.spi;

public sealed interface MessageContent permits
        ContentBlock.Text,
        ContentBlock.Image,
        ContentBlock.Audio {
}
