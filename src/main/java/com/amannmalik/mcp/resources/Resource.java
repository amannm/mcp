package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.annotations.Annotations;
import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.util.DisplayNameProvider;
import com.amannmalik.mcp.validation.*;
import jakarta.json.*;

public record Resource(
        String uri,
        String name,
        String title,
        String description,
        String mimeType,
        Long size,
        Annotations annotations,
        JsonObject _meta
) implements DisplayNameProvider {
    public static final JsonCodec<Resource> CODEC = new AbstractEntityCodec<>() {
        @Override
        public JsonObject toJson(Resource r) {
            JsonObjectBuilder b = Json.createObjectBuilder()
                    .add("uri", r.uri())
                    .add("name", r.name());
            if (r.title() != null) b.add("title", r.title());
            if (r.description() != null) b.add("description", r.description());
            if (r.mimeType() != null) b.add("mimeType", r.mimeType());
            if (r.size() != null) b.add("size", r.size());
            if (r.annotations() != Annotations.EMPTY) {
                b.add("annotations", Annotations.CODEC.toJson(r.annotations()));
            }
            if (r._meta() != null) b.add("_meta", r._meta());
            return b.build();
        }

        @Override
        public Resource fromJson(JsonObject obj) {
            if (obj == null) throw new IllegalArgumentException("object required");
            String uri = requireString(obj, "uri");
            String name = requireString(obj, "name");
            String title = obj.getString("title", null);
            String description = obj.getString("description", null);
            String mimeType = obj.getString("mimeType", null);
            Long size = obj.containsKey("size") ? obj.getJsonNumber("size").longValue() : null;
            Annotations annotations = obj.containsKey("annotations")
                    ? Annotations.CODEC.fromJson(getObject(obj, "annotations"))
                    : Annotations.EMPTY;
            JsonObject meta = obj.getJsonObject("_meta");
            return new Resource(uri, name, title, description, mimeType, size, annotations, meta);
        }
    };

    public Resource {
        uri = UriValidator.requireAbsolute(uri);
        name = InputSanitizer.requireClean(name);
        title = InputSanitizer.cleanNullable(title);
        description = InputSanitizer.cleanNullable(description);
        mimeType = InputSanitizer.cleanNullable(mimeType);
        if (size != null && size < 0) {
            throw new IllegalArgumentException("size must be >= 0");
        }
        annotations = annotations == null ? Annotations.EMPTY : annotations;
        MetaValidator.requireValid(_meta);
    }
}
