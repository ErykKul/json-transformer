package com.github.erykkul.json.transformer;

import static jakarta.json.JsonValue.ValueType.OBJECT;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

public class Value {
    private final String valuePointer;
    private final String valueExpression;

    public Value(final String valuePointer, final String valueExpression) {
        this.valuePointer = valuePointer;
        this.valueExpression = valueExpression;
    }

    public JsonValue copy(final TransformationContext ctx, final JsonValue from, final JsonValue to) {
        if (valueExpression.startsWith("\"")) {
            final String literal = valueExpression.substring(1, valueExpression.length() - 1);
            return Utils.add(Utils.fixTargetPath(to, OBJECT, valuePointer), valuePointer, Json.createValue(literal));
        } else if (valueExpression.startsWith("func(")) {
            final String function = valueExpression.substring("func(".length(), valueExpression.length() - 1);
            final String[] functionParts = function.split("\\(");
            final String functionName = functionParts[0];
            final String functionArg = functionParts[1].substring(0, functionParts[1].length() - 1);
            return ctx.getFunctions().get(functionName).copy(ctx, from, to, valuePointer, functionArg);
        }
        final JsonValue result = Utils.getValue(from, valueExpression);
        if (Utils.isEmpty(result)) {
            return to;
        }
        if (!"".equals(valueExpression) && "".equals(valuePointer)) {
            return result;
        }
        return Utils.add(Utils.fixTargetPath(to, OBJECT, valuePointer), valuePointer, result);
    }

    public JsonObject asJson() {
        return Json.createObjectBuilder().add("valuePointer", valuePointer).add("valueExpression", valueExpression)
                .build();
    }
}
