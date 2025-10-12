package com.amannmalik.mcp.spi.internal;

import com.amannmalik.mcp.spi.Annotations;
import jakarta.json.JsonObject;

public final class ContentBlockContract {
    private ContentBlockContract() {
    }

    public static String requireText(String text) {
        SpiPreconditions.requireNonNull(text, "text is required");
        return SpiPreconditions.requireClean(text);
    }

    public static byte[] requireData(byte[] data) {
        return SpiPreconditions.requireData(data, "data");
    }

    public static String requireMimeType(String mimeType) {
        SpiPreconditions.requireNonNull(mimeType, "mimeType is required");
        return SpiPreconditions.requireClean(mimeType);
    }

    public static Annotations normalizeAnnotations(Annotations annotations) {
        return SpiPreconditions.annotationsOrEmpty(annotations);
    }

    public static JsonObject requireMeta(JsonObject meta) {
        SpiPreconditions.requireMeta(meta);
        return meta;
    }

    public static byte[] copy(byte[] value) {
        return SpiPreconditions.clone(value);
    }
}
