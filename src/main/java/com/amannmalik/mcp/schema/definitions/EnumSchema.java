package com.amannmalik.mcp.schema.definitions;

import com.amannmalik.mcp.json.McpJsonWriter;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;

import java.util.List;
import java.util.Optional;

/** JSON enum schema definition. */
public record EnumSchema(
        List<String> values,
        Optional<List<String>> names,
        Optional<String> title,
        Optional<String> description
) implements PrimitiveSchemaDefinition {

    public EnumSchema {
        values = List.copyOf(values);
        names = names.map(List::copyOf);
    }

    @Override
    public JsonObject json() {
        JsonObjectBuilder b = McpJsonWriter.object().add("type", "string");
        JsonArrayBuilder arr = McpJsonWriter.array();
        values.forEach(arr::add);
        b.add("enum", arr.build());
        names.ifPresent(n -> {
            JsonArrayBuilder na = McpJsonWriter.array();
            n.forEach(na::add);
            b.add("enumNames", na.build());
        });
        McpJsonWriter.add(b, "title", title.map(Json::createValue));
        McpJsonWriter.add(b, "description", description.map(Json::createValue));
        return b.build();
    }

    static EnumSchema fromJson(JsonObject obj) {
        List<String> vals = obj.getJsonArray("enum").getValuesAs(JsonString.class).stream().map(JsonString::getString).toList();
        Optional<List<String>> names = Optional.ofNullable(obj.getJsonArray("enumNames"))
                .map(a -> a.getValuesAs(JsonString.class).stream().map(JsonString::getString).toList());
        return new EnumSchema(
                vals,
                names,
                Optional.ofNullable(obj.getString("title", null)),
                Optional.ofNullable(obj.getString("description", null))
        );
    }
}
