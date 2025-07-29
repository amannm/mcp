package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.server.resources.Resource;
import com.amannmalik.mcp.server.resources.ResourceAnnotations;
import com.amannmalik.mcp.server.resources.ResourceBlock;

/** Supported content types for prompt messages. */
public sealed interface PromptContent
        permits PromptContent.Text,
        PromptContent.Image,
        PromptContent.Audio,
        PromptContent.EmbeddedResource,
        PromptContent.ResourceLink {
    String type();
    ResourceAnnotations annotations();

    /** Text content. */
    record Text(String text, ResourceAnnotations annotations) implements PromptContent {
        public Text {
            if (text == null) throw new IllegalArgumentException("text is required");
        }
        @Override public String type() { return "text"; }
    }

    /** Image content. */
    record Image(byte[] data, String mimeType, ResourceAnnotations annotations) implements PromptContent {
        public Image {
            if (data == null || mimeType == null) {
                throw new IllegalArgumentException("data and mimeType are required");
            }
        }
        @Override public String type() { return "image"; }
    }

    /** Audio content. */
    record Audio(byte[] data, String mimeType, ResourceAnnotations annotations) implements PromptContent {
        public Audio {
            if (data == null || mimeType == null) {
                throw new IllegalArgumentException("data and mimeType are required");
            }
        }
        @Override public String type() { return "audio"; }
    }

    /** Embedded resource content. */
    record EmbeddedResource(ResourceBlock resource, ResourceAnnotations annotations) implements PromptContent {
        public EmbeddedResource {
            if (resource == null) throw new IllegalArgumentException("resource is required");
        }
        @Override public String type() { return "resource"; }
    }

    /** Link to a resource without embedding its contents. */
    record ResourceLink(Resource resource) implements PromptContent {
        public ResourceLink {
            if (resource == null) throw new IllegalArgumentException("resource is required");
        }

        @Override public ResourceAnnotations annotations() {
            return resource.annotations();
        }

        @Override public String type() { return "resource_link"; }
    }
}
