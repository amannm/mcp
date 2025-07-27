package com.amannmalik.mcp.schema.resources;

import com.amannmalik.mcp.schema.core.FieldValidations;
import com.amannmalik.mcp.schema.core.Meta;

import java.util.Optional;
import java.util.Arrays;

/** Binary resource contents. */
public record BlobResourceContents(
        String uri,
        byte[] blob,
        Optional<String> mimeType,
        Optional<Meta> _meta
) implements ResourceContents {
    public BlobResourceContents {
        uri = FieldValidations.requireUri(uri);
        blob = blob.clone();
        mimeType = mimeType.filter(m -> !m.isBlank());
    }

    public byte[] blob() { return blob.clone(); }
}
