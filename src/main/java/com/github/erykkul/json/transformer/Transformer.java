package com.github.erykkul.json.transformer;

import java.util.List;

import jakarta.json.JsonObject;

public class Transformer {
    private final List<Transformation> transformations;

    public Transformer(final List<Transformation> transformations) {
        this.transformations = transformations;
    }

    public JsonObject transform(final JsonObject from) {
        JsonObject result = JsonObject.EMPTY_JSON_OBJECT;
        for (final Transformation t : transformations) {
            result = t.transform(from, result);
        }
        return result;
    }
}
