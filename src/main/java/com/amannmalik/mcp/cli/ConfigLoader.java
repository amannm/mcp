package com.amannmalik.mcp.cli;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.math.BigDecimal;

public final class ConfigLoader {
    private ConfigLoader() {
    }

    public static CliConfig load(Path path) throws IOException {
        String name = path.getFileName().toString().toLowerCase();
        try (InputStream in = Files.newInputStream(path)) {
            if (name.endsWith(".json")) {
                JsonReader r = Json.createReader(in);
                JsonObject obj = r.readObject();
                return parseJson(obj);
            }
            if (name.endsWith(".yaml") || name.endsWith(".yml")) {
                Load loader = new Load(LoadSettings.builder().build());
                JsonValue value = toJsonValue(loader.loadFromInputStream(in));
                if (!(value instanceof JsonObject obj)) throw new IllegalArgumentException("invalid yaml");
                return parseJson(obj);
            }
        }
        throw new IllegalArgumentException("Unsupported config: " + name);
    }

    private static CliConfig parseJson(JsonObject obj) {
        String mode = obj.getString("mode");
        String transport = obj.getString("transport", "stdio");
        List<String> authServers = switch (obj.get("authorizationServers")) {
            case JsonArray arr -> arr.getValuesAs(JsonString.class)
                    .stream().map(JsonString::getString).toList();
            case JsonString s -> List.of(s.getString());
            case null -> List.of();
            default -> throw new IllegalArgumentException("authorizationServers must be string or array");
        };
        return switch (mode) {
            case "server" -> new ServerConfig(
                    parseTransport(transport),
                    obj.getInt("port", 0),
                    obj.containsKey("instructions") ? obj.getString("instructions") : null,
                    obj.containsKey("expectedAudience") ? obj.getString("expectedAudience") : null,
                    obj.containsKey("resourceMetadataUrl") ? obj.getString("resourceMetadataUrl") : null,
                    authServers);
            case "client" -> new ClientConfig(parseTransport(transport), obj.getString("command"));
            case "host" -> {
                var cObj = obj.getJsonObject("clients");
                if (cObj == null || cObj.isEmpty()) throw new IllegalArgumentException("clients required");
                yield new HostConfig(cObj.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                            var v = e.getValue();
                            if (!(v instanceof JsonString s)) {
                                throw new IllegalArgumentException("client command must be string");
                            }
                            return s.getString();
                        })));
            }
            default -> throw new IllegalArgumentException("Invalid mode: " + mode);
        };
    }

    private static TransportType parseTransport(String name) {
        return switch (name) {
            case "stdio" -> TransportType.STDIO;
            case "http" -> TransportType.HTTP;
            default -> throw new IllegalArgumentException("Unknown transport: " + name);
        };
    }

    private static JsonValue toJsonValue(Object value) {
        return switch (value) {
            case Map<?, ?> m -> {
                var b = Json.createObjectBuilder();
                for (var e : m.entrySet()) {
                    b.add(e.getKey().toString(), toJsonValue(e.getValue()));
                }
                yield b.build();
            }
            case List<?> l -> {
                JsonArrayBuilder b = Json.createArrayBuilder();
                for (var item : l) b.add(toJsonValue(item));
                yield b.build();
            }
            case String s -> Json.createValue(s);
            case Number n -> Json.createValue(new BigDecimal(n.toString()));
            case Boolean b -> b ? JsonValue.TRUE : JsonValue.FALSE;
            case null -> JsonValue.NULL;
            default -> Json.createValue(value.toString());
        };
    }
}
