package com.github.erykkul.json.transformer;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

public class TransformationContext {
    private final JsonObject GlobalFrom;
    private final JsonObject GlobalTo;
    private final JsonValue localFrom;
    private final JsonValue localTo;

    public TransformationContext(final JsonObject globalFrom, final JsonObject globalTo, final JsonValue localFrom,
            final JsonValue localTo) {
        GlobalFrom = globalFrom;
        GlobalTo = globalTo;
        this.localFrom = localFrom;
        this.localTo = localTo;
    }

    public JsonObject getGlobalFrom() {
        return GlobalFrom;
    }

    public JsonObject getGlobalTo() {
        return GlobalTo;
    }

    public JsonValue getLocalFrom() {
        return localFrom;
    }

    public JsonValue getLocalTo() {
        return localTo;
    }
}
