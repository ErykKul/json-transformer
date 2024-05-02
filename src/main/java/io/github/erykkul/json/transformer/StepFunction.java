// Author: Eryk Kulikowski (2024). Apache 2.0 License

package io.github.erykkul.json.transformer;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.json.Json;
import jakarta.json.JsonValue;
import jakarta.json.JsonValue.ValueType;

@FunctionalInterface
public interface StepFunction {

    StepFunction GENERATE_UUID = (ctx, source, result, sourcePointer, resultPointer, expression, engineHolder) -> {
        return Utils.replace(Utils.fixPath(result, ValueType.OBJECT, resultPointer), resultPointer,
                Json.createValue(UUID.randomUUID().toString()));
    };

    StepFunction REMOVE = (ctx, source, result, sourcePointer, resultPointer, expression, engineHolder) -> {
        return Utils.remove(result, resultPointer);
    };

    StepFunction SCRIPT = (ctx, source, result, sourcePointer, resultPointer, expression, engineHolder) -> {
        final JsonValue value = Utils.getValue(source, sourcePointer);
        if (Utils.isEmpty(value)) {
            return result;
        }
        Utils.eval(engineHolder, expression, value);
        final JsonValue res = Utils.asJsonValue(Utils.getObject(engineHolder, "res"));
        if (Utils.isEmpty(res)) {
            return result;
        }
        return Utils.replace(Utils.fixPath(result, ValueType.ARRAY, resultPointer), resultPointer, res);
    };

    StepFunction FILTER = (ctx, source, result, sourcePointer, resultPointer, expression, engineHolder) -> {
        final JsonValue value = Utils.getValue(source, sourcePointer);
        if (Utils.isEmpty(value)) {
            return result;
        }
        final List<JsonValue> res = Utils.stream(value).filter(x -> {
            Utils.eval(engineHolder, expression, x);
            return Boolean.TRUE.equals(Utils.getObject(engineHolder, "res"));
        }).collect(Collectors.toList());
        return Utils.replace(Utils.fixPath(result, ValueType.ARRAY, resultPointer), resultPointer,
                Json.createArrayBuilder(res).build());
    };

    StepFunction MAP = (ctx, source, result, sourcePointer, resultPointer, expression, engineHolder) -> {
        final JsonValue value = Utils.getValue(source, sourcePointer);
        if (Utils.isEmpty(value)) {
            return result;
        }
        final List<JsonValue> res = Utils.stream(value).map(x -> {
            Utils.eval(engineHolder, expression, x);
            return Utils.asJsonValue(Utils.getObject(engineHolder, "res"));
        }).collect(Collectors.toList());
        return Utils.replace(Utils.fixPath(result, ValueType.ARRAY, resultPointer), resultPointer,
                Json.createArrayBuilder(res).build());
    };

    StepFunction REDUCE = (ctx, source, result, sourcePointer, resultPointer, expression, engineHolder) -> {
        final JsonValue value = Utils.getValue(source, sourcePointer);
        if (Utils.isEmpty(value)) {
            return result;
        }
        Utils.stream(value).forEach(x -> {
            Utils.eval(engineHolder, expression, x);
        });
        return Utils.replace(Utils.fixPath(result, ValueType.OBJECT, resultPointer), resultPointer,
                Utils.asJsonValue(Utils.getObject(engineHolder, "res")));
    };

    JsonValue apply(TransformationContext ctx, JsonValue source, JsonValue result, String sourcePointer,
            String resultPointer, String expression, EngineHolder engineHolder);
}
