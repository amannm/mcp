package com.amannmalik.mcp.test.impl;

import com.amannmalik.mcp.spi.*;
import jakarta.json.*;

public final class DefaultElicitationProvider implements ElicitationProvider {
    @Override
    public ElicitResult elicit(ElicitRequest request, long timeoutMillis) {
        var schema = request.requestedSchema();
        JsonObjectBuilder builder = Json.createObjectBuilder();
        if (schema != null) {
            var properties = schema.getJsonObject("properties");
            if (properties != null) {
                for (var entry : properties.entrySet()) {
                    builder.add(entry.getKey(), defaultValue(entry.getValue().asJsonObject()));
                }
            }
        }
        return new ElicitResult(ElicitationAction.ACCEPT, builder.build(), null);
    }

    private JsonValue defaultValue(JsonObject property) {
        var type = property.getString("type", "string");
        return switch (type) {
            case "integer" -> Json.createValue(0);
            case "number" -> Json.createValue(0.0);
            case "boolean" -> JsonValue.FALSE;
            case "object" -> Json.createObjectBuilder().build();
            case "array" -> Json.createArrayBuilder().build();
            default -> {
                if (property.getJsonArray("enum") instanceof JsonArray values &&
                        !values.isEmpty() && values.getFirst() instanceof JsonValue first) {
                    yield first;
                }
                yield Json.createValue("value");
            }
        };
    }
}
