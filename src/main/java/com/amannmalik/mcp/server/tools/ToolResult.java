package com.amannmalik.mcp.server.tools;

import com.amannmalik.mcp.annotations.Annotations;
import com.amannmalik.mcp.prompts.Role;
import com.amannmalik.mcp.server.resources.Resource;
import com.amannmalik.mcp.server.resources.ResourceBlock;
import com.amannmalik.mcp.server.resources.ResourcesCodec;
import com.amannmalik.mcp.validation.InputSanitizer;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Set;

public record ToolResult(JsonArray content,
                         JsonObject structuredContent,
                         boolean isError,
                         JsonObject _meta) {
    public ToolResult {
        content = sanitize(content == null ? JsonValue.EMPTY_JSON_ARRAY : content);
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
            result.add("annotations", ResourcesCodec.toJsonObject(toAnnotations(obj.getJsonObject("annotations"))));
        }
        if (obj.containsKey("_meta")) {
            MetaValidator.requireValid(obj.getJsonObject("_meta"));
            result.add("_meta", obj.getJsonObject("_meta"));
        }
        return result.build();
    }

    private static JsonObject toImage(JsonObject obj) {
        byte[] data = decodeBase64(obj.getString("data"));
        JsonObjectBuilder result = Json.createObjectBuilder()
                .add("type", "image")
                .add("data", Base64.getEncoder().encodeToString(data))
                .add("mimeType", InputSanitizer.requireClean(obj.getString("mimeType")));
        if (obj.containsKey("annotations")) {
            result.add("annotations", ResourcesCodec.toJsonObject(toAnnotations(obj.getJsonObject("annotations"))));
        }
        if (obj.containsKey("_meta")) {
            MetaValidator.requireValid(obj.getJsonObject("_meta"));
            result.add("_meta", obj.getJsonObject("_meta"));
        }
        return result.build();
    }

    private static JsonObject toAudio(JsonObject obj) {
        byte[] data = decodeBase64(obj.getString("data"));
        JsonObjectBuilder result = Json.createObjectBuilder()
                .add("type", "audio")
                .add("data", Base64.getEncoder().encodeToString(data))
                .add("mimeType", InputSanitizer.requireClean(obj.getString("mimeType")));
        if (obj.containsKey("annotations")) {
            result.add("annotations", ResourcesCodec.toJsonObject(toAnnotations(obj.getJsonObject("annotations"))));
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
        if (obj.containsKey("annotations")) result.add("annotations", obj.getJsonObject("annotations"));
        if (obj.containsKey("_meta")) {
            MetaValidator.requireValid(obj.getJsonObject("_meta"));
            result.add("_meta", obj.getJsonObject("_meta"));
        }
        return result.build();
    }

    private static byte[] decodeBase64(String value) {
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid base64 data", e);
        }
    }

    private static Annotations toAnnotations(JsonObject obj) {
        if (obj == null) return null;
        Set<Role> audience = EnumSet.noneOf(Role.class);
        JsonArray arr = obj.getJsonArray("audience");
        if (arr != null) {
            arr.getValuesAs(JsonString.class)
                    .forEach(js -> audience.add(Role.valueOf(js.getString().toUpperCase())));
        }
        Double priority = obj.containsKey("priority") ? obj.getJsonNumber("priority").doubleValue() : null;
        Instant lastModified = null;
        if (obj.containsKey("lastModified")) {
            try {
                lastModified = Instant.parse(obj.getString("lastModified"));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid lastModified", e);
            }
        }
        return new Annotations(audience.isEmpty() ? Set.of() : EnumSet.copyOf(audience), priority, lastModified);
    }
}
