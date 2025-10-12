package com.amannmalik.mcp.core;

import com.amannmalik.mcp.spi.Annotations;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public final class ContentBlockContract {
    private ContentBlockContract() {
    }

    public static String requireText(String text) {
        ValidationUtil.requireNonNull(text, "text is required");
        return ValidationUtil.requireClean(text);
    }

    public static byte[] requireData(byte[] data) {
        return ValidationUtil.requireData(data, "data");
    }

    public static String requireMimeType(String mimeType) {
        ValidationUtil.requireNonNull(mimeType, "mimeType is required");
        return ValidationUtil.requireClean(mimeType);
    }

    public static Annotations normalizeAnnotations(Annotations annotations) {
        return ValidationUtil.annotationsOrEmpty(annotations);
    }

    public static JsonObject requireMeta(JsonObject meta) {
        ValidationUtil.requireMeta(meta);
        return meta;
    }

    public static byte[] copy(byte[] value) {
        return ValidationUtil.clone(value);
    }
}
