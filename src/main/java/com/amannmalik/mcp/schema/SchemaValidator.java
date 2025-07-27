package com.amannmalik.mcp.schema;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Runtime validation utilities for schema based data.
 */
public final class SchemaValidator {
    private SchemaValidator() {}

    /** Result of validating a payload against a schema. */
    public record ValidationResult(boolean valid, Map<String, String> errors) {
        public ValidationResult {
            errors = Map.copyOf(errors);
        }
    }

    /** Validate arguments for a tool call. */
    public static ValidationResult validateToolInput(
            Tools.Tool tool,
            Map<String, Object> arguments) {
        Objects.requireNonNull(tool);
        Objects.requireNonNull(arguments);
        return validate(arguments, tool.inputSchema().properties(), tool.inputSchema().required());
    }

    /** Validate data returned from an elicitation request. */
    public static ValidationResult validateElicitationData(
            Elicitation.ElicitationSchema schema,
            Map<String, Object> data) {
        Objects.requireNonNull(schema);
        Objects.requireNonNull(data);
        return validate(data, schema.properties(), schema.required());
    }

    private static ValidationResult validate(
            Map<String, Object> values,
            Map<String, PrimitiveSchemaDefinition> props,
            Optional<List<String>> required) {
        Map<String, String> errors = new HashMap<>();
        required.orElse(List.of()).forEach(k -> {
            if (!values.containsKey(k)) errors.put(k, "required property missing");
        });
        values.forEach((k, v) -> {
            var schema = props.get(k);
            if (schema == null) {
                errors.put(k, "unknown property");
            } else {
                checkValue(k, v, schema, errors);
            }
        });
        return new ValidationResult(errors.isEmpty(), errors);
    }

    private static void checkValue(
            String name,
            Object value,
            PrimitiveSchemaDefinition schema,
            Map<String, String> errors) {
        switch (schema) {
            case StringSchema s -> validateString(name, value, s, errors);
            case NumberSchema n -> validateNumber(name, value, n, errors);
            case BooleanSchema b -> validateBoolean(name, value, b, errors);
            case EnumSchema e -> validateEnum(name, value, e, errors);
            default -> errors.put(name, "unsupported schema");
        }
    }

    private static void validateString(
            String name, Object value, StringSchema schema, Map<String, String> errors) {
        if (!(value instanceof String str)) {
            errors.put(name, "expected string");
            return;
        }
        schema.minLength().ifPresent(min -> {
            if (str.length() < min) errors.put(name, "minLength " + min);
        });
        schema.maxLength().ifPresent(max -> {
            if (str.length() > max) errors.put(name, "maxLength " + max);
        });
    }

    private static void validateNumber(
            String name, Object value, NumberSchema schema, Map<String, String> errors) {
        double num;
        if (value instanceof Number n) {
            num = n.doubleValue();
        } else if (value instanceof String s) {
            try { num = Double.parseDouble(s); } catch (NumberFormatException ex) {
                errors.put(name, "expected number");
                return;
            }
        } else {
            errors.put(name, "expected number");
            return;
        }
        schema.minimum().ifPresent(min -> {
            if (num < min) errors.put(name, "minimum " + min);
        });
        schema.maximum().ifPresent(max -> {
            if (num > max) errors.put(name, "maximum " + max);
        });
    }

    private static void validateBoolean(
            String name, Object value, BooleanSchema schema, Map<String, String> errors) {
        if (value instanceof Boolean) return;
        if (value instanceof String s && (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false"))) return;
        errors.put(name, "expected boolean");
    }

    private static void validateEnum(
            String name, Object value, EnumSchema schema, Map<String, String> errors) {
        String str = (value instanceof String s) ? s : null;
        if (str == null) {
            errors.put(name, "expected string");
            return;
        }
        if (!schema.values().contains(str)) errors.put(name, "expected one of " + schema.values());
    }
}
