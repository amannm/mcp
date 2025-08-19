package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.spi.Annotations;
import com.amannmalik.mcp.spi.ResourceTemplate;
import jakarta.json.*;

public final class ResourceTemplateAbstractEntityCodec extends AbstractEntityCodec<ResourceTemplate> {
    @Override
    public JsonObject toJson(ResourceTemplate t) {
        JsonObjectBuilder b = Json.createObjectBuilder()
                .add("uriTemplate", t.uriTemplate())
                .add("name", t.name());
        if (t.title() != null) b.add("title", t.title());
        if (t.description() != null) b.add("description", t.description());
        if (t.mimeType() != null) b.add("mimeType", t.mimeType());
        if (t.annotations() != AnnotationsJsonCodec.EMPTY) {
            b.add("annotations", new AnnotationsJsonCodec().toJson(t.annotations()));
        }
        return addMeta(b, t._meta()).build();
    }

    @Override
    public ResourceTemplate fromJson(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        String uriTemplate = requireString(obj, "uriTemplate");
        String name = requireString(obj, "name");
        String title = obj.getString("title", null);
        String description = obj.getString("description", null);
        String mimeType = obj.getString("mimeType", null);
        Annotations annotations = obj.containsKey("annotations")
                ? new AnnotationsJsonCodec().fromJson(getObject(obj, "annotations"))
                : AnnotationsJsonCodec.EMPTY;
        return new ResourceTemplate(uriTemplate, name, title, description, mimeType, annotations, meta(obj));
    }
}
