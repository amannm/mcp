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
import java.util.Map;

public final class ConfigLoader {
    private ConfigLoader() {}

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
                if (!(data instanceof Map<?,?> map)) throw new IllegalArgumentException("invalid yaml");
                return parseMap(map);
            }
        }
        throw new IllegalArgumentException("Unsupported config: " + name);
    }

    private static CliConfig parseJson(JsonObject obj) {
        String mode = obj.getString("mode");
        String transport = obj.getString("transport", "stdio");
        return switch (mode) {
            case "server" -> new ServerConfig(parseTransport(transport), obj.getInt("port", 0));
            case "client" -> new ClientConfig(parseTransport(transport), obj.getString("command"));
            default -> throw new IllegalArgumentException("Invalid mode: " + mode);
        };
    }

    private static CliConfig parseMap(Map<?,?> map) {
        String mode = map.get("mode").toString();
        Object tVal = map.get("transport");
        String transport = tVal == null ? "stdio" : tVal.toString();
        Object portVal = map.get("port");
        Object cmdVal = map.get("command");
        return switch (mode) {
            case "server" -> new ServerConfig(parseTransport(transport), portVal == null ? 0 : ((Number) portVal).intValue());
            case "client" -> new ClientConfig(parseTransport(transport), cmdVal.toString());
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
}
