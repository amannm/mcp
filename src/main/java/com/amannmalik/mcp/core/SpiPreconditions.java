package com.amannmalik.mcp.core;

import com.amannmalik.mcp.codec.AnnotationsJsonCodec;
import com.amannmalik.mcp.spi.Annotations;
import com.amannmalik.mcp.util.Immutable;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Centralises contract enforcement for the exported SPI so that the public API stays declarative.
 */
public final class SpiPreconditions {

    private SpiPreconditions() {
    }

    public static <T> T requireNonNull(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    public static void requireAllNonNull(String message, Object... values) {
        if (values == null) {
            throw new IllegalArgumentException(message);
        }
        for (var value : values) {
            if (value == null) {
                throw new IllegalArgumentException(message);
            }
        }
    }

    public static String requireClean(String value) {
        return ValidationUtil.requireClean(value);
    }

    public static String cleanNullable(String value) {
        return ValidationUtil.cleanNullable(value);
    }

    public static <T> List<T> immutableList(Collection<? extends T> items) {
        return Immutable.list(items);
    }

    public static <T> Set<T> immutableSet(Set<? extends T> items) {
        return Immutable.set(items);
    }

    public static <E extends Enum<E>> Set<E> immutableEnumSet(Collection<? extends E> items) {
        return Immutable.enumSet(items);
    }

    public static Map<String, String> cleanMap(Map<String, String> map) {
        return ValidationUtil.requireCleanMap(map);
    }

    public static void requireMeta(JsonObject meta) {
        ValidationUtil.requireMeta(meta);
    }

    public static int requirePositive(int value, String field) {
        return ValidationUtil.requirePositive(value, field);
    }

    public static long requireNonNegative(long value, String field) {
        return ValidationUtil.requireNonNegative(value, field);
    }

    public static int requireNonNegative(int value, String field) {
        return ValidationUtil.requireNonNegative(value, field);
    }

    public static long requirePositive(long value, String field) {
        return ValidationUtil.requirePositive(value, field);
    }

    public static double requireFraction(double value, String field) {
        return ValidationUtil.requireFraction(value, field);
    }

    public static Double fractionOrNull(Double value, String field) {
        return value == null ? null : requireFraction(value, field);
    }

    public static Integer nonNegativeOrNull(Integer value, String field) {
        return value == null ? null : requireNonNegative(value, field);
    }

    public static Long nonNegativeOrNull(Long value, String field) {
        return value == null ? null : requireNonNegative(value, field);
    }

    public static URI requireAbsoluteUri(URI uri) {
        return ValidationUtil.requireAbsoluteUri(uri);
    }

    public static URI requireFileUri(URI uri) {
        return ValidationUtil.requireFileUri(uri);
    }

    public static String requireAbsoluteTemplate(String template) {
        return ValidationUtil.requireAbsoluteTemplate(template);
    }

    public static byte[] requireData(byte[] data, String field) {
        if (data == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return data.clone();
    }

    public static byte[] clone(byte[] data) {
        return data == null ? null : data.clone();
    }

    public static List<String> cleanStringList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(ValidationUtil::requireClean)
                .toList();
    }

    public static <T> List<T> copyList(List<T> values) {
        return List.copyOf(values);
    }

    public static <T> Set<T> copySet(Set<T> values) {
        return Set.copyOf(values);
    }

    public static Map<String, String> copyMap(Map<String, String> values) {
        return Map.copyOf(values);
    }

    public static Annotations annotationsOrEmpty(Annotations value) {
        return Objects.requireNonNullElse(value, AnnotationsJsonCodec.EMPTY);
    }

    public static <T> T defaultIfNull(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }
}
