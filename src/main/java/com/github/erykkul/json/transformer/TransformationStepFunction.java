package com.github.erykkul.json.transformer;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.script.ScriptEngine;

import jakarta.json.Json;
import jakarta.json.JsonValue;
import jakarta.json.JsonValue.ValueType;

@FunctionalInterface
public interface TransformationStepFunction {

    TransformationStepFunction GENERATE_UUID = (ctx, source, result, sourcePointer, resultPointer, funcArg) -> {
        return Utils.replace(Utils.fixTargetPath(result, ValueType.OBJECT, resultPointer), resultPointer, Json.createValue(UUID.randomUUID().toString()));
    };

    TransformationStepFunction REMOVE = (ctx, source, result, sourcePointer, resultPointer, funcArg) -> {
        return Utils.remove(result, resultPointer);
    };

    TransformationStepFunction FILTER = (ctx, source, result, sourcePointer, resultPointer, funcArg) -> {
        final JsonValue value = Utils.getValue(source, sourcePointer);
        if (Utils.isEmpty(value)) {
            return result;
        }
        final ScriptEngine engine = Utils.engine();
        final List<JsonValue> res = Utils.stream(value).filter(x -> {
            Utils.eval(engine, funcArg, x);
            return Boolean.TRUE.equals(Utils.getObject(engine, "res"));
        }).collect(Collectors.toList());
        return Utils.replace(Utils.fixTargetPath(result, ValueType.ARRAY, resultPointer), resultPointer, Json.createArrayBuilder(res).build());
    };

    TransformationStepFunction MAP = (ctx, source, result, sourcePointer, resultPointer, funcArg) -> {
        final JsonValue value = Utils.getValue(source, sourcePointer);
        if (Utils.isEmpty(value)) {
            return result;
        }
        final ScriptEngine engine = Utils.engine();
        final List<JsonValue> res = Utils.stream(value).map(x -> {
            Utils.eval(engine, funcArg, x);
            return Utils.asJsonValue(Utils.getObject(engine, "res"));
        }).collect(Collectors.toList());
        return Utils.replace(Utils.fixTargetPath(result, ValueType.ARRAY, resultPointer), resultPointer, Json.createArrayBuilder(res).build());
    };

    TransformationStepFunction REDUCE = (ctx, source, result, sourcePointer, resultPointer, funcArg) -> {
        final JsonValue value = Utils.getValue(source, sourcePointer);
        if (Utils.isEmpty(value)) {
            return result;
        }
        final ScriptEngine engine = Utils.engine();
        Utils.stream(value).forEach(x -> {
            Utils.eval(engine, funcArg, x);
        });
        return Utils.replace(Utils.fixTargetPath(result, ValueType.OBJECT, resultPointer), resultPointer, Utils.asJsonValue(Utils.getObject(engine, "res")));
    };

    JsonValue apply(TransformationContext ctx, JsonValue source, JsonValue result, String sourcePointer, String resultPointer, String funcArg);

    // TODO:
    // expand filepaths
}
