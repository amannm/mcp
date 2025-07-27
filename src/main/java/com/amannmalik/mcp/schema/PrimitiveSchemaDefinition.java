package com.amannmalik.mcp.schema;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;

import java.util.List;
import java.util.Optional;

public sealed interface PrimitiveSchemaDefinition
        permits StringSchema, NumberSchema, BooleanSchema, EnumSchema {
    Optional<String> title();
    Optional<String> description();
    JsonObject toJson();
}

record StringSchema(Optional<String> title, Optional<String> description,
                    Optional<String> format, Optional<Integer> minLength,
                    Optional<Integer> maxLength) implements PrimitiveSchemaDefinition {
    public StringSchema {
        minLength = minLength.filter(v -> v >= 0);
        maxLength = maxLength.filter(v -> v >= 0);
    }

    @Override
    public JsonObject toJson() {
        var b = Json.createObjectBuilder().add("type", "string");
        title.ifPresent(v -> b.add("title", v));
        description.ifPresent(v -> b.add("description", v));
        format.ifPresent(v -> b.add("format", v));
        minLength.ifPresent(v -> b.add("minLength", v));
        maxLength.ifPresent(v -> b.add("maxLength", v));
        return b.build();
    }
}

record NumberSchema(Optional<String> title, Optional<String> description,
                    Optional<Double> minimum, Optional<Double> maximum)
        implements PrimitiveSchemaDefinition {
    @Override
    public JsonObject toJson() {
        var b = Json.createObjectBuilder().add("type", "number");
        title.ifPresent(v -> b.add("title", v));
        description.ifPresent(v -> b.add("description", v));
        minimum.ifPresent(v -> b.add("minimum", v));
        maximum.ifPresent(v -> b.add("maximum", v));
        return b.build();
    }
}

record BooleanSchema(Optional<String> title, Optional<String> description)
        implements PrimitiveSchemaDefinition {
    @Override
    public JsonObject toJson() {
        var b = Json.createObjectBuilder().add("type", "boolean");
        title.ifPresent(v -> b.add("title", v));
        description.ifPresent(v -> b.add("description", v));
        return b.build();
    }
}

record EnumSchema(Optional<String> title, Optional<String> description,
                  List<String> values, Optional<List<String>> names)
        implements PrimitiveSchemaDefinition {
    public EnumSchema {
        values = List.copyOf(values);
        if (values.isEmpty()) throw new IllegalArgumentException("enum values required");
        names = names.map(List::copyOf);
    }

    @Override
    public JsonObject toJson() {
        JsonArrayBuilder valuesArray = Json.createArrayBuilder();
        values.forEach(valuesArray::add);
        var b = Json.createObjectBuilder().add("enum", valuesArray);
        title.ifPresent(v -> b.add("title", v));
        description.ifPresent(v -> b.add("description", v));
        names.ifPresent(list -> {
            JsonArrayBuilder namesArray = Json.createArrayBuilder();
            list.forEach(namesArray::add);
            b.add("enumNames", namesArray);
        });
        return b.build();
    }
}
