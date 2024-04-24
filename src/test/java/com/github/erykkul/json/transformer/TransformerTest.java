package com.github.erykkul.json.transformer;

import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

public class TransformerTest {
    @Test
    public void testTransformer() {
        final TransformerFactory factory = new TransformerFactory(getLogger());
        final JsonObject transformationJson = getObject("example/transformer.json");
        final JsonObject exampleJson = getObject("example/example.json");
        final Transformer transformer = factory.createFromJsonString(transformationJson.toString());
        System.out.println(transformer.transform(exampleJson, JsonObject.EMPTY_JSON_OBJECT));
    }

    public Map<String, ValueFunction> getLogger() {
        final Map<String, ValueFunction> functions = new HashMap<>();
        functions.put("withLogger", (ctx, from, to, valuePointer, funcArg) -> {
            System.out.println("*****\n");
            System.out.println("ctx -> " + ctx.asJson() + "\n");
            System.out.println("from -> " + from + "\n");
            System.out.println("to -> " + to + "\n");
            System.out.println("valuePointer -> " + valuePointer + "\n");
            System.out.println("valueExpression -> " + funcArg + "\n");
            final Value v = new Value(valuePointer, funcArg);
            final JsonValue result = v.copy(ctx, from, to);
            System.out.println("result -> " + result + "\n");
            System.out.println("*****");
            return result;
        });
        return functions;
    }

    public JsonObject getObject(final String fileName) {
        try {
            final String content = Files.readString(Paths.get(fileName));
            final JsonReader jsonReader = Json.createReader(new StringReader(content));
            final JsonObject object = jsonReader.readObject();
            jsonReader.close();
            return object;
        } catch (final Exception e) {
            e.printStackTrace();
            fail();
            throw new RuntimeException("failed");
        }
    }
}
