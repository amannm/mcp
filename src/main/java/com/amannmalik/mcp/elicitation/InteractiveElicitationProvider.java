package com.amannmalik.mcp.elicitation;

import com.amannmalik.mcp.spi.*;
import jakarta.json.*;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

public final class InteractiveElicitationProvider implements ElicitationProvider {

    private final BufferedReader reader;

    public InteractiveElicitationProvider() {
        this.reader = new BufferedReader(new InputStreamReader(System.in));
    }

    @Override
    public ElicitResult elicit(ElicitRequest request, long timeoutMillis) throws InterruptedException {
        try {
            System.err.println("\n=== MCP Elicitation Request ===");
            System.err.println(request.message());
            System.err.println();

            var schema = request.requestedSchema();
            var props = schema.getJsonObject("properties");
            var req = schema.getJsonArray("required");
            Set<String> required = new HashSet<>();
            if (req != null) {
                for (var s : req.getValuesAs(JsonString.class)) {
                    required.add(s.getString());
                }
            }

            var content = Json.createObjectBuilder();

            for (var entry : props.entrySet()) {
                var name = entry.getKey();
                var prop = entry.getValue().asJsonObject();
                var type = prop.getString("type");
                var isRequired = required.contains(name);
                System.err.print(name + " (" + type + (isRequired ? ", required" : "") + ")");
                if (prop.containsKey("description")) {
                    System.err.print(" - " + prop.getString("description"));
                }
                if (prop.containsKey("enum")) {
                    System.err.print(" options: " + prop.getJsonArray("enum"));
                }
                System.err.println();
                var defaultVal = prop.get("default");
                while (true) {
                    System.err.print(name + ": ");
                    var line = reader.readLine();
                    if (line == null) {
                        return new ElicitResult(ElicitationAction.CANCEL, null, null);
                    }
                    line = line.trim();
                    if (line.isEmpty()) {
                        if (defaultVal != null) {
                            content.add(name, defaultVal);
                            break;
                        }
                        if (isRequired) {
                            System.err.println("Value required");
                            continue;
                        }
                        break;
                    }
                    try {
                        var val = parseValue(type, line);
                        if (prop.containsKey("enum")) {
                            var allowed = false;
                            for (var v : prop.getJsonArray("enum").getValuesAs(JsonString.class)) {
                                if (v.getString().equals(line)) {
                                    allowed = true;
                                    break;
                                }
                            }
                            if (!allowed) {
                                System.err.println("Invalid option");
                                continue;
                            }
                        }
                        content.add(name, val);
                        break;
                    } catch (IllegalArgumentException e) {
                        System.err.println("Invalid value: " + e.getMessage());
                    }
                }
            }

            System.err.print("Action accept (a)/decline (d)/cancel (c): ");
            var act = reader.readLine();
            if (act == null) {
                return new ElicitResult(ElicitationAction.CANCEL, null, null);
            }
            act = act.trim().toLowerCase();
            if (act.startsWith("a")) {
                return new ElicitResult(ElicitationAction.ACCEPT, content.build(), null);
            }
            if (act.startsWith("d")) {
                return new ElicitResult(ElicitationAction.DECLINE, null, null);
            }
            return new ElicitResult(ElicitationAction.CANCEL, null, null);
        } catch (IOException e) {
            throw new InterruptedException("IO error during elicitation: " + e.getMessage());
        }
    }

    private JsonValue parseValue(String type, String input) {
        return switch (type) {
            case "string" -> Json.createValue(input);
            case "integer" -> {
                try {
                    yield Json.createValue(Long.parseLong(input));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("integer required");
                }
            }
            case "number" -> {
                try {
                    yield Json.createValue(Double.parseDouble(input));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("number required");
                }
            }
            case "boolean" -> {
                if ("true".equalsIgnoreCase(input)) {
                    yield JsonValue.TRUE;
                } else if ("false".equalsIgnoreCase(input)) {
                    yield JsonValue.FALSE;
                } else {
                    throw new IllegalArgumentException("boolean required");
                }
            }
            default -> throw new IllegalArgumentException("unsupported type: " + type);
        };
    }
}
