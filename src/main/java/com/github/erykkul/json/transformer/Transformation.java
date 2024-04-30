package com.github.erykkul.json.transformer;

import static jakarta.json.JsonValue.EMPTY_JSON_ARRAY;
import static jakarta.json.JsonValue.EMPTY_JSON_OBJECT;
import static jakarta.json.JsonValue.ValueType.ARRAY;
import static jakarta.json.JsonValue.ValueType.OBJECT;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

public class Transformation {
    private final boolean append;
    private final boolean useResultAsSource;
    private final String sourcePointer;
    private final String resultPointer;
    private final List<TransformationStep> steps;
    private final Map<String, TransformationStepFunction> functions;

    public Transformation(final boolean append, final boolean useResultAsSource, final String sourcePointer,
            final String resultPointer, final List<TransformationStep> steps, final Map<String, TransformationStepFunction> functions) {
        this.append = append;
        this.useResultAsSource = useResultAsSource;
        this.sourcePointer = sourcePointer;
        this.resultPointer = resultPointer;
        this.steps = steps;
        this.functions = functions;
    }

    public JsonObject transform(final JsonObject source, final JsonObject result) {
        final JsonObject sourceOrResult = useResultAsSource ? result : source;
        final TransformationContext ctx = new TransformationContext(sourceOrResult, result, sourceOrResult, result, this);
        if (!sourcePointer.contains("[i]")) {
            return doTransform(ctx, sourcePointer, resultPointer).asJsonObject();
        }
        final List<String> sourcePointers = Arrays.asList(sourcePointer.split("\\[i\\]", -1));
        final List<String> resultPointers = Arrays.asList(resultPointer.split("\\[i\\]", -1));
        return transform(ctx, sourcePointers, resultPointers, false).asJsonObject();
    }

    public JsonObject toJsonObject() {
        return Json.createObjectBuilder().add("append", append).add("useResultAsSource", useResultAsSource)
                .add("sourcePointer", sourcePointer).add("resultPointer", resultPointer)
                .add("steps",
                        Json.createArrayBuilder(steps.stream().map(TransformationStep::toJsonObject).collect(Collectors.toList())))
                .build();
    }

    public Map<String, TransformationStepFunction> getFunctions() {
        return functions;
    }

    private JsonValue transform(final TransformationContext ctx, final List<String> sourcePointers,
            final List<String> resultPointers, final boolean flatten) {
        if (sourcePointers.size() == 1) {
            return doTransform(ctx, sourcePointers.get(0), String.join("[i]", resultPointers));
        }
        final JsonValue sourceValue = Utils.getValue(ctx.getLocalSource(), sourcePointers.get(0));
        final String rootOrResultPointer = resultPointers.isEmpty() ? "" : resultPointers.get(0);
        final JsonValue fixedLocalResult = Utils.fixTargetPath(ctx.getLocalResult(), append ? ARRAY : OBJECT,
                rootOrResultPointer);
        if (!Utils.isArray(sourceValue)) {
            return fixedLocalResult;
        }

        final List<String> remainingSourcePointers = sourcePointers.subList(1, sourcePointers.size());
        final List<String> remainingResultPointers = resultPointers.isEmpty() ? Collections.emptyList()
                : resultPointers.subList(1, resultPointers.size());
        final boolean doFlatten = flatten || resultPointers.size() == 1;
        final JsonArray sourceArray = sourceValue.asJsonArray();
        JsonValue result = Utils.getValue(fixedLocalResult, rootOrResultPointer);
        int flattenedMergeIdx = 0;
        for (int i = 0; i < sourceArray.size(); i++) {
            result = Utils.isArray(result) ? result : EMPTY_JSON_ARRAY;
            final JsonArray resultArray = result.asJsonArray();
            final JsonValue resultObject = resultArray.size() > i ? resultArray.get(i) : EMPTY_JSON_OBJECT;
            final TransformationContext localContext = new TransformationContext(ctx.getGlobalSource(),
                    ctx.getGlobalResult(), sourceArray.get(i), resultObject, this);
            final JsonValue transformed = transform(localContext, remainingSourcePointers, remainingResultPointers,
                    doFlatten);
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
        return Utils.replace(fixedLocalResult, rootOrResultPointer, result);
    }

    private JsonValue doTransform(final TransformationContext ctx, final String sourcePointer,
            final String resultPointer) {
        final JsonValue sourceValue = Utils.getValue(ctx.getLocalSource(), sourcePointer);
        final JsonValue fixedResult = Utils.fixTargetPath(ctx.getLocalResult(), sourceValue.getValueType(), resultPointer);
        if (Utils.isEmpty(sourceValue)) {
            return ctx.getLocalResult();
        }
        if (!append || Utils.isArray(sourceValue)) {
            JsonValue result = Utils.getValue(fixedResult, resultPointer);
            for (final TransformationStep step : steps) {
                result = copy(ctx, sourceValue, result, step);
            }
            if (Utils.isEmpty(fixedResult)) {
                return result;
            }
            return Utils.replace(fixedResult, resultPointer, result);
        } else {
            JsonValue result = EMPTY_JSON_OBJECT;
            for (final TransformationStep step : steps) {
                result = step.execute(ctx, sourceValue, result);
            }
            final JsonValue targetArray = Utils.getValue(fixedResult, resultPointer);
            if (!Utils.isArray(targetArray)) {
                return result;
            }
            final JsonArray target = Json.createArrayBuilder(targetArray.asJsonArray()).add(result).build();
            return Utils.replace(fixedResult, resultPointer, target);
        }
    }

    private JsonValue copy(final TransformationContext ctx, final JsonValue sourceValue, final JsonValue result,
            final TransformationStep step) {
        if (Utils.isArray(sourceValue)) {
            return arrayCopy(ctx, sourceValue.asJsonArray(),
                    Utils.isArray(result) ? result.asJsonArray() : EMPTY_JSON_ARRAY, step);
        }
        return step.execute(ctx, sourceValue, result);
    }

    private JsonArray arrayCopy(final TransformationContext ctx, final JsonArray sourceArray,
            final JsonArray resultArray, final TransformationStep step) {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        if (append) {
            builder.addAll(Json.createArrayBuilder(resultArray));
        }
        for (int i = 0; i < sourceArray.size(); i++) {
            final JsonValue result = !append && i < resultArray.size() ? resultArray.get(i) : EMPTY_JSON_OBJECT;
            builder.add(step.execute(ctx, sourceArray.get(i), result));
        }
        return builder.build();
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
            return Json.createObjectBuilder(result.asJsonObject()).addAll(Json.createObjectBuilder(source.asJsonObject()))
                    .build();
        }
        return result;
    }
}
