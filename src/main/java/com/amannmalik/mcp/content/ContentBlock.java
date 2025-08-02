package com.amannmalik.mcp.content;

import com.amannmalik.mcp.annotations.Annotations;
import com.amannmalik.mcp.prompts.PromptContent;
import com.amannmalik.mcp.client.sampling.MessageContent;
import com.amannmalik.mcp.server.resources.Resource;
import com.amannmalik.mcp.server.resources.ResourceBlock;
import com.amannmalik.mcp.validation.InputSanitizer;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.JsonObject;

public sealed interface ContentBlock
        permits ContentBlock.Text, ContentBlock.Image, ContentBlock.Audio,
                ContentBlock.ResourceLink, ContentBlock.EmbeddedResource {
    String type();
    Annotations annotations();
    JsonObject _meta();

    record Text(String text, Annotations annotations, JsonObject _meta)
            implements ContentBlock, PromptContent, MessageContent {
        public Text {
            if (text == null) throw new IllegalArgumentException("text is required");
            text = InputSanitizer.requireClean(text);
            MetaValidator.requireValid(_meta);
            if (annotations == null) annotations = Annotations.EMPTY;
        }

        @Override
        public String type() {
            return "text";
        }
    }

    record Image(byte[] data, String mimeType, Annotations annotations, JsonObject _meta)
            implements ContentBlock, PromptContent, MessageContent {
        public Image {
            if (data == null || mimeType == null)
                throw new IllegalArgumentException("data and mimeType are required");
            data = data.clone();
            mimeType = InputSanitizer.requireClean(mimeType);
            MetaValidator.requireValid(_meta);
            if (annotations == null) annotations = Annotations.EMPTY;
        }

        @Override
        public String type() {
            return "image";
        }
    }

    record Audio(byte[] data, String mimeType, Annotations annotations, JsonObject _meta)
            implements ContentBlock, PromptContent, MessageContent {
        public Audio {
            if (data == null || mimeType == null)
                throw new IllegalArgumentException("data and mimeType are required");
            data = data.clone();
            mimeType = InputSanitizer.requireClean(mimeType);
            MetaValidator.requireValid(_meta);
            if (annotations == null) annotations = Annotations.EMPTY;
        }

        @Override
        public String type() {
            return "audio";
        }
    }

    record ResourceLink(Resource resource)
            implements ContentBlock, PromptContent {
        public ResourceLink {
            if (resource == null) throw new IllegalArgumentException("resource is required");
        }

        @Override
        public Annotations annotations() {
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

    record EmbeddedResource(ResourceBlock resource, Annotations annotations, JsonObject _meta)
            implements ContentBlock, PromptContent {
        public EmbeddedResource {
            if (resource == null) throw new IllegalArgumentException("resource is required");
            MetaValidator.requireValid(_meta);
            if (annotations == null) annotations = Annotations.EMPTY;
        }

        @Override
        public String type() {
            return "resource";
        }
    }
}
