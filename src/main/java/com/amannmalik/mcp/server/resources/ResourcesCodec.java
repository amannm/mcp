package com.amannmalik.mcp.server.resources;

import com.amannmalik.mcp.prompts.Role;
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
                .add("uri", block.uri());
        if (block.mimeType() != null) b.add("mimeType", block.mimeType());
        if (block._meta() != null) b.add("_meta", block._meta());
        switch (block) {
            case ResourceBlock.Text t -> b.add("text", t.text());
            case ResourceBlock.Binary b2 -> b.add("blob", Base64.getEncoder().encodeToString(b2.blob()));
        }
        return b.build();
    }

    public static ResourceBlock toResourceBlock(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        String uri = obj.getString("uri", null);
        if (uri == null) throw new IllegalArgumentException("uri required");
        String mime = obj.getString("mimeType", null);
        JsonObject meta = obj.containsKey("_meta") ? obj.getJsonObject("_meta") : null;

        if (obj.containsKey("annotations")) {
            throw new IllegalArgumentException("annotations not allowed");
        }

        boolean hasText = obj.containsKey("text");
        boolean hasBlob = obj.containsKey("blob");
        if (hasText == hasBlob) {
            throw new IllegalArgumentException("exactly one of text or blob must be present");
        }

        for (String key : obj.keySet()) {
            if (!Set.of("uri", "mimeType", "_meta", "text", "blob").contains(key)) {
                throw new IllegalArgumentException("unexpected field: " + key);
            }
        }

        if (hasText) {
            return new ResourceBlock.Text(uri, mime, obj.getString("text"), null, meta);
        }

        byte[] data = Base64.getDecoder().decode(obj.getString("blob"));
        return new ResourceBlock.Binary(uri, mime, data, null, meta);
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

    public static ResourceAnnotations toAnnotations(JsonObject obj) {
        Set<Role> audience = EnumSet.noneOf(Role.class);
        var audienceArr = obj.getJsonArray("audience");
        if (audienceArr != null) {
            audienceArr.getValuesAs(JsonString.class)
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
        if (n == null) throw new IllegalArgumentException("notification required");
        return Json.createObjectBuilder()
                .add("uri", n.uri())
                .build();
    }

    public static JsonObject toJsonObject(SubscribeRequest req) {
        if (req == null) throw new IllegalArgumentException("request required");
        return Json.createObjectBuilder()
                .add("uri", req.uri())
                .build();
    }

    public static SubscribeRequest toSubscribeRequest(JsonObject obj) {
        if (obj == null || !obj.containsKey("uri")) {
            throw new IllegalArgumentException("uri required");
        }
        return new SubscribeRequest(obj.getString("uri"));
    }

    public static JsonObject toJsonObject(UnsubscribeRequest req) {
        if (req == null) throw new IllegalArgumentException("request required");
        return Json.createObjectBuilder()
                .add("uri", req.uri())
                .build();
    }

    public static UnsubscribeRequest toUnsubscribeRequest(JsonObject obj) {
        if (obj == null || !obj.containsKey("uri")) {
            throw new IllegalArgumentException("uri required");
        }
        return new UnsubscribeRequest(obj.getString("uri"));
    }


    public static JsonObject toJsonObject(ReadResourceRequest req) {
        if (req == null) throw new IllegalArgumentException("request required");
        return Json.createObjectBuilder().add("uri", req.uri()).build();
    }

    public static ReadResourceRequest toReadResourceRequest(JsonObject obj) {
        if (obj == null || !obj.containsKey("uri")) {
            throw new IllegalArgumentException("uri required");
        }
        return new ReadResourceRequest(obj.getString("uri"));
    }

    public static JsonObject toJsonObject(ReadResourceResult result) {
        var arr = Json.createArrayBuilder();
        result.contents().forEach(c -> arr.add(toJsonObject(c)));
        return Json.createObjectBuilder().add("contents", arr.build()).build();
    }

    public static ReadResourceResult toReadResourceResult(JsonObject obj) {
        var arr = obj.getJsonArray("contents");
        if (arr == null) throw new IllegalArgumentException("contents required");
        java.util.List<ResourceBlock> list = new java.util.ArrayList<>();
        arr.forEach(v -> list.add(toResourceBlock(v.asJsonObject())));
        return new ReadResourceResult(list);
    }

    public static ResourceUpdatedNotification toResourceUpdatedNotification(JsonObject obj) {
        if (obj == null || !obj.containsKey("uri")) {
            throw new IllegalArgumentException("uri required");
        }
        if (obj.size() != 1) {
            throw new IllegalArgumentException("unexpected fields");
        }
        return new ResourceUpdatedNotification(obj.getString("uri"));
    }


    public static JsonObject toJsonObject(ListResourcesResult result) {
        if (result == null) throw new IllegalArgumentException("result required");
        var arr = Json.createArrayBuilder();
        result.resources().forEach(r -> arr.add(toJsonObject(r)));
        JsonObjectBuilder b = Json.createObjectBuilder().add("resources", arr.build());
        if (result.nextCursor() != null) b.add("nextCursor", result.nextCursor());
        return b.build();
    }

    public static ListResourcesResult toListResourcesResult(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        var arr = obj.getJsonArray("resources");
        if (arr == null) throw new IllegalArgumentException("resources required");
        java.util.List<Resource> resources = new java.util.ArrayList<>();
        arr.forEach(v -> resources.add(toResource(v.asJsonObject())));
        return new ListResourcesResult(resources, obj.getString("nextCursor", null));
    }

    public static JsonObject toJsonObject(ListResourceTemplatesRequest req) {
        if (req == null) throw new IllegalArgumentException("request required");
        JsonObjectBuilder b = Json.createObjectBuilder();
        if (req.cursor() != null) b.add("cursor", req.cursor());
        return b.build();
    }

    public static ListResourceTemplatesRequest toListResourceTemplatesRequest(JsonObject obj) {
        return new ListResourceTemplatesRequest(obj.getString("cursor", null));
    }

    public static JsonObject toJsonObject(ListResourceTemplatesResult result) {
        if (result == null) throw new IllegalArgumentException("result required");
        var arr = Json.createArrayBuilder();
        result.resourceTemplates().forEach(t -> arr.add(toJsonObject(t)));
        JsonObjectBuilder b = Json.createObjectBuilder().add("resourceTemplates", arr.build());
        if (result.nextCursor() != null) b.add("nextCursor", result.nextCursor());
        return b.build();
    }

    public static ListResourceTemplatesResult toListResourceTemplatesResult(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        var arr = obj.getJsonArray("resourceTemplates");
        if (arr == null) throw new IllegalArgumentException("resourceTemplates required");
        java.util.List<ResourceTemplate> templates = new java.util.ArrayList<>();
        arr.forEach(v -> templates.add(toResourceTemplate(v.asJsonObject())));
        return new ListResourceTemplatesResult(templates, obj.getString("nextCursor", null));
    }
}
