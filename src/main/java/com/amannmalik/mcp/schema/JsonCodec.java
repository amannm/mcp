package com.amannmalik.mcp.schema;

import jakarta.json.*;

import java.lang.reflect.*;
import java.util.*;

/**
 * Jakarta JSON-P based serialization utilities.
 */
public final class JsonCodec {
    private JsonCodec() {}

    public static JsonObject toJson(JsonRpcTypes.JsonRpcMessage message) {
        return switch (message) {
            case BaseProtocol.Request r -> requestToJson(r);
            case BaseProtocol.Result r -> resultToJson(r);
            case BaseProtocol.Notification n -> notificationToJson(n);
            case JsonRpcTypes.JsonRpcError e -> errorToJson(e);
        };
    }

    public static <T extends JsonRpcTypes.JsonRpcMessage> T fromJson(JsonObject json, Class<T> type) {
        if (BaseProtocol.Request.class.isAssignableFrom(type))
            return type.cast(jsonToRequest(json, type.asSubclass(BaseProtocol.Request.class)));
        if (BaseProtocol.Result.class.isAssignableFrom(type))
            return type.cast(jsonToResult(json, type.asSubclass(BaseProtocol.Result.class)));
        if (BaseProtocol.Notification.class.isAssignableFrom(type))
            return type.cast(jsonToNotification(json, type.asSubclass(BaseProtocol.Notification.class)));
        if (JsonRpcTypes.JsonRpcError.class.isAssignableFrom(type))
            return type.cast(jsonToError(json));
        throw new IllegalArgumentException("Unsupported message type: " + type);
    }

    public static JsonObject requestToJson(BaseProtocol.Request request) {
        JsonObject obj = encodeRecord((Record) request);
        return Json.createObjectBuilder(obj)
                .add("jsonrpc", JsonRpcTypes.JSONRPC_VERSION)
                .add("method", request.method())
                .add("id", encodeValue(request.id().raw()))
                .build();
    }

    public static JsonObject resultToJson(BaseProtocol.Result result) {
        JsonObject obj = encodeRecord((Record) result);
        return Json.createObjectBuilder(obj)
                .add("jsonrpc", JsonRpcTypes.JSONRPC_VERSION)
                .add("id", encodeValue(result.id().raw()))
                .build();
    }

    public static JsonObject notificationToJson(BaseProtocol.Notification notification) {
        JsonObject obj = encodeRecord((Record) notification);
        return Json.createObjectBuilder(obj)
                .add("jsonrpc", JsonRpcTypes.JSONRPC_VERSION)
                .add("method", notification.method())
                .build();
    }

    private static JsonObject errorToJson(JsonRpcTypes.JsonRpcError error) {
        return Json.createObjectBuilder()
                .add("jsonrpc", JsonRpcTypes.JSONRPC_VERSION)
                .add("id", encodeValue(error.id().raw()))
                .add("code", error.code())
                .add("message", error.message())
                .build();
    }

    private static <T extends BaseProtocol.Request> T jsonToRequest(JsonObject json, Class<T> type) {
        return type.cast(decodeRecord(json, (Class<? extends Record>) type));
    }

    private static <T extends BaseProtocol.Result> T jsonToResult(JsonObject json, Class<T> type) {
        return type.cast(decodeRecord(json, (Class<? extends Record>) type));
    }

    private static <T extends BaseProtocol.Notification> T jsonToNotification(JsonObject json, Class<T> type) {
        return type.cast(decodeRecord(json, (Class<? extends Record>) type));
    }

    private static JsonRpcTypes.JsonRpcError jsonToError(JsonObject json) {
        return new JsonRpcTypes.BasicError(
                jsonRequestId(json.get("id")),
                json.getInt("code"),
                json.getString("message")
        );
    }

    private static JsonRpcTypes.RequestId jsonRequestId(JsonValue value) {
        return switch (value.getValueType()) {
            case STRING -> new JsonRpcTypes.StringRequestId(((JsonString) value).getString());
            case NUMBER -> new JsonRpcTypes.NumberRequestId(((JsonNumber) value).longValue());
            default -> throw new IllegalArgumentException("invalid id type");
        };
    }

    private static BaseProtocol.ProgressToken jsonProgressToken(JsonValue value) {
        return switch (value.getValueType()) {
            case STRING -> new BaseProtocol.StringProgressToken(((JsonString) value).getString());
            case NUMBER -> new BaseProtocol.NumberProgressToken(((JsonNumber) value).longValue());
            default -> throw new IllegalArgumentException("invalid progress token type");
        };
    }

