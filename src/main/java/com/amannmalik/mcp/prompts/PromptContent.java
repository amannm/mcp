package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.server.resources.Resource;
import com.amannmalik.mcp.server.resources.ResourceAnnotations;
import com.amannmalik.mcp.server.resources.ResourceBlock;
import com.amannmalik.mcp.validation.InputSanitizer;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.JsonObject;

public sealed interface PromptContent
        permits PromptContent.Text,
        PromptContent.Image,
        PromptContent.Audio,
        PromptContent.EmbeddedResource,
        PromptContent.ResourceLink {
    String type();

    ResourceAnnotations annotations();

    JsonObject _meta();

    record Text(String text, ResourceAnnotations annotations, JsonObject _meta) implements PromptContent {
        public Text {
            if (text == null) throw new IllegalArgumentException("text is required");
            text = InputSanitizer.requireClean(text);
            MetaValidator.requireValid(_meta);
        }

        @Override
        public String type() {
            return "text";
        }
    }

    record Image(byte[] data, String mimeType, ResourceAnnotations annotations, JsonObject _meta) implements PromptContent {
        public Image {
            if (data == null || mimeType == null) {
                throw new IllegalArgumentException("data and mimeType are required");
            }
            data = data.clone();
            mimeType = InputSanitizer.requireClean(mimeType);
            MetaValidator.requireValid(_meta);
        }

        @Override
        public String type() {
            return "image";
        }
    }

    record Audio(byte[] data, String mimeType, ResourceAnnotations annotations, JsonObject _meta) implements PromptContent {
        public Audio {
            if (data == null || mimeType == null) {
                throw new IllegalArgumentException("data and mimeType are required");
            }
            data = data.clone();
            mimeType = InputSanitizer.requireClean(mimeType);
            MetaValidator.requireValid(_meta);
        }

        @Override
        public String type() {
            return "audio";
        }
    }

    record EmbeddedResource(ResourceBlock resource, ResourceAnnotations annotations, JsonObject _meta) implements PromptContent {
        public EmbeddedResource {
            if (resource == null) throw new IllegalArgumentException("resource is required");
            MetaValidator.requireValid(_meta);
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
        public JsonObject _meta() {
            return resource._meta();
        }

        @Override
        public String type() {
            return "resource_link";
        }
    }
}
