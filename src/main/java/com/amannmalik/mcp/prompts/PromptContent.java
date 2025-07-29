package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.server.resources.Resource;
import com.amannmalik.mcp.server.resources.ResourceAnnotations;
import com.amannmalik.mcp.server.resources.ResourceBlock;
import com.amannmalik.mcp.validation.InputSanitizer;

public sealed interface PromptContent
        permits PromptContent.Text,
        PromptContent.Image,
        PromptContent.Audio,
        PromptContent.EmbeddedResource,
        PromptContent.ResourceLink {
    String type();

    ResourceAnnotations annotations();

    record Text(String text, ResourceAnnotations annotations) implements PromptContent {
        public Text {
            if (text == null) throw new IllegalArgumentException("text is required");
            text = InputSanitizer.requireClean(text);
        }

        @Override
        public String type() {
            return "text";
        }
    }

    record Image(byte[] data, String mimeType, ResourceAnnotations annotations) implements PromptContent {
        public Image {
            if (data == null || mimeType == null) {
                throw new IllegalArgumentException("data and mimeType are required");
            }
            data = data.clone();
            mimeType = InputSanitizer.requireClean(mimeType);
        }

        @Override
        public String type() {
            return "image";
        }
    }

    record Audio(byte[] data, String mimeType, ResourceAnnotations annotations) implements PromptContent {
        public Audio {
            if (data == null || mimeType == null) {
                throw new IllegalArgumentException("data and mimeType are required");
            }
            data = data.clone();
            mimeType = InputSanitizer.requireClean(mimeType);
        }

        @Override
        public String type() {
            return "audio";
        }
    }

    record EmbeddedResource(ResourceBlock resource, ResourceAnnotations annotations) implements PromptContent {
        public EmbeddedResource {
            if (resource == null) throw new IllegalArgumentException("resource is required");
        }

        @Override
        public String type() {
            return "resource";
        }
    }

    record ResourceLink(Resource resource) implements PromptContent {
        public ResourceLink {
            if (resource == null) throw new IllegalArgumentException("resource is required");
        }

        @Override
        public ResourceAnnotations annotations() {
            return resource.annotations();
        }

        @Override
        public String type() {
            return "resource_link";
        }
    }
}
