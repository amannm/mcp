package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.spi.Context;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.*;

import java.util.HashMap;
import java.util.Map;

public class ContextJsonCodec implements JsonCodec<Context> {

    public ContextJsonCodec() {
    }

    @Override
    public JsonObject toJson(Context ctx) {
        var args = Json.createObjectBuilder();
        ctx.arguments().forEach(args::add);
        return Json.createObjectBuilder().add("arguments", args.build()).build();
    }

    @Override
    public Context fromJson(JsonObject obj) {
        var argsObj = obj.getJsonObject("arguments");
        Map<String, String> map = new HashMap<>();
        if (argsObj != null) {
            argsObj.forEach((k, v) -> {
                if (v instanceof JsonString js) {
                    map.put(k, js.getString());
                } else {
                    throw new IllegalArgumentException("context arguments must be strings");
                }
            });
        }
        return new Context(ValidationUtil.requireCleanMap(map));
    }
}
