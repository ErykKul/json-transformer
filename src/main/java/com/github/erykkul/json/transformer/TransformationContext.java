package com.github.erykkul.json.transformer;

import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

public class TransformationContext {
    private final Transformation transformation;
    private final JsonObject globalFrom;
    private final JsonObject globalTo;
    private final JsonValue localFrom;
    private final JsonValue localTo;

    public TransformationContext(final JsonObject globalFrom, final JsonObject globalTo, final JsonValue localFrom,
            final JsonValue localTo, final Transformation transformation) {
        this.globalFrom = globalFrom;
        this.globalTo = globalTo;
        this.localFrom = localFrom;
        this.localTo = localTo;
        this.transformation = transformation;
    }

    public JsonValue asJson() {
        return Json.createObjectBuilder().add("transformation", transformation.asJson()).add("globalFrom", globalFrom)
                .add("globalTo", globalTo).add("localFrom", localFrom).add("localTo", localTo).build();
    }

    public JsonObject getGlobalFrom() {
        return globalFrom;
    }

    public JsonObject getGlobalTo() {
        return globalTo;
    }

    public JsonValue getLocalFrom() {
        return localFrom;
    }

    public JsonValue getLocalTo() {
        return localTo;
    }

    public Map<String, TransformationStepFunction> getFunctions() {
        return transformation.getFunctions();
    }
}
