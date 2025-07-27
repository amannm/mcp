package com.amannmalik.mcp.schema.definitions;

import com.amannmalik.mcp.json.McpJsonWriter;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import java.util.Optional;

/** JSON number or integer schema definition. */
public record NumberSchema(
        Type type,
        Optional<String> title,
        Optional<String> description,
        Optional<Double> minimum,
        Optional<Double> maximum
) implements PrimitiveSchemaDefinition {

    public enum Type { NUMBER("number"), INTEGER("integer");
        private final String value; Type(String v) { this.value = v; }
        @Override public String toString() { return value; }
        static Optional<Type> from(String v) { return switch (v) {
            case "number" -> Optional.of(NUMBER);
            case "integer" -> Optional.of(INTEGER);
            default -> Optional.empty();
        }; }
    }

    public NumberSchema {
        minimum.ifPresent(min -> maximum.ifPresent(max -> { if (max < min) throw new IllegalArgumentException("max < min"); }));
    }

    @Override
    public JsonObject json() {
        JsonObjectBuilder b = McpJsonWriter.object().add("type", type.toString());
        McpJsonWriter.add(b, "title", title.map(Json::createValue));
        McpJsonWriter.add(b, "description", description.map(Json::createValue));
        McpJsonWriter.add(b, "minimum", minimum.map(Json::createValue));
        McpJsonWriter.add(b, "maximum", maximum.map(Json::createValue));
        return b.build();
    }

    static NumberSchema fromJson(JsonObject obj) {
        var t = Type.from(obj.getString("type"));
        return new NumberSchema(
                t.orElse(Type.NUMBER),
                Optional.ofNullable(obj.getString("title", null)),
                Optional.ofNullable(obj.getString("description", null)),
                obj.containsKey("minimum") ? Optional.of(obj.getJsonNumber("minimum").doubleValue()) : Optional.empty(),
                obj.containsKey("maximum") ? Optional.of(obj.getJsonNumber("maximum").doubleValue()) : Optional.empty()
        );
    }
}
