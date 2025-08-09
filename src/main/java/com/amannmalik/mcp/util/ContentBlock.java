package com.amannmalik.mcp.util;

import com.amannmalik.mcp.annotations.Annotations;
import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.prompts.PromptContent;
import com.amannmalik.mcp.resources.Resource;
import com.amannmalik.mcp.resources.ResourceBlock;
import com.amannmalik.mcp.sampling.MessageContent;
import com.amannmalik.mcp.validation.ValidationUtil;
import jakarta.json.*;

import java.util.Set;

public sealed interface ContentBlock
        permits ContentBlock.Text, ContentBlock.Image, ContentBlock.Audio,
        ContentBlock.ResourceLink, ContentBlock.EmbeddedResource {
    String type();

    Annotations annotations();

    JsonObject _meta();

    JsonCodec<ContentBlock> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(ContentBlock content) {
            JsonObjectBuilder b = Json.createObjectBuilder().add("type", content.type());
            if (content.annotations() != null && content.annotations() != Annotations.EMPTY) {
                b.add("annotations", Annotations.CODEC.toJson(content.annotations()));
            }
            if (content._meta() != null) b.add("_meta", content._meta());
            return switch (content) {
                case Text t -> b.add("text", t.text()).build();
                case Image i -> b.add("data", Base64Util.encode(i.data()))
                        .add("mimeType", i.mimeType()).build();
                case Audio a -> b.add("data", Base64Util.encode(a.data()))
                        .add("mimeType", a.mimeType()).build();
                case ResourceLink l -> {
                    JsonObject obj = Resource.CODEC.toJson(l.resource());
                    obj.forEach((k, v) -> {
                        if (!"_meta".equals(k)) b.add(k, v);
                    });
                    if (l.resource()._meta() != null) b.add("_meta", l.resource()._meta());
                    yield b.build();
                }
                case EmbeddedResource r -> b.add("resource", ResourceBlock.CODEC.toJson(r.resource())).build();
            };
        }

        @Override
        public ContentBlock fromJson(JsonObject obj) {
            if (obj == null) throw new IllegalArgumentException("object required");
            String type = obj.getString("type", null);
            if (type == null) throw new IllegalArgumentException("type required");
            AbstractEntityCodec.requireOnlyKeys(obj, switch (type) {
                case "text" -> Set.of("type", "text", "annotations", "_meta");
                case "image", "audio" -> Set.of("type", "data", "mimeType", "annotations", "_meta");
                case "resource" -> Set.of("type", "resource", "annotations", "_meta");
                case "resource_link" -> Set.of("type", "uri", "name", "title", "description", "mimeType", "size", "annotations", "_meta");
                default -> throw new IllegalArgumentException("unknown content type: " + type);
            });
            return switch (type) {
                case "text" -> new Text(
                        obj.getString("text"),
                        obj.containsKey("annotations") ? Annotations.CODEC.fromJson(obj.getJsonObject("annotations")) : null,
                        obj.getJsonObject("_meta"));
                case "image" -> new Image(
                        Base64Util.decode(obj.getString("data")),
                        obj.getString("mimeType"),
                        obj.containsKey("annotations") ? Annotations.CODEC.fromJson(obj.getJsonObject("annotations")) : null,
                        obj.getJsonObject("_meta"));
                case "audio" -> new Audio(
                        Base64Util.decode(obj.getString("data")),
                        obj.getString("mimeType"),
                        obj.containsKey("annotations") ? Annotations.CODEC.fromJson(obj.getJsonObject("annotations")) : null,
                        obj.getJsonObject("_meta"));
                case "resource" -> new EmbeddedResource(
                        ResourceBlock.CODEC.fromJson(obj.getJsonObject("resource")),
                        obj.containsKey("annotations") ? Annotations.CODEC.fromJson(obj.getJsonObject("annotations")) : null,
                        obj.getJsonObject("_meta"));
                case "resource_link" -> new ResourceLink(Resource.CODEC.fromJson(obj));
                default -> throw new IllegalArgumentException("unknown content type: " + type);
            };
        }
    };

    private static Annotations orEmpty(Annotations annotations) {
        return annotations == null ? Annotations.EMPTY : annotations;
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
