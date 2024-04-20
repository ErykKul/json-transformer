package com.github.erykkul.json.transformer;

import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.JsonValue.ValueType;

public class Value {
    private final String valuePointer;
    private final String valueExpression;
    private final Map<String, CustomValueCopyFunction> functions;

    public Value(final String valuePointer, final String valueExpression,
            final Map<String, CustomValueCopyFunction> functions) {
        this.valuePointer = valuePointer;
        this.valueExpression = valueExpression;
        this.functions = functions;
    }

    public JsonValue copy(final TransformationContext ctx, final JsonObject from, final JsonObject to) {
        if (valueExpression.startsWith("\"")) {
            final String literal = valueExpression.substring(1, valueExpression.length() - 1);
            return Json.createPointer(valuePointer).add(Utils.fixTargetPath(to, ValueType.OBJECT, valuePointer),
                    Json.createValue(literal));
        } else if (valueExpression.startsWith("func(")) {
            final String function = valueExpression.substring("func(".length(), valueExpression.length() - 1);
            final String[] functionParts = function.split("\\(");
            final String functionName = functionParts[0];
            final String functionArg = functionParts[1].substring(0, functionParts[1].length() - 1);
            return this.functions.get(functionName).copy(ctx, from, to, valuePointer, functionArg);
        } else if (!"".equals(valueExpression) && "".equals(valuePointer)) {
            return from.getValue(valueExpression);
        }
        return Json.createPointer(valuePointer).add(Utils.fixTargetPath(to, ValueType.OBJECT, valuePointer),
                from.getValue(valueExpression));
    }
}
