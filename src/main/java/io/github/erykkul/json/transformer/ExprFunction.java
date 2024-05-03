// Author: Eryk Kulikowski (2024). Apache 2.0 License

package io.github.erykkul.json.transformer;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.json.Json;
import jakarta.json.JsonValue;
import jakarta.json.JsonValue.ValueType;

@FunctionalInterface
public interface ExprFunction {

    ExprFunction COPY = (ctx, source, result, expression) -> {
        final String[] args = expression.split(",");
        final String from = args.length > 0 ? args[0].trim() : "";
        final String to = args.length > 1 ? args[1].trim() : "";
        return Utils.replace(Utils.fixPath(result, ValueType.OBJECT, to), to, Utils.getValue(source, from));
    };

    ExprFunction GENERATE_UUID = (ctx, source, result, expression) -> {
        return Utils.replace(Utils.fixPath(result, ValueType.OBJECT, expression), expression,
                Json.createValue(UUID.randomUUID().toString()));
    };

    ExprFunction REMOVE = (ctx, source, result, expression) -> {
        return Utils.remove(result, expression);
    };

    ExprFunction SCRIPT = (ctx, source, result, expression) -> {
        if (Utils.isEmpty(source)) {
            return result;
        }
        Utils.eval(ctx.engine(), "res = null");
        Utils.eval(ctx.engine(), expression, source, "x");
        final JsonValue res = Utils.asJsonValue(Utils.getObject(ctx.engine(), "res"));
        if (Utils.isEmpty(res)) {
            return result;
        }
        return res;
    };

    ExprFunction FILTER = (ctx, source, result, expression) -> {
        if (Utils.isEmpty(source)) {
            return result;
        }
        Utils.eval(ctx.engine(), "res = null");
        final List<JsonValue> res = Utils.stream(source).filter(x -> {
            Utils.eval(ctx.engine(), expression, x, "x");
            return Boolean.TRUE.equals(Utils.getObject(ctx.engine(), "res"));
        }).collect(Collectors.toList());
        return Json.createArrayBuilder(res).build();
    };

    ExprFunction MAP = (ctx, source, result, expression) -> {
        if (Utils.isEmpty(source)) {
            return result;
        }
        Utils.eval(ctx.engine(), "res = null");
        final List<JsonValue> res = Utils.stream(source).map(x -> {
            Utils.eval(ctx.engine(), expression, x, "x");
            return Utils.asJsonValue(Utils.getObject(ctx.engine(), "res"));
        }).collect(Collectors.toList());
        return Json.createArrayBuilder(res).build();
    };

    ExprFunction REDUCE = (ctx, source, result, expression) -> {
        if (Utils.isEmpty(source)) {
            return result;
        }
        Utils.eval(ctx.engine(), "res = null");
        Utils.stream(source).forEach(x -> {
            Utils.eval(ctx.engine(), expression, x, "x");
        });
        return Utils.asJsonValue(Utils.getObject(ctx.engine(), "res"));
    };

    JsonValue execute(TransformationCtx ctx, JsonValue source, JsonValue result, String expression);
}
