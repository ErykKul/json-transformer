// Author: Eryk Kulikowski (2024). Apache 2.0 License

package io.github.erykkul.json.transformer;

import java.util.Arrays;
import java.util.List;

import jakarta.json.Json;
import jakarta.json.JsonValue;

public class TransformationStep {

    public static JsonValue execute(final TransformationContext ctx, final JsonValue source, final JsonValue result, final List<String> expressions) {
        if (expressions != null && !expressions.isEmpty()) {
            JsonValue res = result;
            for (final String expression : expressions) {
                res = executeExpresion(ctx, ctx.useResultAsSource() ? res : source, res, expression);
            }
            return res;
        }
        return source;
    }

    private static JsonValue executeExpresion(final TransformationContext ctx, final JsonValue source, final JsonValue result,
            final String expression) {
        if (expression.startsWith("\"")) {
            final String literal = expression.length() > 1
                    ? expression.substring(1, expression.length() - 1)
                    : "";
            return Json.createValue(literal);
        } else if (!"".equals(expression)) {
            final String[] functionParts = expression.split("\\(");
            final String functionName = functionParts.length > 0 ? functionParts[0] : "";
            final String str = functionParts.length > 1
                    ? String.join("(", Arrays.copyOfRange(functionParts, 1, functionParts.length))
                    : "";
            final String functionArg = str.length() > 0 ? str.substring(0, str.length() - 1) : "";
            final StepFunction func = ctx.getFunctions().get(functionName);
            return func == null ? result
                    : func.apply(ctx, source, result, functionArg);
        }
        return result;
    }
}
