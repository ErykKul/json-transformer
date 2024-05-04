// Author: Eryk Kulikowski (2024). Apache 2.0 License

package io.github.erykkul.json.transformer;

import static jakarta.json.JsonValue.EMPTY_JSON_ARRAY;
import static jakarta.json.JsonValue.EMPTY_JSON_OBJECT;
import static jakarta.json.JsonValue.ValueType.ARRAY;
import static jakarta.json.JsonValue.ValueType.OBJECT;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

public class Transformation {

    public static JsonValue executeExpressions(final TransformationCtx ctx, final JsonValue source,
            final JsonValue result, final List<String> expressions) {
        if (expressions != null && !expressions.isEmpty()) {
            JsonValue res = result;
            for (final String expression : expressions) {
                res = executeExpresion(ctx, ctx.useResultAsSource() ? res : source, res, expression);
            }
            return res;
        }
        return source;
    }

    private static JsonValue executeExpresion(final TransformationCtx ctx, final JsonValue source,
            final JsonValue result, final String expression) {
        if (expression.startsWith("\"")) {
            final String literal = expression.length() > 1 ? expression.substring(1, expression.length() - 1) : "";
            return Json.createValue(literal);
        } else if (!"".equals(expression)) {
            final String[] functionParts = expression.split("\\(");
            final String functionName = functionParts.length > 0 ? functionParts[0] : "";
            final String str = functionParts.length > 1
                    ? String.join("(", Arrays.copyOfRange(functionParts, 1, functionParts.length))
                    : "";
            final String functionArg = str.length() > 0 ? str.substring(0, str.length() - 1) : "";
            final ExprFunction func = ctx.getFunctions().get(functionName);
            return func == null ? result : func.execute(ctx, source, result, functionArg);
        }
        return result;
    }

    private final boolean append;
    private final boolean useResultAsSource;
    private final String sourcePointer;
    private final String resultPointer;
    private final List<String> expressions;
    private final Map<String, ExprFunction> functions;

    public Transformation(final boolean append, final boolean useResultAsSource, final String sourcePointer,
            final String resultPointer, final List<String> expressions,
            final Map<String, ExprFunction> functions) {
        this.append = append;
        this.useResultAsSource = useResultAsSource;
        this.sourcePointer = sourcePointer;
        this.resultPointer = resultPointer;
        this.expressions = expressions;
        this.functions = functions;
    }

    public JsonObject transform(final JsonObject source, final JsonObject result, final EngineHolder engineHolder) {
        final JsonObject srcOrRes = useResultAsSource ? result : source;
        final TransformationCtx ctx = new TransformationCtx(srcOrRes, result, srcOrRes, result, this, engineHolder);
        if (!sourcePointer.contains("[i]")) {
            return doTransform(ctx, sourcePointer, resultPointer).asJsonObject();
        }
        final List<String> sourcePointers = Arrays.asList(sourcePointer.split("\\[i\\]", -1));
        final List<String> resultPointers = Arrays.asList(resultPointer.split("\\[i\\]", -1));
        return transform(ctx, sourcePointers, resultPointers, false, engineHolder).asJsonObject();
    }

    public JsonObject toJsonObject() {
        return Json.createObjectBuilder().add("append", append).add("useResultAsSource", useResultAsSource)
                .add("sourcePointer", sourcePointer).add("resultPointer", resultPointer)
                .add("expressions", Json.createArrayBuilder(expressions)).build();
    }

    public Map<String, ExprFunction> getFunctions() {
        return functions;
    }

    public boolean useResultAsSource() {
        return useResultAsSource;
    }

