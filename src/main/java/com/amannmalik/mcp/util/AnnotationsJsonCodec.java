package com.amannmalik.mcp.util;

import com.amannmalik.mcp.api.Annotations;
import com.amannmalik.mcp.api.Role;
import com.amannmalik.mcp.jsonrpc.JsonCodec;
import jakarta.json.*;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.Set;

public class AnnotationsJsonCodec implements JsonCodec<Annotations> {

    public static final Annotations EMPTY = new Annotations(Set.of(), null, null);

    @Override
    public JsonObject toJson(Annotations ann) {
        if (ann == null || ann.equals(EMPTY)) return JsonValue.EMPTY_JSON_OBJECT;
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

    @Override
    public Annotations fromJson(JsonObject obj) {
        if (obj == null || obj.isEmpty()) return EMPTY;
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
        Annotations ann = new Annotations(audience, priority, lastModified);
        return ann.audience().isEmpty() && ann.priority() == null && ann.lastModified() == null ? EMPTY : ann;
    }
}
