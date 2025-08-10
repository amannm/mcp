package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.*;
import jakarta.json.*;

import java.util.Objects;
import java.util.Set;
import java.util.function.*;

public sealed abstract class AbstractEntityCodec<T> implements JsonCodec<T> permits
        AbstractEntityPageCodec,
        CallToolRequestAbstractEntityCodec,
        ClientInfoAbstractEntityCodec,
        CreateMessageResponseAbstractEntityCodec,
        GetPromptRequestAbstractEntityCodec,
        InitializeRequestAbstractEntityCodec,
        InitializeResponseAbstractEntityCodec,
        ListRootsResultAbstractEntityCodec,
        LoggingMessageNotificationAbstractEntityCodec,
        PromptAbstractEntityCodec,
        PromptArgumentAbstractEntityCodec,
        PromptInstanceAbstractEntityCodec,
        PromptMessageAbstractEntityCodec,
        ReadResourceRequestAbstractEntityCodec,
        ResourceAbstractEntityCodec,
        ResourceEmptyCodec,
        ResourceEntityFieldCodec,
        ResourceEntityMetaCodec,
        ResourceTemplateAbstractEntityCodec,
        ResourceUpdatedNotificationAbstractEntityCodec,
        RootAbstractEntityCodec, SamplingMessageAbstractEntityCodec,
        ServerInfoAbstractEntityCodec,
        SetLevelRequestAbstractEntityCodec,
        SubscribeRequestAbstractEntityCodec,
        ToolAbstractEntityCodec,
        ToolAnnotationsAbstractEntityCodec,
        ToolResultAbstractEntityCodec,
        UnsubscribeRequestAbstractEntityCodec {

    protected static final JsonCodec<Annotations> ANNOTATIONS_CODEC = new AnnotationsJsonCodec();
    protected static final JsonCodec<ContentBlock> CONTENT_BLOCK_CODEC = new ContentBlockJsonCodec();

    protected static final Set<String> REQUEST_KEYS = Set.of("cursor", "_meta");
    protected static final Set<String> META_KEYS = Set.of("_meta");

    public static <T> JsonObject paginated(String field, Pagination.Page<T> page, Function<T, JsonValue> encoder, JsonObject meta) {
        JsonArrayBuilder arr = Json.createArrayBuilder();
        page.items().forEach(item -> arr.add(encoder.apply(item)));
        JsonObjectBuilder b = Json.createObjectBuilder().add(field, arr.build());
        if (page.nextCursor() != null) b.add("nextCursor", page.nextCursor());
        if (meta != null) b.add("_meta", meta);
        return b.build();
    }

    public static <T> JsonCodec<T> paginatedRequest(
            Function<T, String> cursor,
            Function<T, JsonObject> meta,
            BiFunction<String, JsonObject, T> from) {
        return new AbstractEntityPageCodec<>(cursor, meta, from);
    }

    public static <I, R> JsonCodec<R> paginatedResult(
            String field,
            String itemName,
            Function<R, Pagination.Page<I>> toPage,
            Function<R, JsonObject> meta,
            JsonCodec<I> itemCodec,
            BiFunction<Pagination.Page<I>, JsonObject, R> from) {
        return new ResourceEntityFieldCodec<>(field, toPage, itemCodec, meta, itemName, from);
    }

    public static <T> JsonCodec<T> metaOnly(
            Function<T, JsonObject> meta,
            Function<JsonObject, T> from) {
        return new ResourceEntityMetaCodec<>(meta, from);
    }

    public static <T> JsonCodec<T> empty(Supplier<T> from) {
        return new ResourceEmptyCodec<>(from);
    }

    protected static String requireString(JsonObject obj, String key) {
        String val = obj.getString(key, null);
        if (val == null) throw new IllegalArgumentException(key + " required");
        return val;
    }

    protected static JsonObject getObject(JsonObject obj, String key) {
        return obj.getJsonObject(key);
    }

    protected static Role requireRole(JsonObject obj) {
        return Role.valueOf(requireString(obj, "role").toUpperCase());
    }

    protected static ContentBlock requireContent(JsonObject obj) {
        JsonObject c = getObject(obj, "content");
        if (c == null) throw new IllegalArgumentException("content required");
        return CONTENT_BLOCK_CODEC.fromJson(c);
    }

    public static void requireOnlyKeys(JsonObject obj, Set<String> allowed) {
        Objects.requireNonNull(obj);
        Objects.requireNonNull(allowed);
        for (String key : obj.keySet()) {
            if (!allowed.contains(key)) throw new IllegalArgumentException("unexpected field: " + key);
        }
    }
}
