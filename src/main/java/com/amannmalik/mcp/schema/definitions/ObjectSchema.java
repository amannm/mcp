package com.amannmalik.mcp.schema.definitions;

import com.amannmalik.mcp.json.McpJsonWriter;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Simplified JSON object schema supporting only top level properties. */
public record ObjectSchema(
        Map<String, PrimitiveSchemaDefinition> properties,
        Optional<List<String>> required
) {
    public ObjectSchema {
        properties = Map.copyOf(properties);
        required = required.map(List::copyOf);
    }

    public JsonObject json() {
        JsonObjectBuilder props = McpJsonWriter.object();
        properties.forEach((k, v) -> props.add(k, v.json()));
        JsonObjectBuilder b = McpJsonWriter.object().add("type", "object").add("properties", props.build());
        required.ifPresent(r -> {
            var arr = McpJsonWriter.array();
            r.forEach(arr::add);
            b.add("required", arr.build());
        });
        return b.build();
    }

    public static ObjectSchema fromJson(JsonObject obj) {
        JsonObject props = obj.getJsonObject("properties");
        Map<String, PrimitiveSchemaDefinition> map = props.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> PrimitiveSchemaDefinition.fromJson(e.getValue().asJsonObject()).orElseThrow()));
        Optional<List<String>> req = Optional.ofNullable(obj.getJsonArray("required"))
                .map(a -> a.getValuesAs(JsonString.class).stream().map(JsonString::getString).toList());
        return new ObjectSchema(map, req);
    }
}
