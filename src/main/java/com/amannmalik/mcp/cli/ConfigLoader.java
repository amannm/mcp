package com.amannmalik.mcp.cli;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class ConfigLoader {
    private ConfigLoader() {}

    public static JsonObject load(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            String name = path.getFileName().toString().toLowerCase();
            if (name.endsWith(".json")) return readJson(in);
            if (name.endsWith(".yaml") || name.endsWith(".yml")) return readYaml(in);
            throw new IllegalArgumentException("Unsupported config: " + path);
        }
    }

    private static JsonObject readJson(InputStream in) throws IOException {
        try (JsonReader r = Json.createReader(in)) {
            return r.readObject();
        }
    }

    private static JsonObject readYaml(InputStream in) {
        Object data = new Yaml().load(in);
        if (!(data instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("YAML root must be a mapping");
        }
        return toObject(map);
    }

    private static JsonObject toObject(Map<?, ?> map) {
        JsonObjectBuilder b = Json.createObjectBuilder();
        for (Map.Entry<?, ?> e : map.entrySet()) {
            b.add(String.valueOf(e.getKey()), toValue(e.getValue()));
        }
        return b.build();
    }

    private static JsonValue toValue(Object v) {
        if (v == null) return JsonValue.NULL;
        if (v instanceof Map<?, ?> m) return toObject(m);
        if (v instanceof List<?> l) {
            JsonArrayBuilder a = Json.createArrayBuilder();
            for (Object o : l) a.add(toValue(o));
            return a.build();
        }
        if (v instanceof Boolean b) return b ? JsonValue.TRUE : JsonValue.FALSE;
        if (v instanceof Integer i) return Json.createValue(i);
        if (v instanceof Long l) return Json.createValue(l);
        if (v instanceof Double d) return Json.createValue(d);
        if (v instanceof Number n) return Json.createValue(n.doubleValue());
        return Json.createReader(new StringReader(v.toString())).readValue();
    }
}
