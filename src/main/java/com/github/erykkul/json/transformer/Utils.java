package com.github.erykkul.json.transformer;

import static jakarta.json.JsonValue.EMPTY_JSON_ARRAY;
import static jakarta.json.JsonValue.EMPTY_JSON_OBJECT;
import static jakarta.json.JsonValue.NULL;
import static jakarta.json.JsonValue.ValueType.ARRAY;
import static jakarta.json.JsonValue.ValueType.OBJECT;

import java.util.stream.Stream;

import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;

public class Utils {

    public static JsonValue fixTargetPath(final JsonValue to, final JsonValue.ValueType t, final String jsonPointer) {
        final String[] fields = jsonPointer.split("/");
        JsonValue result = to;
        String path = "";
        for (int i = 0; i < fields.length; i++) {
            if (!"".equals(fields[i])) {
                path = path + "/" + fields[i];
            }
            if (!containsValue(result, path)) {
                if (i < fields.length - 1 || OBJECT.equals(t)) {
                    result = add(result, path, EMPTY_JSON_OBJECT);
                } else {
                    result = add(result, path, EMPTY_JSON_ARRAY);
                }
            }
        }
        return result;
    }

    public static boolean isEmpty(final JsonValue value) {
        return value == null || EMPTY_JSON_ARRAY.equals(value) || EMPTY_JSON_OBJECT.equals(value) || NULL.equals(value);
    }

    public static JsonValue getValue(final JsonValue from, final String pointer) {
        if (!containsValue(from, pointer)) {
            return EMPTY_JSON_OBJECT;
        }
        return Json.createPointer(pointer).getValue(toJsonStructure(from));
    }

    public static JsonValue replace(final JsonValue in, final String at, final JsonValue with) {
        if (!containsValue(in, at)) {
            return add(in, at, with);
        }
        return Json.createPointer(at).replace(toJsonStructure(in), with);
    }

    public static JsonValue add(final JsonValue in, final String at, final JsonValue value) {
        try {
            return Json.createPointer(at).add(toJsonStructure(in), value);
        } catch (final JsonException e) {
            return in;
        }
    }

    public static JsonValue remove(final JsonValue in, final String at) {
        if (!containsValue(in, at)) {
            return in;
        }
        return Json.createPointer(at).remove(toJsonStructure(in));
    }

    public static Stream<JsonValue> stream(final JsonValue in) {
        if (!isEmpty(in)) {
            return Stream.empty();
        }
        if (isArray(in)) {
            return in.asJsonArray().stream();
        }
        if (isObject(in)) {
            in.asJsonObject().values().stream();
        }
        return Stream.of(in);
    }

    public static boolean containsValue(final JsonValue in, final String at) {
        if (isEmpty(in)) {
            return false;
        }
        try {
            return Json.createPointer(at).containsValue(toJsonStructure(in));
        } catch (final JsonException e) {
            return false;
        }
    }

    public static boolean isArray(final JsonValue js) {
        return ARRAY.equals(js.getValueType());
    }

    public static boolean isObject(final JsonValue js) {
        return OBJECT.equals(js.getValueType());
    }

    private static JsonStructure toJsonStructure(final JsonValue in) {
        if (isArray(in)) {
            return in.asJsonArray();
        }
        if (isObject(in)) {
            return in.asJsonObject();
        }
        return EMPTY_JSON_OBJECT;
    }
}
