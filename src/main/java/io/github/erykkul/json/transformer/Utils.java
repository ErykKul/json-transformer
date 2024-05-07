// Author: Eryk Kulikowski (2024). Apache 2.0 License

package io.github.erykkul.json.transformer;

import static jakarta.json.JsonValue.EMPTY_JSON_ARRAY;
import static jakarta.json.JsonValue.EMPTY_JSON_OBJECT;
import static jakarta.json.JsonValue.NULL;
import static jakarta.json.JsonValue.ValueType.ARRAY;
import static jakarta.json.JsonValue.ValueType.OBJECT;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonException;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.json.JsonValue.ValueType;

public class Utils {

    private static final Logger logger = Logger.getLogger(Utils.class.getName());

    public static JsonValue fixPath(final JsonValue in, final JsonValue.ValueType t, final String jsonPointer) {
        final String[] fields = jsonPointer.split("/");
        JsonValue result = in;
        String path = "";
        for (int i = 0; i < fields.length; i++) {
            if (!"".equals(fields[i])) {
                path = path + "/" + fields[i];
            }
            if (notContainsValue(result, path)) {
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

    public static JsonValue getValue(final JsonValue source, final String pointer) {
        if ("".equals(pointer)) {
            return source;
        }
        if (notContainsValue(source, pointer)) {
            return EMPTY_JSON_OBJECT;
        }
        return Json.createPointer(pointer).getValue(asJsonStructure(source));
    }

    public static JsonValue replace(final JsonValue in, final String at, final JsonValue with) {
        if ("".equals(at)) {
            return with;
        }
        if (notContainsValue(in, at)) {
            return add(in, at, with);
        }
        return Json.createPointer(at).replace(asJsonStructure(in), with);
    }

    public static JsonValue remove(final JsonValue in, final String at) {
        if (notContainsValue(in, at)) {
            return in;
        }
        return Json.createPointer(at).remove(asJsonStructure(in));
    }

    public static Stream<JsonValue> stream(final JsonValue in) {
        if (isEmpty(in)) {
            return Stream.empty();
        }
        if (isArray(in)) {
            return in.asJsonArray().stream();
        }
        if (isObject(in)) {
            return in.asJsonObject().values().stream();
        }
        return Stream.of(in);
    }

    public static boolean isArray(final JsonValue js) {
        return ARRAY.equals(js.getValueType());
    }

    public static boolean isObject(final JsonValue js) {
        return OBJECT.equals(js.getValueType());
    }

    public static ScriptEngine engine(final EngineHolder engineHolder) {
        if (engineHolder.getEngine() != null) {
            return engineHolder.getEngine();
        }
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("javascript");
        try {
            engine.eval("Map = Java.type('java.util.LinkedHashMap')");
            engine.eval("Set = Java.type('java.util.LinkedHashSet')");
            engine.eval("List = Java.type('java.util.ArrayList')");
            engine.eval("Collectors = Java.type('java.util.stream.Collectors')");
        } catch (final Exception e) {
            logger.severe("Script engine for javascript not found: " + e);
        }
        engineHolder.setEngine(engine);
        return engine;
    }

    public static void eval(final EngineHolder engineHolder, final String script) {
        try {
            engine(engineHolder).eval(script);
        } catch (final Exception e) {
            logger.severe("Script failed: " + e);
        }
    }

    public static void eval(final EngineHolder engineHolder, final String script, final JsonValue value,
            final String key) {
        try {
            final ScriptEngine engine = engine(engineHolder);
            engine.put(key, asObject(value));
            engine.eval(script);
        } catch (final Exception e) {
            logger.severe("Script failed: " + e);
        }
    }

    public static Object getObject(final EngineHolder engineHolder, final String key) {
        try {
            return engine(engineHolder).get(key);
        } catch (final NullPointerException e) {
            logger.severe("Engine is null");
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static JsonValue asJsonValue(final Object o) {
        if (o instanceof Number) {
            return Json.createValue((Number) o);
        } else if (o instanceof String) {
            return Json.createValue((String) o);
        } else if (o instanceof Map) {
            try {
                return Json.createObjectBuilder((Map<String, ?>) o).build();
            } catch (final ClassCastException e) {
                return JsonValue.EMPTY_JSON_OBJECT;
            }
        } else if (o instanceof Collection) {
            return Json.createArrayBuilder((Collection<?>) o).build();
        }
        return JsonValue.EMPTY_JSON_OBJECT;
    }

    public static Object asObject(final JsonValue js) {
        final ValueType t = js.getValueType();
        if (ValueType.NUMBER.equals(t)) {
            return ((JsonNumber) js).numberValue();
        } else if (ValueType.STRING.equals(t)) {
            return ((JsonString) js).getString();
        } else if (ValueType.TRUE.equals(t)) {
            return true;
        } else if (ValueType.FALSE.equals(t)) {
            return false;
        } else if (ValueType.OBJECT.equals(t)) {
            return ((JsonObject) js).entrySet().stream().collect(
                    Collectors.toMap(Entry::getKey, x -> asObject(x.getValue()), (x, y) -> y, LinkedHashMap::new));
        } else if (ValueType.ARRAY.equals(t)) {
            return ((JsonArray) js).stream().map(Utils::asObject).collect(Collectors.toList());
        }
        return Collections.emptyMap();
    }

    private static JsonValue add(final JsonValue in, final String at, final JsonValue value) {
        try {
            return Json.createPointer(at).add(asJsonStructure(in), value);
        } catch (final JsonException e) {
            return in;
        }
    }

    private static boolean notContainsValue(final JsonValue in, final String at) {
        if (isEmpty(in)) {
            return true;
        }
        try {
            return !Json.createPointer(at).containsValue(asJsonStructure(in));
        } catch (final JsonException e) {
            return true;
        }
    }

    private static JsonStructure asJsonStructure(final JsonValue in) {
        if (isArray(in)) {
            return in.asJsonArray();
        }
        if (isObject(in)) {
            return in.asJsonObject();
        }
        return EMPTY_JSON_OBJECT;
    }
}