    private JsonValue transform(final TransformationCtx ctx, final List<String> sourcePointers,
            final List<String> resultPointers, final boolean flatten, final EngineHolder engineHolder) {
        if (sourcePointers.size() == 1) {
            return doTransform(ctx, sourcePointers.get(0), String.join("[i]", resultPointers));
        }
        final JsonValue sourceValue = Utils.getValue(ctx.getLocalSource(), sourcePointers.get(0));
        final String rootOrResultPointer = resultPointers.isEmpty() ? "" : resultPointers.get(0);
        final JsonValue fixedResult = Utils.fixPath(ctx.getLocalResult(), append ? ARRAY : OBJECT, rootOrResultPointer);
        if (!Utils.isArray(sourceValue)) {
            return fixedResult;
        }

        final List<String> remainingSourcePointers = sourcePointers.subList(1, sourcePointers.size());
        final List<String> remainingResultPointers = resultPointers.isEmpty() ? Collections.emptyList()
                : resultPointers.subList(1, resultPointers.size());
        final boolean doFlatten = flatten || resultPointers.size() == 1;
        final JsonArray sourceArray = sourceValue.asJsonArray();
        JsonValue result = Utils.getValue(fixedResult, rootOrResultPointer);
        int flattenedMergeIdx = 0;
        for (int i = 0; i < sourceArray.size(); i++) {
            result = Utils.isArray(result) ? result : EMPTY_JSON_ARRAY;
            final JsonArray resultArray = result.asJsonArray();
            final JsonValue resultObject = resultArray.size() > i ? resultArray.get(i) : EMPTY_JSON_OBJECT;
            final TransformationCtx localContext = new TransformationCtx(ctx.getGlobalSource(), ctx.getGlobalResult(),
                    sourceArray.get(i), resultObject, this, engineHolder);
            final JsonValue transformed = transform(localContext, remainingSourcePointers, remainingResultPointers,
                    doFlatten, engineHolder);
            if (doFlatten && !append && Utils.isArray(transformed)) {
                result = mergeValues(transformed.asJsonArray(), result.asJsonArray(), flattenedMergeIdx);
                flattenedMergeIdx += transformed.asJsonArray().size();
            } else if (!append && !Utils.isArray(transformed) && resultArray.size() > i) {
                result = Json.createArrayBuilder(resultArray).set(i, transformed).build();
            } else if (doFlatten && Utils.isArray(transformed)) {
                result = Json.createArrayBuilder(resultArray)
                        .addAll(Json.createArrayBuilder(transformed.asJsonArray())).build();
            } else {
                result = Json.createArrayBuilder(resultArray).add(transformed).build();
            }
        }
        return Utils.replace(fixedResult, rootOrResultPointer, result);
    }

    private JsonValue doTransform(final TransformationCtx ctx, final String sourcePointer, final String resultPointer) {
        final JsonValue sourceValue = Utils.getValue(ctx.getLocalSource(), sourcePointer);
        final JsonValue fixedResult = Utils.fixPath(ctx.getLocalResult(), sourceValue.getValueType(), resultPointer);
        if (append) {
            final JsonValue result = executeExpressions(ctx, sourceValue, EMPTY_JSON_OBJECT, expressions);
            final JsonValue resultArray = Utils.getValue(fixedResult, resultPointer);
            if (!Utils.isArray(resultArray)) {
                return result;
            }
            return Utils.replace(fixedResult, resultPointer,
                    Json.createArrayBuilder(resultArray.asJsonArray()).add(result).build());
        } else {
            final JsonValue result = executeExpressions(ctx, sourceValue, Utils.getValue(fixedResult, resultPointer),
                    expressions);
            if (Utils.isEmpty(fixedResult)) {
                return result;
            }
            return Utils.replace(fixedResult, resultPointer, result);
        }
    }

    private JsonArray mergeValues(final JsonArray source, final JsonArray result, final int startIdx) {
        final JsonArrayBuilder builder = Json.createArrayBuilder(result);
        for (int i = 0; i < source.size(); i++) {
            if (result.size() > startIdx + i) {
                builder.set(startIdx + i, mergeValue(source.get(i), result.get(startIdx + i)));
            } else {
                builder.add(source.get(i));
            }
        }
        return builder.build();
    }

    private JsonValue mergeValue(final JsonValue source, final JsonValue result) {
        if (Utils.isArray(source) && Utils.isArray(result)) {
            return mergeValues(source.asJsonArray(), result.asJsonArray(), 0);
        }
        if (Utils.isObject(source) && Utils.isObject(result)) {
            return Json.createObjectBuilder(result.asJsonObject())
                    .addAll(Json.createObjectBuilder(source.asJsonObject())).build();
        }
        return result;
    }
}