    private static JsonValue encodeValue(Object v) {
        if (v == null) return JsonValue.NULL;
        if (v instanceof Optional<?> o) return o.map(JsonCodec::encodeValue).orElse(JsonValue.NULL);
        if (v instanceof String s) return Json.createValue(s);
        if (v instanceof Integer i) return Json.createValue(i);
        if (v instanceof Long l) return Json.createValue(l);
        if (v instanceof Double d) return Json.createValue(d);
        if (v instanceof Boolean b) return b ? JsonValue.TRUE : JsonValue.FALSE;
        if (v instanceof Enum<?> e) return Json.createValue(e.name());
        if (v instanceof JsonRpcTypes.RequestId id) return encodeValue(id.raw());
        if (v instanceof BaseProtocol.ProgressToken t) return encodeValue(t.raw());
        if (v instanceof Map<?,?> m) {
            JsonObjectBuilder b = Json.createObjectBuilder();
            m.forEach((k, val) -> b.add(String.valueOf(k), encodeValue(val)));
            return b.build();
        }
        if (v instanceof List<?> list) {
            JsonArrayBuilder b = Json.createArrayBuilder();
            list.forEach(elem -> b.add(encodeValue(elem)));
            return b.build();
        }
        if (v instanceof byte[] arr) {
            return Json.createValue(Base64.getEncoder().encodeToString(arr));
        }
        if (v instanceof Record r) return encodeRecord(r);
        throw new IllegalArgumentException("Unsupported value type: " + v.getClass());
    }

    private static Object decodeValue(JsonValue v, Class<?> raw, Type generic) {
        if (raw == Optional.class) {
            ParameterizedType pt = (ParameterizedType) generic;
            Class<?> arg = (Class<?>) pt.getActualTypeArguments()[0];
            if (v == null || v.getValueType() == JsonValue.ValueType.NULL) return Optional.empty();
            return Optional.ofNullable(decodeValue(v, arg, arg));
        }
        if (v == null || v.getValueType() == JsonValue.ValueType.NULL) return null;
        if (raw == String.class) return ((JsonString) v).getString();
        if (raw == int.class || raw == Integer.class) return ((JsonNumber) v).intValue();
        if (raw == long.class || raw == Long.class) return ((JsonNumber) v).longValue();
        if (raw == double.class || raw == Double.class) return ((JsonNumber) v).doubleValue();
        if (raw == boolean.class || raw == Boolean.class) return v == JsonValue.TRUE || ((JsonValue) v).equals(JsonValue.TRUE);
        if (Enum.class.isAssignableFrom(raw)) return Enum.valueOf(raw.asSubclass(Enum.class), ((JsonString) v).getString());
        if (JsonRpcTypes.RequestId.class.isAssignableFrom(raw)) return jsonRequestId(v);
        if (BaseProtocol.ProgressToken.class.isAssignableFrom(raw)) return jsonProgressToken(v);
        if (List.class.isAssignableFrom(raw)) {
            ParameterizedType pt = (ParameterizedType) generic;
            Type elemT = pt.getActualTypeArguments()[0];
            Class<?> elemC = (Class<?>) (elemT instanceof ParameterizedType p ? p.getRawType() : elemT);
            List<Object> list = new ArrayList<>();
            for (JsonValue ev : v.asJsonArray()) list.add(decodeValue(ev, elemC, elemT));
            return list;
        }
        if (Map.class.isAssignableFrom(raw)) {
            ParameterizedType pt = (ParameterizedType) generic;
            Type valT = pt.getActualTypeArguments()[1];
            Class<?> valC = (Class<?>) (valT instanceof ParameterizedType p ? p.getRawType() : valT);
            Map<String, Object> map = new LinkedHashMap<>();
            for (var e : v.asJsonObject().entrySet()) map.put(e.getKey(), decodeValue(e.getValue(), valC, valT));
            return map;
        }
        if (raw == byte[].class) return Base64.getDecoder().decode(((JsonString) v).getString());
        if (Record.class.isAssignableFrom(raw)) return decodeRecord(v.asJsonObject(), raw.asSubclass(Record.class));
        throw new IllegalArgumentException("Unsupported type: " + raw);
    }

    private static <T extends Record> T decodeRecord(JsonObject obj, Class<T> type) {
        try {
            RecordComponent[] comps = type.getRecordComponents();
            Object[] args = new Object[comps.length];
            for (int i = 0; i < comps.length; i++) {
                var c = comps[i];
                args[i] = decodeValue(obj.get(c.getName()), c.getType(), c.getGenericType());
            }
            Class<?>[] paramTypes = Arrays.stream(comps).map(RecordComponent::getType).toArray(Class[]::new);
            @SuppressWarnings("unchecked")
            Constructor<T> ctor = (Constructor<T>) type.getDeclaredConstructor(paramTypes);
            return ctor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static JsonObject encodeRecord(Record record) {
        JsonObjectBuilder b = Json.createObjectBuilder();
        for (var c : record.getClass().getRecordComponents()) {
            try {
                JsonValue v = encodeValue(c.getAccessor().invoke(record));
                if (v == JsonValue.NULL) b.addNull(c.getName());
                else b.add(c.getName(), v);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
        return b.build();
    }
}
