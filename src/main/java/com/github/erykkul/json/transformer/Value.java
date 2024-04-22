package com.github.erykkul.json.transformer;

import static jakarta.json.JsonValue.ValueType.OBJECT;

import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonValue;

public class Value {
    private final String valuePointer;
    private final String valueExpression;
    private final Map<String, ValueFunction> functions;

    public Value(final String valuePointer, final String valueExpression, final Map<String, ValueFunction> functions) {
        this.valuePointer = valuePointer;
        this.valueExpression = valueExpression;
        this.functions = functions;
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
            return this.functions.get(functionName).copy(ctx, from, to, valuePointer, functionArg);
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
}
