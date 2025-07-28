package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.server.resources.Resource;

/** Supported content types for prompt messages. */
public sealed interface PromptContent
        permits PromptContent.Text, PromptContent.Image, PromptContent.Audio, PromptContent.ResourceContent {
    String type();

    /** Text content. */
    record Text(String text) implements PromptContent {
        public Text {
            if (text == null) throw new IllegalArgumentException("text is required");
        }
        @Override public String type() { return "text"; }
    }

    /** Image content. */
    record Image(byte[] data, String mimeType) implements PromptContent {
        public Image {
            if (data == null || mimeType == null) {
                throw new IllegalArgumentException("data and mimeType are required");
            }
        }
        @Override public String type() { return "image"; }
    }

    /** Audio content. */
    record Audio(byte[] data, String mimeType) implements PromptContent {
        public Audio {
            if (data == null || mimeType == null) {
                throw new IllegalArgumentException("data and mimeType are required");
            }
        }
        @Override public String type() { return "audio"; }
    }

    /** Embedded resource content. */
    record ResourceContent(Resource resource) implements PromptContent {
        public ResourceContent {
            if (resource == null) throw new IllegalArgumentException("resource is required");
        }
        @Override public String type() { return "resource"; }
    }
}
