package com.github.erykkul.json.transformer;

import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

public class TransformerTest {
    @Test
    public void shouldAnswerWithTrue() {
        try {
            TransformerFactory factory = new TransformerFactory();
            final String content = Files.readString(Paths.get("example/example.json"));
            final JsonReader jsonReader = Json.createReader(new StringReader(content));
            final JsonObject object = jsonReader.readObject();
            jsonReader.close();
            final Transformer transformer = factory.createFromFile("example/transformer.json");
            System.out.println(transformer.transform(object, JsonObject.EMPTY_JSON_OBJECT));
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }
}
