package com.amannmalik.mcp.api;

import com.amannmalik.mcp.jsonrpc.JsonCodec;
import com.amannmalik.mcp.util.*;
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
    public static final JsonCodec<Resource> CODEC = new ResourceAbstractEntityCodec();

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
