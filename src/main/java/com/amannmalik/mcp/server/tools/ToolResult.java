package com.amannmalik.mcp.server.tools;

import com.amannmalik.mcp.annotations.AnnotationsCodec;
import com.amannmalik.mcp.server.resources.Resource;
import com.amannmalik.mcp.server.resources.ResourceBlock;
import com.amannmalik.mcp.server.resources.ResourcesCodec;
import com.amannmalik.mcp.util.Base64Util;
import com.amannmalik.mcp.validation.InputSanitizer;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

public record ToolResult(JsonArray content,
                         JsonObject structuredContent,
                         Boolean isError,
                         JsonObject _meta) {
    public ToolResult {
        content = sanitize(content == null ? JsonValue.EMPTY_JSON_ARRAY : content);
        if (isError == null) isError = Boolean.FALSE;
        MetaValidator.requireValid(_meta);
    }

    private static JsonArray sanitize(JsonArray arr) {
        JsonArrayBuilder b = Json.createArrayBuilder();
        for (JsonValue v : arr) {
            if (v.getValueType() == JsonValue.ValueType.OBJECT) {
                JsonObject o = v.asJsonObject();
                String type = o.getString("type", null);
                if ("text".equals(type) && o.containsKey("text")) {
                    b.add(toText(o));
                    continue;
                }
                if ("image".equals(type) && o.containsKey("data") && o.containsKey("mimeType")) {
                    b.add(toImage(o));
                    continue;
                }
                if ("audio".equals(type) && o.containsKey("data") && o.containsKey("mimeType")) {
                    b.add(toAudio(o));
                    continue;
                }
                if ("resource_link".equals(type)) {
                    b.add(toResourceLink(o));
                    continue;
                }
                if ("resource".equals(type) && o.containsKey("resource")) {
                    b.add(toEmbeddedResource(o));
                    continue;
                }
            }
            b.add(v);
        }
        return b.build();
    }

    private static JsonObject toText(JsonObject obj) {
        JsonObjectBuilder result = Json.createObjectBuilder()
                .add("type", "text")
                .add("text", InputSanitizer.requireClean(obj.getString("text")));
        if (obj.containsKey("annotations")) {
            var ann = AnnotationsCodec.toAnnotations(obj.getJsonObject("annotations"));
            result.add("annotations", AnnotationsCodec.toJsonObject(ann));
        }
        if (obj.containsKey("_meta")) {
            MetaValidator.requireValid(obj.getJsonObject("_meta"));
            result.add("_meta", obj.getJsonObject("_meta"));
        }
        return result.build();
    }

    private static JsonObject toImage(JsonObject obj) {
        byte[] data = Base64Util.decode(obj.getString("data"));
        JsonObjectBuilder result = Json.createObjectBuilder()
                .add("type", "image")
                .add("data", Base64Util.encode(data))
                .add("mimeType", InputSanitizer.requireClean(obj.getString("mimeType")));
        if (obj.containsKey("annotations")) {
            var ann = AnnotationsCodec.toAnnotations(obj.getJsonObject("annotations"));
            result.add("annotations", AnnotationsCodec.toJsonObject(ann));
        }
        if (obj.containsKey("_meta")) {
            MetaValidator.requireValid(obj.getJsonObject("_meta"));
            result.add("_meta", obj.getJsonObject("_meta"));
        }
        return result.build();
    }

    private static JsonObject toAudio(JsonObject obj) {
        byte[] data = Base64Util.decode(obj.getString("data"));
        JsonObjectBuilder result = Json.createObjectBuilder()
                .add("type", "audio")
                .add("data", Base64Util.encode(data))
                .add("mimeType", InputSanitizer.requireClean(obj.getString("mimeType")));
        if (obj.containsKey("annotations")) {
            var ann = AnnotationsCodec.toAnnotations(obj.getJsonObject("annotations"));
            result.add("annotations", AnnotationsCodec.toJsonObject(ann));
        }
        if (obj.containsKey("_meta")) {
            MetaValidator.requireValid(obj.getJsonObject("_meta"));
            result.add("_meta", obj.getJsonObject("_meta"));
        }
        return result.build();
    }

    private static JsonObject toResourceLink(JsonObject obj) {
        Resource r = ResourcesCodec.toResource(obj);
        JsonObject base = ResourcesCodec.toJsonObject(r);
        JsonObjectBuilder result = Json.createObjectBuilder(base)
                .add("type", "resource_link");
        if (obj.containsKey("_meta")) {
            MetaValidator.requireValid(obj.getJsonObject("_meta"));
            result.add("_meta", obj.getJsonObject("_meta"));
        }
        return result.build();
    }

    private static JsonObject toEmbeddedResource(JsonObject obj) {
        JsonObject resourceObj = obj.getJsonObject("resource");
        if (resourceObj == null) throw new IllegalArgumentException("resource required");
        ResourceBlock block = ResourcesCodec.toResourceBlock(resourceObj);
        JsonObjectBuilder result = Json.createObjectBuilder()
                .add("type", "resource")
                .add("resource", ResourcesCodec.toJsonObject(block));
        if (obj.containsKey("annotations")) {
            var ann = AnnotationsCodec.toAnnotations(obj.getJsonObject("annotations"));
            result.add("annotations", AnnotationsCodec.toJsonObject(ann));
        }
        if (obj.containsKey("_meta")) {
            MetaValidator.requireValid(obj.getJsonObject("_meta"));
            result.add("_meta", obj.getJsonObject("_meta"));
        }
        return result.build();
    }

}
