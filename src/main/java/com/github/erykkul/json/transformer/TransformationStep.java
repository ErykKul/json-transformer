package com.github.erykkul.json.transformer;

import static jakarta.json.JsonValue.ValueType.OBJECT;

import java.util.Arrays;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

public class TransformationStep {
    private final String valuePointer;
    private final String valueExpression;

    public TransformationStep(final String valuePointer, final String valueExpression) {
        this.valuePointer = valuePointer;
        this.valueExpression = valueExpression;
    }

    public JsonValue execute(final TransformationContext ctx, final JsonValue source, final JsonValue result) {
        if (valueExpression.startsWith("\"")) {
            final String literal = valueExpression.length() > 1
                    ? valueExpression.substring(1, valueExpression.length() - 1)
                    : "";
            return Utils.add(Utils.fixTargetPath(result, OBJECT, valuePointer), valuePointer, Json.createValue(literal));
        } else if (valueExpression.startsWith("func(")) {
            final String function = valueExpression.length() > "func()".length()
                    ? valueExpression.substring("func(".length(), valueExpression.length() - 1)
                    : "";
            final String[] functionParts = function.split("\\(");
            final String functionName = functionParts.length > 0 ? functionParts[0] : "";
            final String str = functionParts.length > 1
                    ? String.join("(", Arrays.copyOfRange(functionParts, 1, functionParts.length))
                    : "";
            final String functionArg = str.length() > 0 ? str.substring(0, str.length() - 1) : "";
            final TransformationStepFunction func = ctx.getFunctions().get(functionName);
            return func == null ? result : func.apply(ctx, source, result, valuePointer, functionArg);
        }
        final JsonValue res = Utils.getValue(source, valueExpression);
        if (Utils.isEmpty(res)) {
            return result;
        }
        if (!"".equals(valueExpression) && "".equals(valuePointer)) {
            return res;
        }
        return Utils.add(Utils.fixTargetPath(result, OBJECT, valuePointer), valuePointer, res);
    }

    public JsonObject toJsonObject() {
        return Json.createObjectBuilder().add("valuePointer", valuePointer).add("valueExpression", valueExpression)
                .build();
    }
}
