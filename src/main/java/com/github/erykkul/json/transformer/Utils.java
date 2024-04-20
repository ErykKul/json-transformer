package com.github.erykkul.json.transformer;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonPointer;
import jakarta.json.JsonValue;

public class Utils {

    public static JsonObject fixTargetPath(final JsonObject to, final JsonValue.ValueType t, final String jsonPointer) {
        final String[] fields = jsonPointer.split("/");
        JsonObject result = to;
        String path = "";
        for (int i = 0; i < fields.length; i++) {
            if (!"".equals(fields[i])) {
                path = path + "/" + fields[i];
            }
            final JsonPointer pointer = Json.createPointer(path);
            if (!pointer.containsValue(result)) {
                if (i < fields.length - 1 || JsonValue.ValueType.OBJECT.equals(t)) {
                    result = pointer.add(result, JsonValue.EMPTY_JSON_OBJECT);
                } else {
                    result = pointer.add(result, JsonValue.EMPTY_JSON_ARRAY);
                }
            }
        }
        return result;
    }
}
