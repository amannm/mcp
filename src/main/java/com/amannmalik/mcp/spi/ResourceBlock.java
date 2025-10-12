package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

import java.net.URI;

public sealed interface ResourceBlock permits
        ResourceBlock.Text,
        ResourceBlock.Binary {
    URI uri();

    String mimeType();

    JsonObject _meta();

    record Text(URI uri, String mimeType, String text, JsonObject _meta) implements ResourceBlock {
        public Text {
            uri = ValidationUtil.requireAbsoluteUri(uri);
            mimeType = ValidationUtil.cleanNullable(mimeType);
            text = ValidationUtil.requireClean(text);
            ValidationUtil.requireMeta(_meta);
        }
    }

    record Binary(URI uri, String mimeType, byte[] blob, JsonObject _meta) implements ResourceBlock {
        public Binary {
            uri = ValidationUtil.requireAbsoluteUri(uri);
            mimeType = ValidationUtil.cleanNullable(mimeType);
            blob = ValidationUtil.requireData(blob, "blob");
            ValidationUtil.requireMeta(_meta);
        }

        @Override
        public byte[] blob() {
            return ValidationUtil.clone(blob);
        }
    }
}
