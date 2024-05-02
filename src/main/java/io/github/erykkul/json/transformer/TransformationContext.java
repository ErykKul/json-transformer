// Author: Eryk Kulikowski (2024). Apache 2.0 License

package io.github.erykkul.json.transformer;

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
    private final EngineHolder engineHolder;

    public TransformationContext(final JsonObject globalSource, final JsonObject globalResult,
            final JsonValue localSource, final JsonValue localResult, final Transformation transformation,
            final EngineHolder engineHolder) {
        this.globalSource = globalSource;
        this.globalResult = globalResult;
        this.localSource = localSource;
        this.localResult = localResult;
        this.transformation = transformation;
        this.engineHolder = engineHolder;
    }

    public JsonObject toJsonObject() {
        return Json.createObjectBuilder().add("transformation", transformation.toJsonObject())
                .add("globalSource", globalSource)
                .add("globalResult", globalResult).add("localSource", localSource).add("localResult", localResult)
                .add("useResultAsSource", transformation.useResultAsSource()).build();
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

    public Map<String, StepFunction> getFunctions() {
        return transformation.getFunctions();
    }

    public boolean useResultAsSource() {
        return transformation.useResultAsSource();
    }

    public EngineHolder engine() {
        return engineHolder;
    }
}
