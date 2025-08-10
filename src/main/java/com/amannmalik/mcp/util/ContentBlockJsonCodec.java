package com.amannmalik.mcp.util;

import com.amannmalik.mcp.api.ContentBlock;
import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.jsonrpc.JsonCodec;
import jakarta.json.*;

import java.util.Set;

public class ContentBlockJsonCodec implements JsonCodec<ContentBlock> {

    private final AnnotationsJsonCodec ANNOTATIONS_CODEC = new AnnotationsJsonCodec();
    private final ResourceBlockJsonCodec RESOURCE_BLOCK_CODEC = new ResourceBlockJsonCodec();
    private final ResourceAbstractEntityCodec RESOURCE_ENTITY_CODEC = new ResourceAbstractEntityCodec();

    @Override
    public JsonObject toJson(ContentBlock content) {
        JsonObjectBuilder b = Json.createObjectBuilder().add("type", content.type());
        if (content.annotations() != null && content.annotations() != AnnotationsJsonCodec.EMPTY) {
            b.add("annotations", ANNOTATIONS_CODEC.toJson(content.annotations()));
        }
        if (content._meta() != null) b.add("_meta", content._meta());
        return switch (content) {
            case ContentBlock.Text t -> b.add("text", t.text()).build();
            case ContentBlock.Image i -> b.add("data", Base64Util.encode(i.data()))
                    .add("mimeType", i.mimeType()).build();
            case ContentBlock.Audio a -> b.add("data", Base64Util.encode(a.data()))
                    .add("mimeType", a.mimeType()).build();
            case ContentBlock.ResourceLink l -> {
                JsonObject obj = RESOURCE_ENTITY_CODEC.toJson(l.resource());
                obj.forEach((k, v) -> {
                    if (!"_meta".equals(k)) b.add(k, v);
                });
                if (l.resource()._meta() != null) b.add("_meta", l.resource()._meta());
                yield b.build();
            }
            case ContentBlock.EmbeddedResource r -> b.add("resource", RESOURCE_BLOCK_CODEC.toJson(r.resource())).build();
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
            case "text" -> new ContentBlock.Text(
                    obj.getString("text"),
                    obj.containsKey("annotations") ? ANNOTATIONS_CODEC.fromJson(obj.getJsonObject("annotations")) : null,
                    obj.getJsonObject("_meta"));
            case "image" -> new ContentBlock.Image(
                    Base64Util.decode(obj.getString("data")),
                    obj.getString("mimeType"),
                    obj.containsKey("annotations") ? ANNOTATIONS_CODEC.fromJson(obj.getJsonObject("annotations")) : null,
                    obj.getJsonObject("_meta"));
            case "audio" -> new ContentBlock.Audio(
                    Base64Util.decode(obj.getString("data")),
                    obj.getString("mimeType"),
                    obj.containsKey("annotations") ? ANNOTATIONS_CODEC.fromJson(obj.getJsonObject("annotations")) : null,
                    obj.getJsonObject("_meta"));
            case "resource" -> new ContentBlock.EmbeddedResource(
                    RESOURCE_BLOCK_CODEC.fromJson(obj.getJsonObject("resource")),
                    obj.containsKey("annotations") ? ANNOTATIONS_CODEC.fromJson(obj.getJsonObject("annotations")) : null,
                    obj.getJsonObject("_meta"));
            case "resource_link" -> new ContentBlock.ResourceLink(RESOURCE_ENTITY_CODEC.fromJson(obj));
            default -> throw new IllegalArgumentException("unknown content type: " + type);
        };
    }
}
