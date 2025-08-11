package com.amannmalik.mcp.api;

import com.amannmalik.mcp.codec.AnnotationsJsonCodec;
import com.amannmalik.mcp.util.DisplayNameProvider;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public record ResourceTemplate(
        String uriTemplate,
        String name,
        String title,
        String description,
        String mimeType,
        Annotations annotations,
        JsonObject _meta
) implements DisplayNameProvider {

    public ResourceTemplate {
        uriTemplate = ValidationUtil.requireAbsoluteTemplate(uriTemplate);
        name = ValidationUtil.requireClean(name);
        title = ValidationUtil.cleanNullable(title);
        description = ValidationUtil.cleanNullable(description);
        mimeType = ValidationUtil.cleanNullable(mimeType);
        annotations = annotations == null ? AnnotationsJsonCodec.EMPTY : annotations;
        ValidationUtil.requireMeta(_meta);
    }

}
