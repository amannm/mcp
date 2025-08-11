package com.amannmalik.mcp.api;

import com.amannmalik.mcp.codec.AnnotationsJsonCodec;
import com.amannmalik.mcp.util.DisplayNameProvider;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

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

    public Resource {
        uri = ValidationUtil.requireAbsoluteUri(uri);
        name = ValidationUtil.requireClean(name);
        title = ValidationUtil.cleanNullable(title);
        description = ValidationUtil.cleanNullable(description);
        mimeType = ValidationUtil.cleanNullable(mimeType);
        if (size != null) size = ValidationUtil.requireNonNegative(size, "size");
        annotations = annotations == null ? AnnotationsJsonCodec.EMPTY : annotations;
        ValidationUtil.requireMeta(_meta);
    }

}
