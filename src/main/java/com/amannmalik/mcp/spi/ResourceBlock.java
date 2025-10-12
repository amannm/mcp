package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.SpiPreconditions;
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
            uri = SpiPreconditions.requireAbsoluteUri(uri);
            mimeType = SpiPreconditions.cleanNullable(mimeType);
            text = SpiPreconditions.requireClean(text);
            SpiPreconditions.requireMeta(_meta);
        }
    }

    record Binary(URI uri, String mimeType, byte[] blob, JsonObject _meta) implements ResourceBlock {
        public Binary {
            uri = SpiPreconditions.requireAbsoluteUri(uri);
            mimeType = SpiPreconditions.cleanNullable(mimeType);
            blob = SpiPreconditions.requireData(blob, "blob");
            SpiPreconditions.requireMeta(_meta);
        }

        @Override
        public byte[] blob() {
            return SpiPreconditions.clone(blob);
        }
    }
}
