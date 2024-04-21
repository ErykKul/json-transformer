package com.github.erykkul.json.transformer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.JsonValue.ValueType;

public class Transformation {

    private final boolean merge;
    private final boolean selfTranform;
    private final String sourcePointer;
    private final String targetPointer;
    private final List<Value> values;

    public Transformation(final boolean merge, final boolean selfTranform, final String sourcePointer,
            final String targetPointer,
            final List<Value> values) {
        this.merge = merge;
        this.selfTranform = selfTranform;
        this.sourcePointer = sourcePointer;
        this.targetPointer = targetPointer;
        this.values = values;
    }

    public JsonObject transform(final JsonObject from, final JsonObject to) {
        final JsonObject selfOrFrom = selfTranform ? to : from;
        final TransformationContext ctx = new TransformationContext(selfOrFrom, to, selfOrFrom, to);
        if (!sourcePointer.contains("[i]")) {
            return doTransform(ctx, sourcePointer, targetPointer).asJsonObject();
        }
        final List<String> sourcePointers = Arrays.asList(sourcePointer.split("\\[i\\]", -1));
        final List<String> targetPointers = Arrays.asList(targetPointer.split("\\[i\\]", -1));
        return transform(ctx, sourcePointers, targetPointers, false).asJsonObject();
    }

    private JsonValue transform(final TransformationContext ctx, final List<String> sourcePointers,
            final List<String> targetPointers, final boolean flatten) {
        if (sourcePointers.size() == 1) {
            return doTransform(ctx, sourcePointers.get(0), String.join("[i]", targetPointers));
        }

        final List<String> remainingSourcePointers = sourcePointers.subList(1, sourcePointers.size());
        final List<String> remainingTargetPointers = targetPointers.isEmpty() ? Collections.emptyList()
                : targetPointers.subList(1, targetPointers.size());
        final String rootOrTargetPointer = targetPointers.isEmpty() ? "" : targetPointers.get(0);
        final JsonValue fixedLocalTo = Utils.fixTargetPath(ctx.getLocalTo(),
                merge ? ValueType.OBJECT : ValueType.ARRAY, rootOrTargetPointer);
        final JsonValue fromValue = Utils.getValue(ctx.getLocalFrom(), sourcePointers.get(0));
        if (!ValueType.ARRAY.equals(fromValue.getValueType())) {
            return fixedLocalTo;
        }
        final JsonArray fromArray = fromValue.asJsonArray();
        final boolean doFlatten = flatten || targetPointers.size() == 1;

        JsonValue result = Utils.getValue(fixedLocalTo, rootOrTargetPointer);
        for (int i = 0; i < fromArray.size(); i++) {
            if (!ValueType.ARRAY.equals(result.getValueType())) {
                result = JsonArray.EMPTY_JSON_ARRAY;
            }
            final JsonArray resultArray = result.asJsonArray();
            final JsonValue resultObject = resultArray.size() > i ? resultArray.get(i) : JsonObject.EMPTY_JSON_OBJECT;
            final TransformationContext localContext = new TransformationContext(ctx.getGlobalFrom(),
                    ctx.getGlobalTo(), fromArray.get(i), resultObject);
            final JsonValue transformed = transform(localContext, remainingSourcePointers, remainingTargetPointers,
                    doFlatten);

            if (merge && !ValueType.ARRAY.equals(transformed.getValueType()) && resultArray.size() > i) {
                result = Json.createArrayBuilder(resultArray).set(i, transformed).build();
            } else if (doFlatten && ValueType.ARRAY.equals(transformed.getValueType())) {
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
        if (Utils.isEmpty(sourceValue)) {
            return ctx.getLocalTo();
        }
        if (merge) {
            final JsonValue fixedTo = Utils.fixTargetPath(ctx.getLocalTo(), sourceValue.getValueType(), targetPointer);
            JsonValue result = Utils.getValue(fixedTo, targetPointer);
            for (final Value v : values) {
                result = merge(ctx, sourceValue, result, v);
            }
            if (Utils.isEmpty(fixedTo)) {
                return result;
            }
            return Utils.replace(fixedTo, targetPointer, result);
        } else if (ValueType.ARRAY.equals(sourceValue.getValueType())) {
            throw new RuntimeException(
                    "source must be an object but it is an array, flatten it with \"[i]\" at the end of the sourcePointer field of the transformation or use \"merge\" transformation");
        } else {
            JsonValue result = JsonObject.EMPTY_JSON_OBJECT;
            for (final Value v : values) {
                result = v.copy(ctx, sourceValue, result);
            }
            final JsonValue fixedTo = Utils.fixTargetPath(ctx.getLocalTo(), JsonValue.ValueType.ARRAY, targetPointer);
            final JsonValue targetArray = Utils.getValue(fixedTo, targetPointer);
            if (!ValueType.ARRAY.equals(targetArray.getValueType())) {
                return result;
            }
            final JsonArray target = Json.createArrayBuilder(targetArray.asJsonArray()).add(result).build();
            return Utils.replace(fixedTo, targetPointer, target);
        }
    }

    private JsonValue merge(final TransformationContext ctx, final JsonValue sourceValue, final JsonValue result,
            final Value v) {
        if (ValueType.ARRAY.equals(sourceValue.getValueType())) {
            return arrayMerge(ctx, sourceValue.asJsonArray(),
                    ValueType.ARRAY.equals(result.getValueType()) ? result.asJsonArray() : JsonArray.EMPTY_JSON_ARRAY,
                    v);
        }
        return v.copy(ctx, sourceValue, result);
    }

    private JsonArray arrayMerge(final TransformationContext ctx, final JsonArray sourceArray,
            final JsonArray resultArray, final Value v) {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        for (int i = 0; i < sourceArray.size(); i++) {
            final JsonValue result = i < resultArray.size() ? resultArray.get(i) : JsonObject.EMPTY_JSON_OBJECT;
            builder.add(v.copy(ctx, sourceArray.get(i), result));
        }
        return builder.build();
    }
}
