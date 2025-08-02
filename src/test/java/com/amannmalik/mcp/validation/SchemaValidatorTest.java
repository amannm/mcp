package com.amannmalik.mcp.validation;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SchemaValidatorTest {
    @Test
    public void failsOnMissingRequiredField() {
        JsonObject schema = Json.createObjectBuilder()
                .add("type", "object")
                .add("required", Json.createArrayBuilder().add("a"))
                .add("properties", Json.createObjectBuilder()
                        .add("a", Json.createObjectBuilder().add("type", "string")))
                .build();
        JsonObject value = Json.createObjectBuilder().build();
        assertThrows(IllegalArgumentException.class, () -> SchemaValidator.validate(schema, value));
    }

    @Test
    public void validatesStringFormat() {
        JsonObject schema = Json.createObjectBuilder()
                .add("type", "object")
                .add("properties", Json.createObjectBuilder()
                        .add("email", Json.createObjectBuilder()
                                .add("type", "string")
                                .add("format", "email")))
                .build();
        JsonObject good = Json.createObjectBuilder().add("email", "a@b.c").build();
        assertDoesNotThrow(() -> SchemaValidator.validate(schema, good));
        JsonObject bad = Json.createObjectBuilder().add("email", "not an email").build();
        assertThrows(IllegalArgumentException.class, () -> SchemaValidator.validate(schema, bad));
    }
}
