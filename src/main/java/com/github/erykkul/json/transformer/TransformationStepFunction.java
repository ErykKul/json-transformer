package com.github.erykkul.json.transformer;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.script.ScriptEngine;

import jakarta.json.Json;
import jakarta.json.JsonValue;

@FunctionalInterface
public interface TransformationStepFunction {

    TransformationStepFunction GENERATE_UUID = (ctx, from, to, valuePointer, funcArg) -> {
        return Utils.replace(to, valuePointer, Json.createValue(UUID.randomUUID().toString()));
    };

    TransformationStepFunction REMOVE = (ctx, from, to, valuePointer, funcArg) -> {
        return Utils.remove(to, valuePointer);
    };

    TransformationStepFunction FILTER = (ctx, from, to, valuePointer, funcArg) -> {
        final JsonValue value = Utils.getValue(from, valuePointer);
        if (Utils.isEmpty(value)) {
            return to;
        }
        final ScriptEngine engine = Utils.engine();
        final List<JsonValue> result = Utils.stream(value).filter(x -> {
            Utils.eval(engine, funcArg, x);
            return Boolean.TRUE.equals(Utils.getObject(engine, "res"));
        }).collect(Collectors.toList());
        return Utils.replace(to, valuePointer, Json.createArrayBuilder(result).build());
    };

    TransformationStepFunction MAP = (ctx, from, to, valuePointer, funcArg) -> {
        final JsonValue value = Utils.getValue(from, valuePointer);
        if (Utils.isEmpty(value)) {
            return to;
        }
        final ScriptEngine engine = Utils.engine();
        final List<JsonValue> result = Utils.stream(value).map(x -> {
            Utils.eval(engine, funcArg, x);
            return Utils.asJsonValue(Utils.getObject(engine, "res"));
        }).collect(Collectors.toList());
        return Utils.replace(to, valuePointer, Json.createArrayBuilder(result).build());
    };

    TransformationStepFunction REDUCE = (ctx, from, to, valuePointer, funcArg) -> {
        final JsonValue value = Utils.getValue(from, valuePointer);
        if (Utils.isEmpty(value)) {
            return to;
        }
        final ScriptEngine engine = Utils.engine();
        Utils.stream(value).forEach(x -> {
            Utils.eval(engine, funcArg, x);
        });
        return Utils.asJsonValue(Utils.getObject(engine, "res"));
    };

    JsonValue copy(TransformationContext ctx, JsonValue from, JsonValue to, String valuePointer, String funcArg);

    // TODO:
    // expand filepaths
}
