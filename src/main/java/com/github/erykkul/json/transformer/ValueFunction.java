package com.github.erykkul.json.transformer;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.script.ScriptEngine;

import jakarta.json.Json;
import jakarta.json.JsonValue;

@FunctionalInterface
public interface ValueFunction {

    ValueFunction GENERATE_UUID = (ctx, from, to, valuePointer, funcArg) -> {
        return Utils.replace(to, valuePointer, Json.createValue(UUID.randomUUID().toString()));
    };

    ValueFunction REMOVE = (ctx, from, to, valuePointer, funcArg) -> {
        return Utils.remove(to, valuePointer);
    };

    ValueFunction FILTER = (ctx, from, to, valuePointer, funcArg) -> {
        final JsonValue value = Utils.getValue(from, valuePointer);
        if (Utils.isEmpty(value)) {
            return to;
        }
        final ScriptEngine engine = Utils.engine();
        final List<JsonValue> result = Utils.stream(value).filter(x -> {
            Utils.eval(engine, funcArg, x);
            return Boolean.TRUE.equals(engine.get("res"));
        }).collect(Collectors.toList());
        return Utils.replace(to, valuePointer, Json.createArrayBuilder(result).build());
    };

    ValueFunction MAP = (ctx, from, to, valuePointer, funcArg) -> {
        final JsonValue value = Utils.getValue(from, valuePointer);
        if (Utils.isEmpty(value)) {
            return to;
        }
        final ScriptEngine engine = Utils.engine();
        final List<JsonValue> result = Utils.stream(value).map(x -> {
            Utils.eval(engine, funcArg, x);
            return Utils.asJsonValue(engine.get("res"));
        }).collect(Collectors.toList());
        return Utils.replace(to, valuePointer, Json.createArrayBuilder(result).build());
    };

    ValueFunction REDUCE = (ctx, from, to, valuePointer, funcArg) -> {
        final JsonValue value = Utils.getValue(from, valuePointer);
        if (Utils.isEmpty(value)) {
            return to;
        }
        final ScriptEngine engine = Utils.engine();
        Utils.stream(value).forEach(x -> {
            Utils.eval(engine, funcArg, x);
        });
        return Utils.asJsonValue(engine.get("res"));
    };

    JsonValue copy(TransformationContext ctx, JsonValue from, JsonValue to, String valuePointer, String funcArg);

    // TODO:
    // expand filepaths
}
