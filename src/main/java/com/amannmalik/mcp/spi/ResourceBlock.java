package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public sealed interface ResourceBlock permits
        ResourceBlock.Text,
        ResourceBlock.Binary {

    String uri();

    String mimeType();

    JsonObject _meta();

    record Text(String uri, String mimeType, String text, JsonObject _meta) implements ResourceBlock {
        public Text {
            uri = ValidationUtil.requireAbsoluteUri(uri);
            mimeType = ValidationUtil.cleanNullable(mimeType);
            text = ValidationUtil.requireClean(text);
            ValidationUtil.requireMeta(_meta);
        }
    }

    record Binary(String uri, String mimeType, byte[] blob, JsonObject _meta) implements ResourceBlock {
        public Binary {
            uri = ValidationUtil.requireAbsoluteUri(uri);
            mimeType = ValidationUtil.cleanNullable(mimeType);
            if (blob == null) {
                throw new IllegalArgumentException("blob is required");
            }
            blob = blob.clone();
            ValidationUtil.requireMeta(_meta);
        }

        @Override
        public byte[] blob() {
            return blob.clone();
        }
    }

}
