package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.validation.*;
import jakarta.json.JsonObject;

public sealed interface ResourceBlock permits ResourceBlock.Text, ResourceBlock.Binary {
    String uri();

    String mimeType();

    JsonObject _meta();

    record Text(String uri, String mimeType, String text, JsonObject _meta) implements ResourceBlock {
        public Text {
            uri = UriValidator.requireAbsolute(uri);
            mimeType = InputSanitizer.cleanNullable(mimeType);
            text = InputSanitizer.requireClean(text);
            MetaValidator.requireValid(_meta);
        }
    }

    record Binary(String uri, String mimeType, byte[] blob, JsonObject _meta) implements ResourceBlock {
        public Binary {
            uri = UriValidator.requireAbsolute(uri);
            mimeType = InputSanitizer.cleanNullable(mimeType);
            if (blob == null) {
                throw new IllegalArgumentException("blob is required");
            }
            blob = blob.clone();
            MetaValidator.requireValid(_meta);
        }

        @Override
        public byte[] blob() {
            return blob.clone();
        }
    }
}
