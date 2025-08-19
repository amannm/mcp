package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.spi.*;
import jakarta.json.*;

import java.util.Set;

public final class CreateMessageResponseAbstractEntityCodec extends AbstractEntityCodec<CreateMessageResponse> {
    @Override
    public JsonObject toJson(CreateMessageResponse resp) {
        var b = Json.createObjectBuilder()
                .add("role", resp.role().name().toLowerCase())
                .add("content", CONTENT_BLOCK_CODEC.toJson((ContentBlock) resp.content()))
                .add("model", resp.model());
        if (resp.stopReason() != null) b.add("stopReason", resp.stopReason());
        if (resp._meta() != null) b.add("_meta", resp._meta());
        return b.build();
    }

    @Override
    public CreateMessageResponse fromJson(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        requireOnlyKeys(obj, Set.of("role", "content", "model", "stopReason", "_meta"));
        var role = requireRole(obj);
        var block = requireContent(obj);
        if (!(block instanceof MessageContent mc)) {
            throw new IllegalArgumentException("content must be message-capable");
        }
        var model = requireString(obj, "model");
        var stop = obj.getString("stopReason", null);
        var meta = obj.getJsonObject("_meta");
        return new CreateMessageResponse(role, mc, model, stop, meta);
    }
}
