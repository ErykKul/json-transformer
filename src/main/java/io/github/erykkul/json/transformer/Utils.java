// Author: Eryk Kulikowski (2024). Apache 2.0 License

package io.github.erykkul.json.transformer;

import static jakarta.json.JsonValue.EMPTY_JSON_ARRAY;
import static jakarta.json.JsonValue.EMPTY_JSON_OBJECT;
import static jakarta.json.JsonValue.NULL;
import static jakarta.json.JsonValue.ValueType.ARRAY;
import static jakarta.json.JsonValue.ValueType.OBJECT;

import java.util.Collection;
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

/**
 * Utils class as used by this library. See documentation: <a href=
 * "https://github.com/ErykKul/json-transformer?tab=readme-ov-file#json-transformer">JSON
 * Transformer</a>
 * 
 * @author Eryk Kulikowski
 * @version 1.0.1
 * @since 1.0.0
 */
public class Utils {

    private static final Logger logger = Logger.getLogger(Utils.class.getName());

    /**
     * Creates JsonValues when needed to make the JSON Pointer a valid JSON Pointer
     * 
     * @param in          the document where the JSON Pointer needs to be valid
     * @param t           the type of the JSON value that the pointer needs to point
     *                    to
     * @param jsonPointer the JSON Pointer
     * @return the "in" document with the newly created values to make JSON pointer
     *         valid
     */
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

    /**
     * Determines if the value is empty.
     * 
     * @param value the value
     * @return true if empty
     */
    public static boolean isEmpty(final JsonValue value) {
        return value == null || EMPTY_JSON_ARRAY.equals(value) || EMPTY_JSON_OBJECT.equals(value) || NULL.equals(value);
    }

    /**
     * Retrieves the value as pointed by the JSON Pointer from the source document
     * 
     * @param source  the source document
     * @param pointer the pointer
     * @return the value
     */
    public static JsonValue getValue(final JsonValue source, final String pointer) {
        if ("".equals(pointer)) {
            return source;
        }
        if (notContainsValue(source, pointer)) {
            return NULL;
        }
        return Json.createPointer(pointer).getValue(asJsonStructure(source));
    }

    /**
     * Replaces the value as pointed by the JSON Pointer with the provided value
     * 
     * @param in   the source document
     * @param at   the pointer
     * @param with the new value
     * @return the "in" value with the replaced value
     */
    public static JsonValue replace(final JsonValue in, final String at, final JsonValue with) {
        if ("".equals(at)) {
            return with;
        }
        if (notContainsValue(in, at)) {
            return add(in, at, with);
        }
        return Json.createPointer(at).replace(asJsonStructure(in), with);
    }

    /**
     * Removes the value at the pointer location.
     * 
     * @param in the document where the value needs to be removed
     * @param at the pointer
     * @return the "in" document with the value removed
     */
    public static JsonValue remove(final JsonValue in, final String at) {
        if (notContainsValue(in, at)) {
            return in;
        }
        return Json.createPointer(at).remove(asJsonStructure(in));
    }

    /**
     * Returns the stream of JSON values contained in the provided value.
     * 
     * @param in the source
     * @return the stream
     */
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

    /**
     * Checks if the JsonValue is a JsonArray object.
     * 
     * @param js the JSON value
     * @return true if the JSON value is a JsonArray
     */
    public static boolean isArray(final JsonValue js) {
        return ARRAY.equals(js.getValueType());
    }

    /**
     * Checks if the JsonValue is a JsonObject object.
     * 
     * @param js the JSON value
     * @return true if the JSON value is a JsonObject
     */
    public static boolean isObject(final JsonValue js) {
        return OBJECT.equals(js.getValueType());
    }

    /**
     * Retrieves the script engine from the engine holder. If the engine holder does
     * not yet hold an engine, a new engine is created and returned.
     * 
     * @param engineHolder the engine holder
     * @return the engine
     */
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
            engine.eval("JsonValue = Java.type('jakarta.json.JsonValue')");
        } catch (final Exception e) {
            logger.severe("Script engine for javascript not found: " + e);
        }
        engineHolder.setEngine(engine);
        return engine;
    }

    /**
     * Evaluates the script on the engine in the engine holder. If the engine holder
     * does not yet hold an engine, a new engine is created.
     * 
     * @param engineHolder the engine holder
     * @param script       the script
     */
    public static void eval(final EngineHolder engineHolder, final String script) {
        try {
            engine(engineHolder).eval(script);
        } catch (final Exception e) {
            logger.severe("Script failed: " + e);
        }
    }

    /**
     * Evaluates the script on the engine in the engine holder. If the engine holder
     * does not yet hold an engine, a new engine is created.
     * 
     * @param engineHolder the engine holder
     * @param script       the script
     * @param value        the JsonValue to put to the engine (as Object) before
     *                     evaluating the script
     * @param key          the key of the object (e.g., "x")
     */
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

    /**
     * Retrieves an object from the script engine.
     * 
     * @param engineHolder the engine holder
     * @param key          the key of the object (e.g., "res")
     * @return the object
     */
    public static Object getObject(final EngineHolder engineHolder, final String key) {
        try {
            return engine(engineHolder).get(key);
        } catch (final NullPointerException e) {
            logger.severe("Engine is null");
        }
        return null;
    }

    /**
     * Creates a JSON value holding the given Object, e.g., as retrieved from the
     * script engine
     * 
     * @param o the object
     * @return the JSON value
     */
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
                return JsonValue.NULL;
            }
        } else if (o instanceof Collection) {
            return Json.createArrayBuilder((Collection<?>) o).build();
        }
        return JsonValue.NULL;
    }

    /**
     * Retrieves the object as held by the JSON value, e.g., before putting it in
     * the engine
     * 
     * @param js the JSON value
     * @return the object
     */
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
        return JsonValue.NULL;
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
