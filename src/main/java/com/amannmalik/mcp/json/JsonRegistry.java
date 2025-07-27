package com.amannmalik.mcp.json;

import jakarta.json.JsonObject;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class JsonRegistry {
    private static final Map<Class<?>, JsonCodec<?>> BY_TYPE = new ConcurrentHashMap<>();
    private static final Map<String, JsonCodec<?>> BY_NAME = new ConcurrentHashMap<>();

    private JsonRegistry() {}

    public static <T> void register(Class<T> type, JsonCodec<T> codec) {
        BY_TYPE.put(type, codec);
    }

    public static <T> void register(String name, JsonCodec<? extends T> codec) {
        BY_NAME.put(name, codec);
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<JsonCodec<T>> codec(Class<T> type) {
        return Optional.ofNullable((JsonCodec<T>) BY_TYPE.get(type));
    }

    public static Optional<JsonCodec<?>> codec(String name) {
        return Optional.ofNullable(BY_NAME.get(name));
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<T> fromUnion(JsonObject obj) {
        return Optional.ofNullable(obj.getString("type", null))
                .flatMap(JsonRegistry::codec)
                .map(c -> (T) c.fromJson(obj));
    }

    public static JsonObject toUnion(Object value) {
        @SuppressWarnings("unchecked")
        JsonCodec<Object> codec = (JsonCodec<Object>) BY_TYPE.get(value.getClass());
        if (codec == null) throw new IllegalArgumentException("No codec for " + value.getClass());
        return (JsonObject) codec.toJson(value);
    }
}
