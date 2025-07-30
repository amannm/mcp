package com.amannmalik.mcp.annotations;

import com.amannmalik.mcp.prompts.Role;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.Set;

public final class AnnotationsCodec {
    private AnnotationsCodec() {
    }

    public static JsonObject toJsonObject(Annotations ann) {
        JsonObjectBuilder b = Json.createObjectBuilder();
        if (!ann.audience().isEmpty()) {
            JsonArrayBuilder arr = Json.createArrayBuilder();
            ann.audience().forEach(r -> arr.add(r.name().toLowerCase()));
            b.add("audience", arr);
        }
        if (ann.priority() != null) b.add("priority", ann.priority());
        if (ann.lastModified() != null) b.add("lastModified", ann.lastModified().toString());
        return b.build();
    }

    public static Annotations toAnnotations(JsonObject obj) {
        if (obj == null) return null;
        Set<Role> audience = EnumSet.noneOf(Role.class);
        var arr = obj.getJsonArray("audience");
        if (arr != null) {
            arr.getValuesAs(JsonString.class)
                    .forEach(js -> audience.add(Role.valueOf(js.getString().toUpperCase())));
        }
        Double priority = obj.containsKey("priority") ? obj.getJsonNumber("priority").doubleValue() : null;
        Instant lastModified = null;
        if (obj.containsKey("lastModified")) {
            try {
                lastModified = Instant.parse(obj.getString("lastModified"));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid lastModified", e);
            }
        }
        return new Annotations(audience.isEmpty() ? Set.of() : EnumSet.copyOf(audience), priority, lastModified);
    }
}
