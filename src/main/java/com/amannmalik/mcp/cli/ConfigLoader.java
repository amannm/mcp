package com.amannmalik.mcp.cli;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
                Object data = loader.loadFromInputStream(in);
                if (!(data instanceof Map<?, ?> raw)) throw new IllegalArgumentException("invalid yaml");
                var map = new java.util.LinkedHashMap<String, Object>();
                for (var e : raw.entrySet()) {
                    map.put(e.getKey().toString(), e.getValue());
                }
                return parseMap(map);
            }
        }
        throw new IllegalArgumentException("Unsupported config: " + name);
    }

    private static CliConfig parseJson(JsonObject obj) {
        String mode = obj.getString("mode");
        String transport = obj.getString("transport", "stdio");
        return switch (mode) {
            case "server" -> new ServerConfig(
                    parseTransport(transport),
                    obj.getInt("port", 0),
                    obj.containsKey("instructions") ? obj.getString("instructions") : null,
                    obj.containsKey("expectedAudience") ? obj.getString("expectedAudience") : null,
                    obj.containsKey("resourceMetadataUrl") ? obj.getString("resourceMetadataUrl") : null,
                    obj.containsKey("authorizationServers")
                            ? obj.getJsonArray("authorizationServers")
                            .getValuesAs(jakarta.json.JsonString.class)
                            .stream().map(jakarta.json.JsonString::getString).toList()
                            : List.of());
            case "client" -> new ClientConfig(parseTransport(transport), obj.getString("command"));
            case "host" -> {
                var cObj = obj.getJsonObject("clients");
                if (cObj == null || cObj.isEmpty()) throw new IllegalArgumentException("clients required");
                yield new HostConfig(cObj.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString())));
            }
            default -> throw new IllegalArgumentException("Invalid mode: " + mode);
        };
    }

    private static CliConfig parseMap(Map<String, ?> map) {
        String mode = map.get("mode").toString();
        Object tVal = map.get("transport");
        String transport = tVal == null ? "stdio" : tVal.toString();
        Object portVal = map.get("port");
        Object cmdVal = map.get("command");
        Object clientsVal = map.get("clients");
        return switch (mode) {
            case "server" -> new ServerConfig(
                    parseTransport(transport),
                    portVal == null ? 0 : ((Number) portVal).intValue(),
                    map.get("instructions") == null ? null : map.get("instructions").toString(),
                    map.get("expectedAudience") == null ? null : map.get("expectedAudience").toString(),
                    map.get("resourceMetadataUrl") == null ? null : map.get("resourceMetadataUrl").toString(),
                    parseAuthServers(map.get("authorizationServers")));
            case "client" -> new ClientConfig(parseTransport(transport), cmdVal.toString());
            case "host" -> {
                if (!(clientsVal instanceof Map<?, ?> cMap) || cMap.isEmpty()) {
                    throw new IllegalArgumentException("clients required");
                }
                yield new HostConfig(cMap.entrySet().stream()
                        .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString())));
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

    private static List<String> parseAuthServers(Object val) {
        if (val == null) return List.of();
        if (val instanceof Iterable<?> it) {
            List<String> list = new java.util.ArrayList<>();
            for (Object o : it) list.add(o.toString());
            return List.copyOf(list);
        }
        return List.of(val.toString());
    }
}
