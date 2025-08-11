package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.Annotations;
import com.amannmalik.mcp.api.Resource;
import jakarta.json.*;

public non-sealed class ResourceAbstractEntityCodec extends AbstractEntityCodec<Resource> {
    @Override
    public JsonObject toJson(Resource r) {
        JsonObjectBuilder b = Json.createObjectBuilder()
                .add("uri", r.uri())
                .add("name", r.name());
        if (r.title() != null) b.add("title", r.title());
        if (r.description() != null) b.add("description", r.description());
        if (r.mimeType() != null) b.add("mimeType", r.mimeType());
        if (r.size() != null) b.add("size", r.size());
        if (r.annotations() != AnnotationsJsonCodec.EMPTY) {
            b.add("annotations", new AnnotationsJsonCodec().toJson(r.annotations()));
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
                ? new AnnotationsJsonCodec().fromJson(getObject(obj, "annotations"))
                : AnnotationsJsonCodec.EMPTY;
        JsonObject meta = obj.getJsonObject("_meta");
        return new Resource(uri, name, title, description, mimeType, size, annotations, meta);
    }
}
