package com.amannmalik.mcp.sampling;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.prompts.Role;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.JsonObject;

public record CreateMessageResponse(
        Role role,
        MessageContent content,
        String model,
        String stopReason,
        JsonObject _meta
) {
    public static final JsonCodec<CreateMessageResponse> JSON = new Codec();

    public CreateMessageResponse {
        if (role == null || content == null || model == null) {
            throw new IllegalArgumentException("role, content, and model are required");
        }
        MetaValidator.requireValid(_meta);
    }

    private static final class Codec extends AbstractEntityCodec<CreateMessageResponse> {
        @Override
        public JsonObject toJson(CreateMessageResponse resp) {
            var obj = object()
                    .add("role", resp.role().name().toLowerCase())
                    .add("content", MessageContent.JSON.toJson(resp.content()))
                    .add("model", resp.model());
            if (resp.stopReason() != null) obj.add("stopReason", resp.stopReason());
            if (resp._meta() != null) obj.add("_meta", resp._meta());
            return obj.build();
        }

        @Override
        public CreateMessageResponse fromJson(JsonObject obj) {
            Role role = Role.valueOf(requireString(obj, "role").toUpperCase());
            MessageContent content = MessageContent.JSON.fromJson(requireObject(obj, "content"));
            String model = requireString(obj, "model");
            String stop = getString(obj, "stopReason");
            JsonObject meta = getObject(obj, "_meta");
            return new CreateMessageResponse(role, content, model, stop, meta);
        }
    }
}
