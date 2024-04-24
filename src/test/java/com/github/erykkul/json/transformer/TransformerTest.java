package com.github.erykkul.json.transformer;

import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

public class TransformerTest {
    public static final ValueFunction LOGGER = (ctx, from, to, valuePointer, funcArg) -> {
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
    };
    public static final TransformerFactory FACTORY_WITH_LOGGER = TransformerFactory.factory(Map.of("withLogger", LOGGER));

    @Test
    public void testTransformer() throws IOException {
        final Transformer transformer = FACTORY_WITH_LOGGER.createFromFile("example/transformer.json");
        final JsonObject result = transformer.transform(getObject("example/example.json"));
        System.out.println(result);
    }

    public JsonObject getObject(final String fileName) throws FileNotFoundException {
        final JsonReader jsonReader = Json.createReader(new FileReader(fileName));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();
        return object;
    }
}
