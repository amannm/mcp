package com.amannmalik.mcp.server.resources;

import com.amannmalik.mcp.annotations.AnnotationsCodec;
import com.amannmalik.mcp.util.Base64Util;
import com.amannmalik.mcp.util.EmptyJsonObjectCodec;
import com.amannmalik.mcp.util.JsonUtil;
import com.amannmalik.mcp.util.PaginatedRequest;
import com.amannmalik.mcp.util.Pagination;
import com.amannmalik.mcp.util.PaginationCodec;
import com.amannmalik.mcp.util.PaginationJson;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

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
        if (r.annotations() != null) b.add("annotations", AnnotationsCodec.toJsonObject(r.annotations()));
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
                obj.containsKey("annotations") ? AnnotationsCodec.toAnnotations(obj.getJsonObject("annotations")) : null,
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
        if (t.annotations() != null) b.add("annotations", AnnotationsCodec.toJsonObject(t.annotations()));
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
                obj.containsKey("annotations") ? AnnotationsCodec.toAnnotations(obj.getJsonObject("annotations")) : null,
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
            case ResourceBlock.Binary b2 -> b.add("blob", Base64Util.encode(b2.blob()));
        }
        return b.build();
    }

    public static ResourceBlock toResourceBlock(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        String uri = obj.getString("uri", null);
        if (uri == null) throw new IllegalArgumentException("uri required");
        String mime = obj.getString("mimeType", null);
        JsonObject meta = obj.containsKey("_meta") ? obj.getJsonObject("_meta") : null;

        boolean hasText = obj.containsKey("text");
        boolean hasBlob = obj.containsKey("blob");
        if (hasText == hasBlob) {
            throw new IllegalArgumentException("exactly one of text or blob must be present");
        }

        JsonUtil.requireOnlyKeys(obj, Set.of("uri", "mimeType", "_meta", "text", "blob"));

        if (hasText) {
            return new ResourceBlock.Text(uri, mime, obj.getString("text"), meta);
        }

        byte[] data = Base64Util.decode(obj.getString("blob"));
        return new ResourceBlock.Binary(uri, mime, data, meta);
    }


    public static JsonObject toJsonObject(ResourceListChangedNotification n) {
        if (n == null) throw new IllegalArgumentException("notification required");
        return EmptyJsonObjectCodec.toJsonObject();
    }

    public static JsonObject toJsonObject(ResourceUpdatedNotification n) {
        if (n == null) throw new IllegalArgumentException("notification required");
        JsonObjectBuilder b = Json.createObjectBuilder()
                .add("uri", n.uri());
        if (n.title() != null) b.add("title", n.title());
        return b.build();
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

    public static JsonObject toJsonObject(ListResourcesResult result) {
        if (result == null) throw new IllegalArgumentException("result required");
        return PaginationJson.toJson(
                "resources",
                new Pagination.Page<>(result.resources(), result.nextCursor()),
                ResourcesCodec::toJsonObject);
    }

    public static JsonObject toJsonObject(ListResourcesRequest req) {
        if (req == null) throw new IllegalArgumentException("request required");
        return PaginationCodec.toJsonObject(new PaginatedRequest(req.cursor()));
    }

    public static ListResourcesRequest toListResourcesRequest(JsonObject obj) {
        String cursor = PaginationCodec.toPaginatedRequest(obj).cursor();
        return new ListResourcesRequest(cursor);
    }

    public static JsonObject toJsonObject(ListResourceTemplatesRequest req) {
        if (req == null) throw new IllegalArgumentException("request required");
        return PaginationCodec.toJsonObject(new PaginatedRequest(req.cursor()));
    }

    public static ListResourceTemplatesRequest toListResourceTemplatesRequest(JsonObject obj) {
        String cursor = PaginationCodec.toPaginatedRequest(obj).cursor();
        return new ListResourceTemplatesRequest(cursor);
    }

    public static JsonObject toJsonObject(ListResourceTemplatesResult result) {
        if (result == null) throw new IllegalArgumentException("result required");
        return PaginationJson.toJson(
                "resourceTemplates",
                new Pagination.Page<>(result.resourceTemplates(), result.nextCursor()),
                ResourcesCodec::toJsonObject);
    }

}
