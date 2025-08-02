package com.amannmalik.mcp.content;

import com.amannmalik.mcp.annotations.Annotations;
import com.amannmalik.mcp.annotations.AnnotationsCodec;
import com.amannmalik.mcp.server.resources.ResourcesCodec;
import com.amannmalik.mcp.util.Base64Util;
import com.amannmalik.mcp.util.JsonUtil;
import jakarta.json.*;

import java.util.Set;

public final class ContentCodec {
    private ContentCodec() {
    }

    public static JsonObject toJsonObject(ContentBlock content) {
        JsonObjectBuilder b = Json.createObjectBuilder().add("type", content.type());
        if (content.annotations() != null && content.annotations() != Annotations.EMPTY) {
            b.add("annotations", AnnotationsCodec.toJsonObject(content.annotations()));
        }
        if (content._meta() != null) b.add("_meta", content._meta());
        switch (content) {
            case ContentBlock.Text t -> b.add("text", t.text());
            case ContentBlock.Image i -> b.add("data", Base64Util.encode(i.data()))
                    .add("mimeType", i.mimeType());
            case ContentBlock.Audio a -> b.add("data", Base64Util.encode(a.data()))
                    .add("mimeType", a.mimeType());
            case ContentBlock.ResourceLink l -> {
                JsonObject obj = ResourcesCodec.toJsonObject(l.resource());
                obj.forEach((k, v) -> {
                    if (!"_meta".equals(k)) b.add(k, v);
                });
                if (l.resource()._meta() != null) b.add("_meta", l.resource()._meta());
            }
            case ContentBlock.EmbeddedResource r -> b.add("resource", ResourcesCodec.toJsonObject(r.resource()));
        }
        return b.build();
    }

    public static ContentBlock toContentBlock(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        String type = obj.getString("type", null);
        if (type == null) throw new IllegalArgumentException("type required");
        JsonUtil.requireOnlyKeys(obj, switch (type) {
            case "text" -> Set.of("type", "text", "annotations", "_meta");
            case "image", "audio" -> Set.of("type", "data", "mimeType", "annotations", "_meta");
            case "resource" -> Set.of("type", "resource", "annotations", "_meta");
            case "resource_link" -> Set.of("type", "uri", "name", "title", "description", "mimeType", "size", "annotations", "_meta");
            default -> throw new IllegalArgumentException("unknown content type: " + type);
        });
        return switch (type) {
            case "text" -> new ContentBlock.Text(obj.getString("text"),
                    obj.containsKey("annotations") ? AnnotationsCodec.toAnnotations(obj.getJsonObject("annotations")) : null,
                    obj.getJsonObject("_meta"));
            case "image" -> new ContentBlock.Image(Base64Util.decode(obj.getString("data")),
                    obj.getString("mimeType"),
                    obj.containsKey("annotations") ? AnnotationsCodec.toAnnotations(obj.getJsonObject("annotations")) : null,
                    obj.getJsonObject("_meta"));
            case "audio" -> new ContentBlock.Audio(Base64Util.decode(obj.getString("data")),
                    obj.getString("mimeType"),
                    obj.containsKey("annotations") ? AnnotationsCodec.toAnnotations(obj.getJsonObject("annotations")) : null,
                    obj.getJsonObject("_meta"));
            case "resource" -> new ContentBlock.EmbeddedResource(
                    ResourcesCodec.toResourceBlock(obj.getJsonObject("resource")),
                    obj.containsKey("annotations") ? AnnotationsCodec.toAnnotations(obj.getJsonObject("annotations")) : null,
                    obj.getJsonObject("_meta"));
            case "resource_link" -> new ContentBlock.ResourceLink(ResourcesCodec.toResource(obj));
            default -> throw new IllegalArgumentException("unknown content type: " + type);
        };
    }
}
