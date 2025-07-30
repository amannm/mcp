package com.amannmalik.mcp.server.resources;

import com.amannmalik.mcp.validation.InputSanitizer;
import com.amannmalik.mcp.validation.MetaValidator;
import com.amannmalik.mcp.validation.UriValidator;
import jakarta.json.JsonObject;

public sealed interface ResourceBlock permits ResourceBlock.Text, ResourceBlock.Binary {
    String uri();

    String mimeType();

    ResourceAnnotations annotations();

    JsonObject _meta();

    record Text(String uri, String mimeType, String text, ResourceAnnotations annotations, JsonObject _meta) implements ResourceBlock {
        public Text {
            uri = UriValidator.requireAbsolute(uri);
            mimeType = mimeType == null ? null : InputSanitizer.requireClean(mimeType);
            text = InputSanitizer.requireClean(text);
            MetaValidator.requireValid(_meta);
        }
    }

    record Binary(String uri, String mimeType, byte[] blob, ResourceAnnotations annotations, JsonObject _meta) implements ResourceBlock {
        public Binary {
            uri = UriValidator.requireAbsolute(uri);
            mimeType = mimeType == null ? null : InputSanitizer.requireClean(mimeType);
            if (blob == null) {
                throw new IllegalArgumentException("blob is required");
            }
            blob = blob.clone();
            MetaValidator.requireValid(_meta);
        }
    }
}
