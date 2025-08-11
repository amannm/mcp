package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.codec.AnnotationsJsonCodec;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public sealed interface ContentBlock permits
        ContentBlock.Text,
        ContentBlock.Image,
        ContentBlock.Audio,
        ContentBlock.ResourceLink,
        ContentBlock.EmbeddedResource {
    private static Annotations orEmpty(Annotations annotations) {
        return annotations == null ? AnnotationsJsonCodec.EMPTY : annotations;
    }

    private static void validateMeta(JsonObject meta) {
        ValidationUtil.requireMeta(meta);
    }

    private static byte[] requireData(byte[] data) {
        if (data == null) throw new IllegalArgumentException("data is required");
        return data.clone();
    }

    private static String requireMimeType(String mimeType) {
        if (mimeType == null) throw new IllegalArgumentException("mimeType is required");
        return ValidationUtil.requireClean(mimeType);
    }

    String type();

    Annotations annotations();

    JsonObject _meta();

    record Text(String text, Annotations annotations, JsonObject _meta)
            implements ContentBlock, PromptContent, MessageContent {
        public Text {
            if (text == null) throw new IllegalArgumentException("text is required");
            text = ValidationUtil.requireClean(text);
            validateMeta(_meta);
            annotations = orEmpty(annotations);
        }

        @Override
        public String type() {
            return "text";
        }
    }

    record Image(byte[] data, String mimeType, Annotations annotations, JsonObject _meta)
            implements ContentBlock, PromptContent, MessageContent {
        public Image {
            data = requireData(data);
            mimeType = requireMimeType(mimeType);
            validateMeta(_meta);
            annotations = orEmpty(annotations);
        }

        @Override
        public String type() {
            return "image";
        }
    }

    record Audio(byte[] data, String mimeType, Annotations annotations, JsonObject _meta)
            implements ContentBlock, PromptContent, MessageContent {
        public Audio {
            data = requireData(data);
            mimeType = requireMimeType(mimeType);
            validateMeta(_meta);
            annotations = orEmpty(annotations);
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
            validateMeta(_meta);
            annotations = orEmpty(annotations);
        }

        @Override
        public String type() {
            return "resource";
        }
    }
}
