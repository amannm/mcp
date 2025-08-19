package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.spi.Annotations;
import com.amannmalik.mcp.spi.Resource;
import jakarta.json.*;

public non-sealed class ResourceAbstractEntityCodec extends AbstractEntityCodec<Resource> {
    @Override
    public JsonObject toJson(Resource r) {
        var b = Json.createObjectBuilder()
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
        var uri = requireString(obj, "uri");
        var name = requireString(obj, "name");
        var title = obj.getString("title", null);
        var description = obj.getString("description", null);
        var mimeType = obj.getString("mimeType", null);
        var size = obj.containsKey("size") ? obj.getJsonNumber("size").longValue() : null;
        var annotations = obj.containsKey("annotations")
                ? new AnnotationsJsonCodec().fromJson(getObject(obj, "annotations"))
                : AnnotationsJsonCodec.EMPTY;
        var meta = obj.getJsonObject("_meta");
        return new Resource(uri, name, title, description, mimeType, size, annotations, meta);
    }
}
