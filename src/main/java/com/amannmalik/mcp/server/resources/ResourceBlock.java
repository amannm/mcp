package com.amannmalik.mcp.server.resources;

import com.amannmalik.mcp.validation.InputSanitizer;
import com.amannmalik.mcp.validation.UriValidator;

public sealed interface ResourceBlock permits ResourceBlock.Text, ResourceBlock.Binary {
    String uri();
    String name();
    String title();
    String mimeType();
    ResourceAnnotations annotations();

    record Text(String uri, String name, String title, String mimeType, String text, ResourceAnnotations annotations) implements ResourceBlock {
        public Text {
            uri = UriValidator.requireAbsolute(uri);
            name = InputSanitizer.requireClean(name);
            title = title == null ? null : InputSanitizer.requireClean(title);
            mimeType = mimeType == null ? null : InputSanitizer.requireClean(mimeType);
            text = InputSanitizer.requireClean(text);
        }
    }

    record Binary(String uri, String name, String title, String mimeType, byte[] blob, ResourceAnnotations annotations) implements ResourceBlock {
        public Binary {
            uri = UriValidator.requireAbsolute(uri);
            name = InputSanitizer.requireClean(name);
            title = title == null ? null : InputSanitizer.requireClean(title);
            mimeType = mimeType == null ? null : InputSanitizer.requireClean(mimeType);
            if (blob == null) {
                throw new IllegalArgumentException("blob is required");
            }
        }
    }
}
