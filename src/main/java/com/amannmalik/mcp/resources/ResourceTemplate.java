package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.annotations.Annotations;
import com.amannmalik.mcp.transport.AbstractEntityCodec;
import com.amannmalik.mcp.transport.JsonCodec;
import com.amannmalik.mcp.util.DisplayNameProvider;
import com.amannmalik.mcp.validation.ValidationUtil;
import jakarta.json.*;

public record ResourceTemplate(
        String uriTemplate,
        String name,
        String title,
        String description,
        String mimeType,
        Annotations annotations,
        JsonObject _meta
) implements DisplayNameProvider {
    public static final JsonCodec<ResourceTemplate> CODEC = new AbstractEntityCodec<>() {
        @Override
        public JsonObject toJson(ResourceTemplate t) {
            JsonObjectBuilder b = Json.createObjectBuilder()
                    .add("uriTemplate", t.uriTemplate())
                    .add("name", t.name());
            if (t.title() != null) b.add("title", t.title());
            if (t.description() != null) b.add("description", t.description());
            if (t.mimeType() != null) b.add("mimeType", t.mimeType());
            if (t.annotations() != Annotations.EMPTY) {
                b.add("annotations", Annotations.CODEC.toJson(t.annotations()));
            }
            if (t._meta() != null) b.add("_meta", t._meta());
            return b.build();
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
                    ? Annotations.CODEC.fromJson(getObject(obj, "annotations"))
                    : Annotations.EMPTY;
            JsonObject meta = obj.getJsonObject("_meta");
            return new ResourceTemplate(uriTemplate, name, title, description, mimeType, annotations, meta);
        }
    };

    public ResourceTemplate {
        uriTemplate = ValidationUtil.requireAbsoluteTemplate(uriTemplate);
        name = ValidationUtil.requireClean(name);
        title = ValidationUtil.cleanNullable(title);
        description = ValidationUtil.cleanNullable(description);
        mimeType = ValidationUtil.cleanNullable(mimeType);
        annotations = annotations == null ? Annotations.EMPTY : annotations;
        ValidationUtil.requireMeta(_meta);
    }
}
