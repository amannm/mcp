package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.spi.Argument;
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
        var name = obj.getString("name", null);
        var value = obj.getString("value", null);
        if (name == null || value == null) {
            throw new IllegalArgumentException("name and value required");
        }
        return new Argument(name, value);
    }
}
