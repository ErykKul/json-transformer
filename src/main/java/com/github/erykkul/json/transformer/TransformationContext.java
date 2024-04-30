package com.github.erykkul.json.transformer;

import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

public class TransformationContext {
    private final Transformation transformation;
    private final JsonObject globalSource;
    private final JsonObject globalResult;
    private final JsonValue localSource;
    private final JsonValue localResult;

    public TransformationContext(final JsonObject globalSource, final JsonObject globalResult, final JsonValue localSource,
            final JsonValue localResult, final Transformation transformation) {
        this.globalSource = globalSource;
        this.globalResult = globalResult;
        this.localSource = localSource;
        this.localResult = localResult;
        this.transformation = transformation;
    }

    public JsonObject toJsonObject() {
        return Json.createObjectBuilder().add("transformation", transformation.toJsonObject()).add("globalSource", globalSource)
                .add("globalResult", globalResult).add("localSource", localSource).add("localResult", localResult).build();
    }

    public JsonObject getGlobalSource() {
        return globalSource;
    }

    public JsonObject getGlobalResult() {
        return globalResult;
    }

    public JsonValue getLocalSource() {
        return localSource;
    }

    public JsonValue getLocalResult() {
        return localResult;
    }

    public Map<String, TransformationStepFunction> getFunctions() {
        return transformation.getFunctions();
    }
}
