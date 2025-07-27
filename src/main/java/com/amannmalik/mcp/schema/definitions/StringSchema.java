package com.amannmalik.mcp.schema.definitions;

import com.amannmalik.mcp.json.McpJsonWriter;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import java.util.Optional;

/** JSON string schema definition. */
public record StringSchema(
        Optional<String> title,
        Optional<String> description,
        Optional<Integer> minLength,
        Optional<Integer> maxLength,
        Optional<Format> format
) implements PrimitiveSchemaDefinition {

    public enum Format { EMAIL("email"), URI("uri"), DATE("date"), DATETIME("date-time");
        private final String value;
        Format(String v) { this.value = v; }
        @Override public String toString() { return value; }
        static Optional<Format> from(String v) {
            return switch (v) {
                case "email" -> Optional.of(EMAIL);
                case "uri" -> Optional.of(URI);
                case "date" -> Optional.of(DATE);
                case "date-time" -> Optional.of(DATETIME);
                default -> Optional.empty();
            };
        }
    }

    public StringSchema {
        title = title.filter(t -> !t.isBlank());
        description = description.filter(t -> !t.isBlank());
        minLength.ifPresent(m -> { if (m < 0) throw new IllegalArgumentException("minLength < 0"); });
        maxLength.ifPresent(m -> { if (m < 0) throw new IllegalArgumentException("maxLength < 0"); });
    }

    @Override
    public JsonObject json() {
        JsonObjectBuilder b = McpJsonWriter.object().add("type", "string");
        McpJsonWriter.add(b, "title", title.map(Json::createValue));
        McpJsonWriter.add(b, "description", description.map(Json::createValue));
        McpJsonWriter.add(b, "minLength", minLength.map(Json::createValue));
        McpJsonWriter.add(b, "maxLength", maxLength.map(Json::createValue));
        McpJsonWriter.add(b, "format", format.map(f -> Json.createValue(f.toString())));
        return b.build();
    }

    static StringSchema fromJson(JsonObject obj) {
        return new StringSchema(
                Optional.ofNullable(obj.getString("title", null)),
                Optional.ofNullable(obj.getString("description", null)),
                obj.containsKey("minLength") ? Optional.of(obj.getInt("minLength")) : Optional.empty(),
                obj.containsKey("maxLength") ? Optional.of(obj.getInt("maxLength")) : Optional.empty(),
                Optional.ofNullable(obj.getString("format", null)).flatMap(Format::from)
        );
    }
}
