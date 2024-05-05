// Author: Eryk Kulikowski (2024). Apache 2.0 License

package io.github.erykkul.json.transformer;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

public class TransformerTest {
    public static final ExprFunction LOGGER = (ctx, source, result, expression) -> {
        System.out.println("*****\n");
        System.out.println("ctx -> " + ctx.toJsonObject() + "\n");
        System.out.println("source -> " + source + "\n");
        System.out.println("result -> " + result + "\n");
        System.out.println("expression -> " + expression + "\n");
        final List<String> expressions = new ArrayList<>();
        if (expression != null && !"".equals(expression)) {
            expressions.add(expression);
        }
        final JsonValue res = Transformation.executeExpressions(ctx, source, result, expressions);
        System.out.println("res -> " + res + "\n");
        System.out.println("*****");
        return res;
    };

    public static final TransformerFactory FACTORY_WITH_LOGGER = TransformerFactory
            .factory(Map.of("withLogger", LOGGER));

    @Test
    public void testTransformer() throws IOException {
        final Transformer transformer = FACTORY_WITH_LOGGER.createFromFile("examples/transformer.json");
        final JsonObject result = transformer.transform(parse("examples/example.json"));
        System.out.println(result);
        assertEquals(parse("examples/transformed.json"), result);
        assertEquals(result, Utils.asJsonValue(Utils.asObject(result)));
    }

    @Test
    public void testExamples() throws IOException {
        final List<String> examples = Arrays.asList("quickStart", "merging1", "merging2", "merging3", "literals",
                "functions", "import", "append", "arrayIndex", "arraysIterations", "parent");
        for (final String example : examples) {
            System.out.println(example);
            final Transformer transformer = FACTORY_WITH_LOGGER
                    .createFromFile("examples/documentation/" + example + "ExampleTransformer.json");
            final JsonObject result = transformer
                    .transform(parse("examples/documentation/" + example + "ExampleSource.json"));
            System.out.println(result);
            assertEquals(parse("examples/documentation/" + example + "ExampleResult.json"), result);
        }
    }

    @Test
    public void testUuid() {
        final Transformer transformer = FACTORY_WITH_LOGGER
                .createFromJsonString(
                        "{\"transformations\": [{\"expressions\":[\"generateUuid(/uuid1)\"]}, {\"resultPointer\":\"/uuid2\", \"expressions\":[\"generateUuid()\"]}]}");
        final JsonObject result = transformer.transform(JsonObject.EMPTY_JSON_OBJECT);
        System.out.println(result);
        assertEquals(95, result.toString().length());
    }

    public JsonObject parse(final String fileName) throws FileNotFoundException {
        final JsonReader jsonReader = Json.createReader(new FileReader(fileName));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();
        return object;
    }
}
