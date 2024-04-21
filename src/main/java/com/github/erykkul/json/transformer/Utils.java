package com.github.erykkul.json.transformer;

import jakarta.json.Json;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.json.JsonValue.ValueType;

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
                if (i < fields.length - 1 || JsonValue.ValueType.OBJECT.equals(t)) {
                    result = add(result, path, JsonValue.EMPTY_JSON_OBJECT);
                } else {
                    result = add(result, path, JsonValue.EMPTY_JSON_ARRAY);
                }
            }
        }
        return result;
    }

    public static boolean isEmpty(final JsonValue value) {
        return value == null || JsonValue.EMPTY_JSON_ARRAY.equals(value) || JsonValue.EMPTY_JSON_OBJECT.equals(value)
                || JsonValue.NULL.equals(value);
    }

    public static JsonValue getValue(final JsonValue from, final String pointer) {
        if (!containsValue(from, pointer)) {
            return JsonValue.EMPTY_JSON_OBJECT;
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
        return Json.createPointer(at).add(toJsonStructure(in), value);
    }

    public static boolean containsValue(final JsonValue in, final String at) {
        if (isEmpty(in)) {
            return false;
        }
        return Json.createPointer(at).containsValue(toJsonStructure(in));
    }

    private static JsonStructure toJsonStructure(final JsonValue in) {
        if (ValueType.ARRAY.equals(in.getValueType())) {
            return in.asJsonArray();
        }
        if (ValueType.OBJECT.equals(in.getValueType())) {
            return in.asJsonObject();
        }
        return JsonValue.EMPTY_JSON_OBJECT;
    }
}
