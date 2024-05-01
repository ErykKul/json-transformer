package com.github.erykkul.json.transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.json.Json;
import jakarta.json.JsonValue;
import jakarta.json.JsonValue.ValueType;

@FunctionalInterface
public interface TransformationStepFunction {

    TransformationStepFunction GENERATE_UUID = (ctx, source, result, sourcePointer, resultPointer, expression, engineHolder) -> {
        return Utils.replace(Utils.fixTargetPath(result, ValueType.OBJECT, resultPointer), resultPointer, Json.createValue(UUID.randomUUID().toString()));
    };

    TransformationStepFunction REMOVE = (ctx, source, result, sourcePointer, resultPointer, expression, engineHolder) -> {
        return Utils.remove(result, resultPointer);
    };

    TransformationStepFunction USE_RESULT_AS_SOURCE = (ctx, source, result, sourcePointer, resultPointer, expression, engineHolder) -> {
        final List<String> expressions = new ArrayList<>();
        if (expression != null && !"".equals(expression)) {
            expressions.add(expression);
        }
        final TransformationStep step = new TransformationStep(sourcePointer, resultPointer, expressions, engineHolder);
        return step.execute(ctx, result, result);
    };

    TransformationStepFunction SCRIPT = (ctx, source, result, sourcePointer, resultPointer, expression, engineHolder) -> {
        final JsonValue value = Utils.getValue(source, sourcePointer);
        if (Utils.isEmpty(value)) {
            return result;
        }
        Utils.eval(engineHolder, expression, value);
        final JsonValue res = Utils.asJsonValue(Utils.getObject(engineHolder, "res"));
        if (Utils.isEmpty(res)) {
            return result;
        }
        return Utils.replace(Utils.fixTargetPath(result, ValueType.ARRAY, resultPointer), resultPointer, res);
    };

    TransformationStepFunction FILTER = (ctx, source, result, sourcePointer, resultPointer, expression, engineHolder) -> {
        final JsonValue value = Utils.getValue(source, sourcePointer);
        if (Utils.isEmpty(value)) {
            return result;
        }
        final List<JsonValue> res = Utils.stream(value).filter(x -> {
            Utils.eval(engineHolder, expression, x);
            return Boolean.TRUE.equals(Utils.getObject(engineHolder, "res"));
        }).collect(Collectors.toList());
        return Utils.replace(Utils.fixTargetPath(result, ValueType.ARRAY, resultPointer), resultPointer, Json.createArrayBuilder(res).build());
    };

    TransformationStepFunction MAP = (ctx, source, result, sourcePointer, resultPointer, expression, engineHolder) -> {
        final JsonValue value = Utils.getValue(source, sourcePointer);
        if (Utils.isEmpty(value)) {
            return result;
        }
        final List<JsonValue> res = Utils.stream(value).map(x -> {
            Utils.eval(engineHolder, expression, x);
            return Utils.asJsonValue(Utils.getObject(engineHolder, "res"));
        }).collect(Collectors.toList());
        return Utils.replace(Utils.fixTargetPath(result, ValueType.ARRAY, resultPointer), resultPointer, Json.createArrayBuilder(res).build());
    };

    TransformationStepFunction REDUCE = (ctx, source, result, sourcePointer, resultPointer, expression, engineHolder) -> {
        final JsonValue value = Utils.getValue(source, sourcePointer);
        if (Utils.isEmpty(value)) {
            return result;
        }
        Utils.stream(value).forEach(x -> {
            Utils.eval(engineHolder, expression, x);
        });
        return Utils.replace(Utils.fixTargetPath(result, ValueType.OBJECT, resultPointer), resultPointer, Utils.asJsonValue(Utils.getObject(engineHolder, "res")));
    };

    JsonValue apply(TransformationContext ctx, JsonValue source, JsonValue result, String sourcePointer, String resultPointer, String expression, ScriptEngineHolder engineHolder);
}
