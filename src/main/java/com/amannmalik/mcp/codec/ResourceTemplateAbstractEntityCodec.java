package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.spi.ResourceTemplate;
import jakarta.json.Json;
import jakarta.json.JsonObject;

public final class ResourceTemplateAbstractEntityCodec extends AbstractEntityCodec<ResourceTemplate> {
    public ResourceTemplateAbstractEntityCodec() {
    }

    @Override
    public JsonObject toJson(ResourceTemplate t) {
        var b = Json.createObjectBuilder()
                .add("uriTemplate", t.uriTemplate())
                .add("name", t.name());
        if (t.title() != null) {
            b.add("title", t.title());
        }
        if (t.description() != null) {
            b.add("description", t.description());
        }
        if (t.mimeType() != null) {
            b.add("mimeType", t.mimeType());
        }
        if (t.annotations() != AnnotationsJsonCodec.EMPTY) {
            b.add("annotations", new AnnotationsJsonCodec().toJson(t.annotations()));
        }
        if (t._meta() != null) {
            b.add("_meta", t._meta());
        }
        return b.build();
    }

    @Override
    public ResourceTemplate fromJson(JsonObject obj) {
        if (obj == null) {
            throw new IllegalArgumentException("object required");
        }
        var uriTemplate = requireString(obj, "uriTemplate");
        var name = requireString(obj, "name");
        var title = obj.getString("title", null);
        var description = obj.getString("description", null);
        var mimeType = obj.getString("mimeType", null);
        var annotations = obj.containsKey("annotations")
                ? new AnnotationsJsonCodec().fromJson(getObject(obj, "annotations"))
                : AnnotationsJsonCodec.EMPTY;
        var meta = obj.getJsonObject("_meta");
        return new ResourceTemplate(uriTemplate, name, title, description, mimeType, annotations, meta);
    }
}
