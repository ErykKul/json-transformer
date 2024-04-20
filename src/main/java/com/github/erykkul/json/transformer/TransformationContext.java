package com.github.erykkul.json.transformer;

import jakarta.json.JsonObject;

public class TransformationContext {
    private final JsonObject GlobalFrom;
    private final JsonObject GlobalTo;
    private final JsonObject localFrom;
    private final JsonObject localTo;

    public TransformationContext(final JsonObject globalFrom, final JsonObject globalTo, final JsonObject localFrom,
            final JsonObject localTo) {
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

    public JsonObject getLocalFrom() {
        return localFrom;
    }

    public JsonObject getLocalTo() {
        return localTo;
    }
}
