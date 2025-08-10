package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.Context;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.*;

import java.util.HashMap;
import java.util.Map;

public class ContextJsonCodec implements JsonCodec<Context> {
    @Override
    public JsonObject toJson(Context ctx) {
        JsonObjectBuilder args = Json.createObjectBuilder();
        ctx.arguments().forEach(args::add);
        return Json.createObjectBuilder().add("arguments", args.build()).build();
    }

    @Override
    public Context fromJson(JsonObject obj) {
        JsonObject argsObj = obj.getJsonObject("arguments");
        Map<String, String> map = new HashMap<>();
        if (argsObj != null) {
            argsObj.forEach((k, v) -> {
                if (v.getValueType() != JsonValue.ValueType.STRING) {
                    throw new IllegalArgumentException("context arguments must be strings");
                }
                map.put(k, ((JsonString) v).getString());
            });
        }
        return new Context(ValidationUtil.requireCleanMap(map));
    }
}
