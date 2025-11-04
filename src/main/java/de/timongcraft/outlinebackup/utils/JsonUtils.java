package de.timongcraft.outlinebackup.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import org.jetbrains.annotations.Nullable;

public final class JsonUtils {

    public static @Nullable String getNullableString(JsonObject json, String key) {
        JsonElement elem = getNullable(json, key);
        return elem == null ? null : elem.getAsString();
    }

    public static String getRequiredString(JsonObject json, String key) {
        JsonElement elem = getNullable(json, key);
        if (elem == null) throw missingField(json, key);
        try {
            return elem.getAsString();
        } catch (ClassCastException | IllegalStateException e) {
            throw invalidFieldType(json, key, "string", e);
        }
    }

    public static @Nullable Long getNullableLong(JsonObject json, String key) {
        JsonElement elem = getNullable(json, key);
        if (elem == null) return null;
        try {
            return elem.getAsLong();
        } catch (ClassCastException | IllegalStateException e) {
            throw invalidFieldType(json, key, "long", e);
        }
    }

    public static long getRequiredLong(JsonObject json, String key) {
        Long value = getNullableLong(json, key);
        if (value == null) throw missingField(json, key);
        return value;
    }

    public static @Nullable Boolean getNullableBoolean(JsonObject json, String key) {
        JsonElement elem = getNullable(json, key);
        if (elem == null) return null;
        try {
            return elem.getAsBoolean();
        } catch (ClassCastException | IllegalStateException e) {
            throw invalidFieldType(json, key, "boolean", e);
        }
    }

    public static boolean getRequiredBoolean(JsonObject json, String key) {
        Boolean value = getNullableBoolean(json, key);
        if (value == null) throw missingField(json, key);
        return value;
    }

    public static JsonObject getNullableObject(JsonObject json, String key) {
        JsonElement elem = getNullable(json, key);
        if (elem == null) return null;
        if (!elem.isJsonObject()) throw invalidFieldType(json, key, "object", null);
        return elem.getAsJsonObject();
    }

    public static JsonObject getRequiredObject(JsonObject json, String key) {
        JsonObject obj = getNullableObject(json, key);
        if (obj == null) throw missingField(json, key);
        return obj;
    }

    public static @Nullable Instant getNullableInstant(JsonObject json, String key) {
        String s = getNullableString(json, key);
        if (s == null) return null;
        try {
            return Instant.parse(s);
        } catch (DateTimeParseException e) {
            throw invalidFieldType(json, key, "ISO-8601 instant", e);
        }
    }

    public static Instant getRequiredInstant(JsonObject json, String key) {
        Instant i = getNullableInstant(json, key);
        if (i == null) throw missingField(json, key);
        return i;
    }

    private static JsonElement getNullable(JsonObject json, String key) {
        if (!json.has(key)) return null;
        JsonElement elem = json.get(key);
        if (elem == null || elem.isJsonNull()) return null;
        return elem;
    }

    private static IllegalArgumentException missingField(JsonObject json, String key) {
        return new IllegalArgumentException("Required field '" + key + "' is missing in JSON: " + json);
    }

    private static IllegalArgumentException invalidFieldType(JsonObject json, String key, String expectedType, Throwable cause) {
        String msg = "Invalid type for field '" + key + "' (expected " + expectedType + ") in JSON: " + json;
        return cause == null ? new IllegalArgumentException(msg) : new IllegalArgumentException(msg, cause);
    }

    private JsonUtils() {}

}