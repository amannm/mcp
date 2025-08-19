package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.spi.ResourceBlock;
import com.amannmalik.mcp.util.Base64Util;
import jakarta.json.*;

import java.util.Set;

public class ResourceBlockJsonCodec implements JsonCodec<ResourceBlock> {
    @Override
    public JsonObject toJson(ResourceBlock block) {
        var b = Json.createObjectBuilder().add("uri", block.uri());
        if (block.mimeType() != null) b.add("mimeType", block.mimeType());
        if (block._meta() != null) b.add("_meta", block._meta());
        return switch (block) {
            case ResourceBlock.Text t -> b.add("text", t.text()).build();
            case ResourceBlock.Binary bin -> b.add("blob", Base64Util.encode(bin.blob())).build();
        };
    }

    @Override
    public ResourceBlock fromJson(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        var uri = obj.getString("uri", null);
        if (uri == null) throw new IllegalArgumentException("uri required");
        var mime = obj.getString("mimeType", null);
        var meta = obj.getJsonObject("_meta");
        var hasText = obj.containsKey("text");
        var hasBlob = obj.containsKey("blob");
        if (hasText == hasBlob) {
            throw new IllegalArgumentException("exactly one of text or blob must be present");
        }
        AbstractEntityCodec.requireOnlyKeys(obj, Set.of("uri", "mimeType", "_meta", "text", "blob"));
        if (hasText) {
            return new ResourceBlock.Text(uri, mime, obj.getString("text"), meta);
        }
        var data = Base64Util.decode(obj.getString("blob"));
        return new ResourceBlock.Binary(uri, mime, data, meta);
    }
}
