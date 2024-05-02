// Author: Eryk Kulikowski (2024). Apache 2.0 License

package io.github.erykkul.json.transformer;

import static jakarta.json.JsonValue.ValueType.OBJECT;

import java.util.Arrays;
import java.util.List;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

public class TransformationStep {
    private final String sourcePointer;
    private final String resultPointer;
    private final List<String> expressions;
    private final EngineHolder engineHolder;

    public TransformationStep(final String sourcePointer, final String resultPointer, final List<String> expressions,
            final EngineHolder engineHolder) {
        this.sourcePointer = sourcePointer;
        this.resultPointer = resultPointer;
        this.expressions = expressions;
        this.engineHolder = engineHolder;
    }

    public JsonValue execute(final TransformationContext ctx, final JsonValue source, final JsonValue result) {
        if (expressions != null && !expressions.isEmpty()) {
            JsonValue res = result;
            for (final String expression : expressions) {
                res = executeExpresion(ctx, ctx.useResultAsSource() ? res : source, res, expression, engineHolder);
            }
            return res;
        }
        final JsonValue res = Utils.getValue(ctx.useResultAsSource() ? result : source, sourcePointer);
        if (Utils.isEmpty(res)) {
            return result;
        }
        if (!"".equals(sourcePointer) && "".equals(resultPointer)) {
            return res;
        }
        return Utils.add(Utils.fixPath(result, OBJECT, resultPointer), resultPointer, res);
    }

    public JsonObject toJsonObject() {
        return Json.createObjectBuilder().add("sourcePointer", sourcePointer).add("resultPointer", resultPointer)
                .add("expressions", Json.createArrayBuilder(expressions).build()).build();
    }

    private JsonValue executeExpresion(final TransformationContext ctx, final JsonValue source, final JsonValue result,
            final String expression, final EngineHolder engineHolder) {
        if (expression.startsWith("\"")) {
            final String literal = expression.length() > 1
                    ? expression.substring(1, expression.length() - 1)
                    : "";
            return Utils.add(Utils.fixPath(result, OBJECT, resultPointer), resultPointer, Json.createValue(literal));
        } else if (!"".equals(expression)) {
            final String[] functionParts = expression.split("\\(");
            final String functionName = functionParts.length > 0 ? functionParts[0] : "";
            final String str = functionParts.length > 1
                    ? String.join("(", Arrays.copyOfRange(functionParts, 1, functionParts.length))
                    : "";
            final String functionArg = str.length() > 0 ? str.substring(0, str.length() - 1) : "";
            final StepFunction func = ctx.getFunctions().get(functionName);
            return func == null ? result
                    : func.apply(ctx, source, result, sourcePointer, resultPointer, functionArg, engineHolder);
        }
        return result;
    }
}
