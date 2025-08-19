package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.GetPromptRequest;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.*;

import java.util.*;

public final class GetPromptRequestAbstractEntityCodec extends AbstractEntityCodec<GetPromptRequest> {
    @Override
    public JsonObject toJson(GetPromptRequest req) {
        var b = Json.createObjectBuilder().add("name", req.name());
        if (!req.arguments().isEmpty()) {
            var args = Json.createObjectBuilder();
            req.arguments().forEach(args::add);
            b.add("arguments", args.build());
        }
        if (req._meta() != null) b.add("_meta", req._meta());
        return b.build();
    }

    @Override
    public GetPromptRequest fromJson(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("params required");
        requireOnlyKeys(obj, Set.of("name", "arguments", "_meta"));
        var name = requireString(obj, "name");
        var argsObj = obj.getJsonObject("arguments");
        Map<String, String> args = Map.of();
        if (argsObj != null) {
            Map<String, String> tmp = new HashMap<>();
            argsObj.forEach((k, v) -> {
                if (v.getValueType() != JsonValue.ValueType.STRING) {
                    throw new IllegalArgumentException("argument values must be strings");
                }
                var key = ValidationUtil.requireClean(k);
                var value = ValidationUtil.requireClean(((JsonString) v).getString());
                tmp.put(key, value);
            });
            args = Map.copyOf(tmp);
        }
        var meta = obj.getJsonObject("_meta");
        return new GetPromptRequest(name, args, meta);
    }
}
