// Author: Eryk Kulikowski (2024). Apache 2.0 License

package io.github.erykkul.json.transformer;

import static jakarta.json.JsonValue.TRUE;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.script.ScriptEngineFactory;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

/**
 * The transformer factory. See documentation: <a href=
 * "https://github.com/ErykKul/json-transformer?tab=readme-ov-file#transformer">Transformer</a>
 * 
 * @author Eryk Kulikowski
 * @version 1.0.2
 * @since 1.0.0
 */
public class TransformerFactory {

    private static final Logger logger = Logger.getLogger(TransformerFactory.class.getName());
    private static final String importOpenTag = "importJS";
    private static final String importEndTag = "endImport";
    private static final Pattern importPattern = Pattern.compile(importOpenTag + "(.*)" + importEndTag);
    private static final Pattern escapePattern = Pattern.compile("(;\r\n|;\n|\r\n|\n|\"|\\\\|\b|\f|\r|\t)");
    private static final Map<String, String> escapeMap = Map.of(
            ";\r\n", "; ",
            ";\n", "; ",
            "\r\n", "; ",
            "\n", "; ",
            "\\", "\\\\\\\\\\\\\\\\",
            "\"", "\\\\\\\\\"",
            "\b", "\\\\\\\\b",
            "\f", "\\\\\\\\f",
            "\r", "\\\\\\\\r",
            "\t", "\\\\\\\\t");

    /**
     * Creates a default transformer factory with only the built-in functions.
     * 
     * @return the default transformer factory
     */
    public static TransformerFactory factory() {
        return new TransformerFactory(Collections.emptyMap(), null);
    }

    /**
     * Creates a default transformer factory with only the built-in functions.
     * 
     * @param scriptEngineFactory the script engine factory
     * @return the default transformer factory
     */
    public static TransformerFactory factory(final ScriptEngineFactory scriptEngineFactory) {
        return new TransformerFactory(Collections.emptyMap(), scriptEngineFactory);
    }

    /**
     * Creates a transformer factory that next to the default built-in functions
     * also registers the functions as provided by the caller.
     * 
     * @param functions the new functions to be added (the functions using the same
     *                  key as built-in function override these functions)
     * @return the transformer factory with the built-in functions and the functions
     *         passed as argument.
     */
    public static TransformerFactory factory(final Map<String, ExprFunction> functions) {
        return new TransformerFactory(functions, null);
    }

    /**
     * Creates a transformer factory that next to the default built-in functions
     * also registers the functions as provided by the caller.
     * 
     * @param functions           the new functions to be added (the functions using
     *                            the same
     *                            key as built-in function override these functions)
     * @param scriptEngineFactory the script engine factory
     * @return the transformer factory with the built-in functions and the functions
     *         passed as argument.
     */
    public static TransformerFactory factory(final Map<String, ExprFunction> functions,
            final ScriptEngineFactory scriptEngineFactory) {
        return new TransformerFactory(functions, scriptEngineFactory);
    }

    private final Map<String, ExprFunction> functions;
    private final ScriptEngineFactory scriptEngineFactory;

    private TransformerFactory(final Map<String, ExprFunction> functions,
            final ScriptEngineFactory scriptEngineFactory) {
        final Map<String, ExprFunction> result = builtin();
        result.putAll(functions);
        this.functions = Collections.unmodifiableMap(result);
        this.scriptEngineFactory = scriptEngineFactory;
    }

    /**
     * Creates a new transformer from the String representation of the JSON document
     * of the transformer.
     * 
     * @param json the String representation of the JSON document of the transformer
     * @return the transformer
     */
    public Transformer createFromJsonString(final String json) {
        return createFromJsonString(json, "");
    }

    /**
     * 
     * @param json       the String representation of the JSON document of the
     *                   transformer
     * @param importPath the path where JavaScript files can be imported from
     * @return the transformer
     */
    public Transformer createFromJsonString(final String json, final String importPath) {
        final String content = importPattern.matcher(json).replaceAll(x -> {
            final String importFile = x.group()
                    .substring(importOpenTag.length(), x.group().length() - importEndTag.length())
                    .trim();
            try {
                return escapePattern.matcher(Files.readString(Paths.get(importPath + importFile)))
                        .replaceAll(y -> escapeMap.get(y.group()));
            } catch (final IOException e) {
                logger.severe("Importing from file \"" + importFile + "\" failed: " + e);
                return "";
            }
        });
        final JsonReader jsonReader = Json.createReader(new StringReader(content));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();
        return new Transformer(object.get("transformations") == null ? Collections.emptyList()
                : object.getJsonArray("transformations").stream().map(this::toTransformation)
                        .collect(Collectors.toList()),
                scriptEngineFactory);
    }

    /**
     * Creates a new transformer from a file containing the JSON document of the
     * transformer.
     * 
     * @param file       the path and name of the file
     * @param importPath the path where JavaScript files can be imported from
     * @return the transformer
     * @throws IOException thrown when the file is not found
     */
    public Transformer createFromFile(final String file, final String importPath) throws IOException {
        final String content = Files.readString(Paths.get(file));
        return createFromJsonString(content, importPath);
    }

    /**
     * Creates a new transformer from a file containing the JSON document of the
     * transformer.
     * 
     * @param file the path and name of the file
     * @return the transformer
     * @throws IOException thrown when the file is not found
     */
    public Transformer createFromFile(final String file) throws IOException {
        final String content = Files.readString(Paths.get(file));
        return createFromJsonString(content);
    }

    /**
     * Creates a new Transformation object from the JsonValue of that transformation
     * document.
     * 
     * @param transformation the JsonValue of the transformation document
     * @return the transformation object
     */
    public Transformation toTransformation(final JsonValue transformation) {
        final JsonObject t = transformation.asJsonObject();
        return new Transformation(TRUE.equals(t.get("append")),
                TRUE.equals(t.get("useResultAsSource")),
                t.get("sourcePointer") == null ? "" : t.getString("sourcePointer"),
                t.get("resultPointer") == null ? "" : t.getString("resultPointer"),
                t.get("expressions") == null ? Collections.emptyList()
                        : t.getJsonArray("expressions").stream().map(x -> ((JsonString) x).getString())
                                .collect(Collectors.toList()),
                functions);
    }

    private Map<String, ExprFunction> builtin() {
        final Map<String, ExprFunction> result = new HashMap<>();
        result.put("copy", ExprFunction.COPY);
        result.put("move", ExprFunction.MOVE);
        result.put("remove", ExprFunction.REMOVE);
        result.put("generateUuid", ExprFunction.GENERATE_UUID);
        result.put("script", ExprFunction.SCRIPT);
        result.put("filter", ExprFunction.FILTER);
        result.put("map", ExprFunction.MAP);
        result.put("reduce", ExprFunction.REDUCE);
        return result;
    }
}
