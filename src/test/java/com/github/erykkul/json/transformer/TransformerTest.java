// Author: Eryk Kulikowski (2024). Apache 2.0 License

package com.github.erykkul.json.transformer;

import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

public class TransformerTest {
    public static final StepFunction LOGGER = (ctx, source, result, sourcePointer, resultPointer, expression, engineHolder) -> {
        System.out.println("*****\n");
        System.out.println("ctx -> " + ctx.toJsonObject() + "\n");
        System.out.println("source -> " + source + "\n");
        System.out.println("result -> " + result + "\n");
        System.out.println("sourcePointer -> " + sourcePointer + "\n");
        System.out.println("resultPointer -> " + resultPointer + "\n");
        System.out.println("expression -> " + expression + "\n");
        final List<String> expressions = new ArrayList<>();
        if (expression != null && !"".equals(expression)) {
            expressions.add(expression);
        }
        final TransformationStep step = new TransformationStep(sourcePointer, resultPointer, expressions, engineHolder);
        final JsonValue res = step.execute(ctx, source, result);
        System.out.println("res -> " + res + "\n");
        System.out.println("*****");
        return res;
    };
    public static final TransformerFactory FACTORY_WITH_LOGGER = TransformerFactory.factory(Map.of("withLogger", LOGGER));

    @Test
    public void testTransformer() throws IOException {
        final Transformer transformer = FACTORY_WITH_LOGGER.createFromFile("example/transformer.json");
        final JsonObject result = transformer.transform(parse("example/example.json"));
        System.out.println(result);
        assertTrue(parse("example/transformed.json").equals(result));
        assertTrue(Utils.asJsonValue(Utils.asObject(result)).equals(result));
    }

    public JsonObject parse(final String fileName) throws FileNotFoundException {
        final JsonReader jsonReader = Json.createReader(new FileReader(fileName));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();
        return object;
    }
}
