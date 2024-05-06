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

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

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

    public static TransformerFactory factory() {
        return new TransformerFactory();
    }

    public static TransformerFactory factory(final Map<String, ExprFunction> functions) {
        return new TransformerFactory(functions);
    }

    private final Map<String, ExprFunction> functions;

    private TransformerFactory(final Map<String, ExprFunction> functions) {
        final Map<String, ExprFunction> result = builtin();
        result.putAll(functions);
        this.functions = Collections.unmodifiableMap(result);
    }

    private TransformerFactory() {
        this(Collections.emptyMap());
    }

    public Transformer createFromJsonString(final String json) {
        final String content = importPattern.matcher(json).replaceAll(x -> {
            final String importFile = x.group()
                    .substring(importOpenTag.length(), x.group().length() - importEndTag.length())
                    .trim();
            try {
                return escapePattern.matcher(Files.readString(Paths.get(importFile)))
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
                        .collect(Collectors.toList()));
    }

    public Transformer createFromFile(final String file) throws IOException {
        final String content = Files.readString(Paths.get(file));
        return createFromJsonString(content);
    }

    public Transformation toTransformation(final JsonValue transformations) {
        final JsonObject t = transformations.asJsonObject();
        System.out.println(transformations.toString());
        final Transformation parsed = new Transformation(TRUE.equals(t.get("append")),
                TRUE.equals(t.get("useResultAsSource")),
                t.get("sourcePointer") == null ? "" : t.getString("sourcePointer"),
                t.get("resultPointer") == null ? "" : t.getString("resultPointer"),
                t.get("expressions") == null ? Collections.emptyList()
                        : t.getJsonArray("expressions").stream().map(x -> ((JsonString) x).getString())
                                .collect(Collectors.toList()),
                functions);
        System.out.println(parsed.toJsonObject());
        return parsed;
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
