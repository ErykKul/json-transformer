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
    private final boolean merge;
    private final boolean selfTranform;
    private final String sourcePointer;
    private final String targetPointer;
    private final List<Value> values;
    private final Map<String, ValueFunction> functions;

    public Transformation(final boolean merge, final boolean selfTranform, final String sourcePointer,
            final String targetPointer, final List<Value> values, final Map<String, ValueFunction> functions) {
        this.merge = merge;
        this.selfTranform = selfTranform;
        this.sourcePointer = sourcePointer;
        this.targetPointer = targetPointer;
        this.values = values;
        this.functions = functions;
    }

    public JsonObject transform(final JsonObject from, final JsonObject to) {
        final JsonObject selfOrFrom = selfTranform ? to : from;
        final TransformationContext ctx = new TransformationContext(selfOrFrom, to, selfOrFrom, to, this);
        if (!sourcePointer.contains("[i]")) {
            return doTransform(ctx, sourcePointer, targetPointer).asJsonObject();
        }
        final List<String> sourcePointers = Arrays.asList(sourcePointer.split("\\[i\\]", -1));
        final List<String> targetPointers = Arrays.asList(targetPointer.split("\\[i\\]", -1));
        return transform(ctx, sourcePointers, targetPointers, false).asJsonObject();
    }

    public JsonObject asJson() {
        return Json.createObjectBuilder().add("merge", merge).add("selfTranform", selfTranform)
                .add("sourcePointer", sourcePointer).add("targetPointer", targetPointer)
                .add("values",
                        Json.createArrayBuilder(values.stream().map(Value::asJson).collect(Collectors.toList())))
                .build();
    }

    public Map<String, ValueFunction> getFunctions() {
        return functions;
    }

    private JsonValue transform(final TransformationContext ctx, final List<String> sourcePointers,
            final List<String> targetPointers, final boolean flatten) {
        if (sourcePointers.size() == 1) {
            return doTransform(ctx, sourcePointers.get(0), String.join("[i]", targetPointers));
        }
        final JsonValue fromValue = Utils.getValue(ctx.getLocalFrom(), sourcePointers.get(0));
        final String rootOrTargetPointer = targetPointers.isEmpty() ? "" : targetPointers.get(0);
        final JsonValue fixedLocalTo = Utils.fixTargetPath(ctx.getLocalTo(), merge ? OBJECT : ARRAY,
                rootOrTargetPointer);
        if (!Utils.isArray(fromValue)) {
            return fixedLocalTo;
        }

        final List<String> remainingSourcePointers = sourcePointers.subList(1, sourcePointers.size());
        final List<String> remainingTargetPointers = targetPointers.isEmpty() ? Collections.emptyList()
                : targetPointers.subList(1, targetPointers.size());
        final boolean doFlatten = flatten || targetPointers.size() == 1;
        final JsonArray fromArray = fromValue.asJsonArray();
        JsonValue result = Utils.getValue(fixedLocalTo, rootOrTargetPointer);
        int flattenedMergeIdx = 0;
        for (int i = 0; i < fromArray.size(); i++) {
            result = Utils.isArray(result) ? result : EMPTY_JSON_ARRAY;
            final JsonArray resultArray = result.asJsonArray();
            final JsonValue resultObject = resultArray.size() > i ? resultArray.get(i) : EMPTY_JSON_OBJECT;
            final TransformationContext localContext = new TransformationContext(ctx.getGlobalFrom(),
                    ctx.getGlobalTo(), fromArray.get(i), resultObject, this);
            final JsonValue transformed = transform(localContext, remainingSourcePointers, remainingTargetPointers,
                    doFlatten);
            if (doFlatten && merge && Utils.isArray(transformed)) {
                result = mergeValues(transformed.asJsonArray(), result.asJsonArray(), flattenedMergeIdx);
                flattenedMergeIdx += transformed.asJsonArray().size();
            } else if (merge && !Utils.isArray(transformed) && resultArray.size() > i) {
                result = Json.createArrayBuilder(resultArray).set(i, transformed).build();
            } else if (doFlatten && Utils.isArray(transformed)) {
                result = Json.createArrayBuilder(resultArray)
                        .addAll(Json.createArrayBuilder(transformed.asJsonArray())).build();
            } else {
                result = Json.createArrayBuilder(resultArray).add(transformed).build();
            }
        }
        return Utils.replace(fixedLocalTo, rootOrTargetPointer, result);
    }

    private JsonValue doTransform(final TransformationContext ctx, final String sourcePointer,
            final String targetPointer) {
        final JsonValue sourceValue = Utils.getValue(ctx.getLocalFrom(), sourcePointer);
        final JsonValue fixedTo = Utils.fixTargetPath(ctx.getLocalTo(), sourceValue.getValueType(), targetPointer);
        if (Utils.isEmpty(sourceValue)) {
            return ctx.getLocalTo();
        }
        if (merge || Utils.isArray(sourceValue)) {
            JsonValue result = Utils.getValue(fixedTo, targetPointer);
            for (final Value v : values) {
                result = copy(ctx, sourceValue, result, v);
            }
            if (Utils.isEmpty(fixedTo)) {
                return result;
            }
            return Utils.replace(fixedTo, targetPointer, result);
        } else {
            JsonValue result = EMPTY_JSON_OBJECT;
            for (final Value v : values) {
                result = v.copy(ctx, sourceValue, result);
            }
            final JsonValue targetArray = Utils.getValue(fixedTo, targetPointer);
            if (!Utils.isArray(targetArray)) {
                return result;
            }
            final JsonArray target = Json.createArrayBuilder(targetArray.asJsonArray()).add(result).build();
            return Utils.replace(fixedTo, targetPointer, target);
        }
    }

    private JsonValue copy(final TransformationContext ctx, final JsonValue sourceValue, final JsonValue result,
            final Value v) {
        if (Utils.isArray(sourceValue)) {
            return arrayCopy(ctx, sourceValue.asJsonArray(),
                    Utils.isArray(result) ? result.asJsonArray() : EMPTY_JSON_ARRAY, v);
        }
        return v.copy(ctx, sourceValue, result);
    }

    private JsonArray arrayCopy(final TransformationContext ctx, final JsonArray sourceArray,
            final JsonArray resultArray, final Value v) {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        if (!merge) {
            builder.addAll(Json.createArrayBuilder(resultArray));
        }
        for (int i = 0; i < sourceArray.size(); i++) {
            final JsonValue result = merge && i < resultArray.size() ? resultArray.get(i) : EMPTY_JSON_OBJECT;
            builder.add(v.copy(ctx, sourceArray.get(i), result));
        }
        return builder.build();
    }

    private JsonArray mergeValues(final JsonArray from, final JsonArray to, final int startIdx) {
        final JsonArrayBuilder builder = Json.createArrayBuilder(to);
        for (int i = 0; i < from.size(); i++) {
            if (to.size() > startIdx + i) {
                builder.set(startIdx + i, mergeValue(from.get(i), to.get(startIdx + i)));
            } else {
                builder.add(mergeValue(from.get(i), to.get(startIdx + i)));
            }
        }
        return builder.build();
    }

    private JsonValue mergeValue(final JsonValue from, final JsonValue to) {
        if (Utils.isArray(from) && Utils.isArray(to)) {
            return mergeValues(from.asJsonArray(), to.asJsonArray(), 0);
        }
        if (Utils.isObject(from) && Utils.isObject(to)) {
            return Json.createObjectBuilder(to.asJsonObject()).addAll(Json.createObjectBuilder(from.asJsonObject()))
                    .build();
        }
        return to;
    }
}
