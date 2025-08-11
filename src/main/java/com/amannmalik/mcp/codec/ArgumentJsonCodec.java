package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.model.Argument;
import jakarta.json.Json;
import jakarta.json.JsonObject;

public class ArgumentJsonCodec implements JsonCodec<Argument> {
    @Override
    public JsonObject toJson(Argument arg) {
        return Json.createObjectBuilder()
                .add("name", arg.name())
                .add("value", arg.value())
                .build();
    }

    @Override
    public Argument fromJson(JsonObject obj) {
        String name = obj.getString("name", null);
        String value = obj.getString("value", null);
        if (name == null || value == null) {
            throw new IllegalArgumentException("name and value required");
        }
        return new Argument(name, value);
    }
}
