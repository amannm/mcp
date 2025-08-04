package com.amannmalik.mcp.sampling;

import com.amannmalik.mcp.content.ContentBlock;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.prompts.Role;
import com.amannmalik.mcp.validation.ValidationUtil;
import jakarta.json.*;

import java.util.Set;

public record CreateMessageResponse(
        Role role,
        MessageContent content,
        String model,
        String stopReason,
        JsonObject _meta
) {
    public static final JsonCodec<CreateMessageResponse> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(CreateMessageResponse resp) {
            JsonObjectBuilder b = Json.createObjectBuilder()
                    .add("role", resp.role().name().toLowerCase())
                    .add("content", ContentBlock.CODEC.toJson((ContentBlock) resp.content()))
                    .add("model", resp.model());
            if (resp.stopReason() != null) b.add("stopReason", resp.stopReason());
            if (resp._meta() != null) b.add("_meta", resp._meta());
            return b.build();
        }

        @Override
        public CreateMessageResponse fromJson(JsonObject obj) {
            if (obj == null) throw new IllegalArgumentException("object required");
            AbstractEntityCodec.requireOnlyKeys(obj, Set.of("role", "content", "model", "stopReason", "_meta"));
            String raw = obj.getString("role", null);
            if (raw == null) throw new IllegalArgumentException("role required");
            Role role = Role.valueOf(raw.toUpperCase());
            JsonObject c = obj.getJsonObject("content");
            if (c == null) throw new IllegalArgumentException("content required");
            MessageContent content = (MessageContent) ContentBlock.CODEC.fromJson(c);
            String model = obj.getString("model", null);
            if (model == null) throw new IllegalArgumentException("model required");
            String stop = obj.getString("stopReason", null);
            JsonObject meta = obj.getJsonObject("_meta");
            return new CreateMessageResponse(role, content, model, stop, meta);
        }
    };

    public CreateMessageResponse {
        if (role == null || content == null || model == null) {
            throw new IllegalArgumentException("role, content, and model are required");
        }
        ValidationUtil.requireMeta(_meta);
    }
}
