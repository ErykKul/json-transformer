package com.github.erykkul.json.transformer;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.script.ScriptEngine;

import jakarta.json.Json;
import jakarta.json.JsonValue;

@FunctionalInterface
public interface TransformationStepFunction {

    TransformationStepFunction GENERATE_UUID = (ctx, source, result, valuePointer, funcArg) -> {
        return Utils.replace(result, valuePointer, Json.createValue(UUID.randomUUID().toString()));
    };

    TransformationStepFunction REMOVE = (ctx, source, result, valuePointer, funcArg) -> {
        return Utils.remove(result, valuePointer);
    };

    TransformationStepFunction FILTER = (ctx, source, result, valuePointer, funcArg) -> {
        final JsonValue value = Utils.getValue(source, valuePointer);
        if (Utils.isEmpty(value)) {
            return result;
        }
        final ScriptEngine engine = Utils.engine();
        final List<JsonValue> res = Utils.stream(value).filter(x -> {
            Utils.eval(engine, funcArg, x);
            return Boolean.TRUE.equals(Utils.getObject(engine, "res"));
        }).collect(Collectors.toList());
        return Utils.replace(result, valuePointer, Json.createArrayBuilder(res).build());
    };

    TransformationStepFunction MAP = (ctx, source, result, valuePointer, funcArg) -> {
        final JsonValue value = Utils.getValue(source, valuePointer);
        if (Utils.isEmpty(value)) {
            return result;
        }
        final ScriptEngine engine = Utils.engine();
        final List<JsonValue> res = Utils.stream(value).map(x -> {
            Utils.eval(engine, funcArg, x);
            return Utils.asJsonValue(Utils.getObject(engine, "res"));
        }).collect(Collectors.toList());
        return Utils.replace(result, valuePointer, Json.createArrayBuilder(res).build());
    };

    TransformationStepFunction REDUCE = (ctx, source, result, valuePointer, funcArg) -> {
        final JsonValue value = Utils.getValue(source, valuePointer);
        if (Utils.isEmpty(value)) {
            return result;
        }
        final ScriptEngine engine = Utils.engine();
        Utils.stream(value).forEach(x -> {
            Utils.eval(engine, funcArg, x);
        });
        return Utils.asJsonValue(Utils.getObject(engine, "res"));
    };

    JsonValue apply(TransformationContext ctx, JsonValue source, JsonValue result, String valuePointer, String funcArg);

    // TODO:
    // expand filepaths
}
