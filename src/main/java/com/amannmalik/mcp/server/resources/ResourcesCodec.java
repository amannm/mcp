package com.amannmalik.mcp.server.resources;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Set;

public final class ResourcesCodec {
    private ResourcesCodec() {
    }

    public static JsonObject toJsonObject(Resource r) {
        JsonObjectBuilder b = Json.createObjectBuilder()
                .add("uri", r.uri())
                .add("name", r.name());
        if (r.title() != null) b.add("title", r.title());
        if (r.description() != null) b.add("description", r.description());
        if (r.mimeType() != null) b.add("mimeType", r.mimeType());
        if (r.size() != null) b.add("size", r.size());
        if (r.annotations() != null) b.add("annotations", toJsonObject(r.annotations()));
        if (r._meta() != null) b.add("_meta", r._meta());
        return b.build();
    }

    public static Resource toResource(JsonObject obj) {
        return new Resource(
                obj.getString("uri"),
                obj.getString("name"),
                obj.getString("title", null),
                obj.getString("description", null),
                obj.getString("mimeType", null),
                obj.containsKey("size") ? obj.getJsonNumber("size").longValue() : null,
                obj.containsKey("annotations") ? toAnnotations(obj.getJsonObject("annotations")) : null,
                obj.containsKey("_meta") ? obj.getJsonObject("_meta") : null
        );
    }

    public static JsonObject toJsonObject(ResourceTemplate t) {
        JsonObjectBuilder b = Json.createObjectBuilder()
                .add("uriTemplate", t.uriTemplate())
                .add("name", t.name());
        if (t.title() != null) b.add("title", t.title());
        if (t.description() != null) b.add("description", t.description());
        if (t.mimeType() != null) b.add("mimeType", t.mimeType());
        if (t.annotations() != null) b.add("annotations", toJsonObject(t.annotations()));
        if (t._meta() != null) b.add("_meta", t._meta());
        return b.build();
    }

    public static ResourceTemplate toResourceTemplate(JsonObject obj) {
        return new ResourceTemplate(
                obj.getString("uriTemplate"),
                obj.getString("name"),
                obj.getString("title", null),
                obj.getString("description", null),
                obj.getString("mimeType", null),
                obj.containsKey("annotations") ? toAnnotations(obj.getJsonObject("annotations")) : null,
                obj.containsKey("_meta") ? obj.getJsonObject("_meta") : null
        );
    }

    public static JsonObject toJsonObject(ResourceBlock block) {
        JsonObjectBuilder b = Json.createObjectBuilder()
                .add("uri", block.uri())
                .add("name", block.name());
        if (block.title() != null) b.add("title", block.title());
        if (block.mimeType() != null) b.add("mimeType", block.mimeType());
        if (block.annotations() != null) b.add("annotations", toJsonObject(block.annotations()));
        if (block._meta() != null) b.add("_meta", block._meta());
        switch (block) {
            case ResourceBlock.Text t -> b.add("text", t.text());
            case ResourceBlock.Binary b2 -> b.add("blob", Base64.getEncoder().encodeToString(b2.blob()));
        }
        return b.build();
    }

    public static ResourceBlock toResourceBlock(JsonObject obj) {
        String uri = obj.getString("uri");
        String name = obj.getString("name");
        String title = obj.getString("title", null);
        String mime = obj.getString("mimeType", null);
        ResourceAnnotations ann = obj.containsKey("annotations") ? toAnnotations(obj.getJsonObject("annotations")) : null;
        JsonObject meta = obj.containsKey("_meta") ? obj.getJsonObject("_meta") : null;
        if (obj.containsKey("text")) {
            return new ResourceBlock.Text(uri, name, title, mime, obj.getString("text"), ann, meta);
        }
        if (obj.containsKey("blob")) {
            byte[] data = Base64.getDecoder().decode(obj.getString("blob"));
            return new ResourceBlock.Binary(uri, name, title, mime, data, ann, meta);
        }
        throw new IllegalArgumentException("unknown content block");
    }

    public static JsonObject toJsonObject(ResourceAnnotations ann) {
        JsonObjectBuilder b = Json.createObjectBuilder();
        if (!ann.audience().isEmpty()) {
            var arr = Json.createArrayBuilder();
            ann.audience().forEach(a -> arr.add(a.name().toLowerCase()));
            b.add("audience", arr);
        }
        if (ann.priority() != null) b.add("priority", ann.priority());
        if (ann.lastModified() != null) b.add("lastModified", ann.lastModified().toString());
        return b.build();
    }

    private static ResourceAnnotations toAnnotations(JsonObject obj) {
        Set<Audience> audience = EnumSet.noneOf(Audience.class);
        var audienceArr = obj.getJsonArray("audience");
        if (audienceArr != null) {
            audienceArr.getValuesAs(JsonString.class)
                    .forEach(js -> audience.add(Audience.valueOf(js.getString().toUpperCase())));
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
        return new ResourceAnnotations(audience.isEmpty() ? Set.of() : EnumSet.copyOf(audience), priority, lastModified);
    }

    public static JsonObject toJsonObject(ResourceListChangedNotification n) {
        if (n == null) throw new IllegalArgumentException("notification required");
        return Json.createObjectBuilder().build();
    }

    public static ResourceListChangedNotification toResourceListChangedNotification(JsonObject obj) {
        if (obj != null && !obj.isEmpty()) {
            throw new IllegalArgumentException("unexpected fields");
        }
        return new ResourceListChangedNotification();
    }

    public static JsonObject toJsonObject(ResourceUpdatedNotification n) {
        JsonObjectBuilder b = Json.createObjectBuilder()
                .add("uri", n.uri());
        if (n.title() != null) b.add("title", n.title());
        return b.build();
    }

    public static ResourceUpdatedNotification toResourceUpdatedNotification(JsonObject obj) {
        return new ResourceUpdatedNotification(
                obj.getString("uri"),
                obj.getString("title", null)
        );
    }
}
