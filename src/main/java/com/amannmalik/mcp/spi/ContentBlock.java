package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.core.ContentBlockContract;
import com.amannmalik.mcp.core.SpiPreconditions;
import jakarta.json.JsonObject;

public sealed interface ContentBlock permits
        ContentBlock.Text,
        ContentBlock.Image,
        ContentBlock.Audio,
        ContentBlock.ResourceLink,
        ContentBlock.EmbeddedResource {
    String type();

    Annotations annotations();

    JsonObject _meta();

    record Text(String text, Annotations annotations, JsonObject _meta)
            implements ContentBlock, PromptContent, MessageContent {
        public Text {
            text = ContentBlockContract.requireText(text);
            ContentBlockContract.requireMeta(_meta);
            annotations = ContentBlockContract.normalizeAnnotations(annotations);
        }

        @Override
        public String type() {
            return "text";
        }
    }

    record Image(byte[] data, String mimeType, Annotations annotations, JsonObject _meta)
            implements ContentBlock, PromptContent, MessageContent {
        public Image {
            data = ContentBlockContract.requireData(data);
            mimeType = ContentBlockContract.requireMimeType(mimeType);
            ContentBlockContract.requireMeta(_meta);
            annotations = ContentBlockContract.normalizeAnnotations(annotations);
        }

        @Override
        public String type() {
            return "image";
        }

        @Override
        public byte[] data() {
            return ContentBlockContract.copy(data);
        }
    }

    record Audio(byte[] data, String mimeType, Annotations annotations, JsonObject _meta)
            implements ContentBlock, PromptContent, MessageContent {
        public Audio {
            data = ContentBlockContract.requireData(data);
            mimeType = ContentBlockContract.requireMimeType(mimeType);
            ContentBlockContract.requireMeta(_meta);
            annotations = ContentBlockContract.normalizeAnnotations(annotations);
        }

        @Override
        public String type() {
            return "audio";
        }

        @Override
        public byte[] data() {
            return ContentBlockContract.copy(data);
        }
    }

    record ResourceLink(Resource resource)
            implements ContentBlock, PromptContent {
        public ResourceLink {
            SpiPreconditions.requireNonNull(resource, "resource is required");
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
            SpiPreconditions.requireNonNull(resource, "resource is required");
            ContentBlockContract.requireMeta(_meta);
            annotations = ContentBlockContract.normalizeAnnotations(annotations);
        }

        @Override
        public String type() {
            return "resource";
        }
    }
}
