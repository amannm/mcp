package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.util.Base64Util;
import com.amannmalik.mcp.validation.*;
import jakarta.json.*;

import java.util.Set;

public sealed interface ResourceBlock permits ResourceBlock.Text, ResourceBlock.Binary {
    JsonCodec<ResourceBlock> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(ResourceBlock block) {
            JsonObjectBuilder b = Json.createObjectBuilder().add("uri", block.uri());
            if (block.mimeType() != null) b.add("mimeType", block.mimeType());
            if (block._meta() != null) b.add("_meta", block._meta());
            return switch (block) {
                case Text t -> b.add("text", t.text()).build();
                case Binary bin -> b.add("blob", Base64Util.encode(bin.blob())).build();
            };
        }

        @Override
        public ResourceBlock fromJson(JsonObject obj) {
            if (obj == null) throw new IllegalArgumentException("object required");
            String uri = obj.getString("uri", null);
            if (uri == null) throw new IllegalArgumentException("uri required");
            String mime = obj.getString("mimeType", null);
            JsonObject meta = obj.getJsonObject("_meta");
            boolean hasText = obj.containsKey("text");
            boolean hasBlob = obj.containsKey("blob");
            if (hasText == hasBlob) {
                throw new IllegalArgumentException("exactly one of text or blob must be present");
            }
            AbstractEntityCodec.requireOnlyKeys(obj, Set.of("uri", "mimeType", "_meta", "text", "blob"));
            if (hasText) {
                return new Text(uri, mime, obj.getString("text"), meta);
            }
            byte[] data = Base64Util.decode(obj.getString("blob"));
            return new Binary(uri, mime, data, meta);
        }
    };

    String uri();

    String mimeType();

    JsonObject _meta();

    record Text(String uri, String mimeType, String text, JsonObject _meta) implements ResourceBlock {
        public Text {
            uri = ValidationUtil.requireAbsoluteUri(uri);
            mimeType = InputSanitizer.cleanNullable(mimeType);
            text = InputSanitizer.requireClean(text);
            ValidationUtil.requireMeta(_meta);
        }
    }

    record Binary(String uri, String mimeType, byte[] blob, JsonObject _meta) implements ResourceBlock {
        public Binary {
            uri = ValidationUtil.requireAbsoluteUri(uri);
            mimeType = InputSanitizer.cleanNullable(mimeType);
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
