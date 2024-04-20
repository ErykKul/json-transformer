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

    public Transformation(final boolean merge, final boolean selfTranform, final String sourcePointer, final String targetPointer,
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
        final List<String> remainingTargetPointers = targetPointers.size() == 0 ? Collections.emptyList()
                : targetPointers.subList(1, targetPointers.size());
        final String rootOrTargetPointer = targetPointers.size() == 0 ? "" : targetPointers.get(0);
        final JsonObject fixedLocalTo = Utils.fixTargetPath(ctx.getLocalTo(),
                merge ? ValueType.OBJECT : ValueType.ARRAY, rootOrTargetPointer);
        final JsonArray fromArray = ctx.getLocalFrom().getValue(sourcePointers.get(0)).asJsonArray();
        final boolean doFlatten = flatten || targetPointers.size() == 1;

        JsonValue result = fixedLocalTo.getValue(rootOrTargetPointer);
        for (int i = 0; i < fromArray.size(); i++) {
            if (!ValueType.ARRAY.equals(result.getValueType())) {
                result = JsonArray.EMPTY_JSON_ARRAY;
            }
            final JsonArray resultArray = result.asJsonArray();
            final JsonObject resultObject = resultArray.size() > i ? resultArray.get(i).asJsonObject()
                    : JsonObject.EMPTY_JSON_OBJECT;
            final TransformationContext localContext = new TransformationContext(ctx.getGlobalFrom(),
                    ctx.getGlobalTo(), fromArray.get(i).asJsonObject(), resultObject);
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
        return Json.createPointer(rootOrTargetPointer).replace(fixedLocalTo, result);
    }

    private JsonValue doTransform(final TransformationContext ctx, final String sourcePointer,
            final String targetPointer) {
        final JsonValue sourceValue = ctx.getLocalFrom().getValue(sourcePointer);
        if (merge) {
            final JsonObject fixedTo = Utils.fixTargetPath(ctx.getLocalTo(), sourceValue.getValueType(), targetPointer);
            JsonValue result = fixedTo.getValue(targetPointer);
            for (final Value v : values) {
                result = merge(ctx, sourceValue, result, v);
            }
            if (JsonObject.EMPTY_JSON_OBJECT.equals(fixedTo)) {
                return result;
            }
            return Json.createPointer(targetPointer).replace(fixedTo, result);
        } else if (ValueType.ARRAY.equals(sourceValue.getValueType())) {
            throw new RuntimeException(
                    "source must be an object but it is an array, flatten it with \"[i]\" at the end of the sourcePointer field of the transformation or use \"merge\" transformation");
        } else {
            JsonValue result = JsonObject.EMPTY_JSON_OBJECT;
            for (final Value v : values) {
                result = v.copy(ctx, sourceValue.asJsonObject(), result.asJsonObject());
            }
            final JsonObject fixedTo = Utils.fixTargetPath(ctx.getLocalTo(), JsonValue.ValueType.ARRAY, targetPointer);
            if (!ValueType.ARRAY.equals(fixedTo.getValueType())) {
                return result;
            }
            final JsonArray target = Json.createArrayBuilder(fixedTo.getValue(targetPointer).asJsonArray()).add(result)
                    .build();
            return Json.createPointer(targetPointer).replace(fixedTo, target);
        }
    }

    private JsonValue merge(final TransformationContext ctx, final JsonValue sourceValue, final JsonValue result,
            final Value v) {
        switch (sourceValue.getValueType()) {
            case JsonValue.ValueType.ARRAY:
                return arrayMerge(ctx, sourceValue.asJsonArray(), result.asJsonArray(), v);

            case JsonValue.ValueType.OBJECT:
                return v.copy(ctx, sourceValue.asJsonObject(), result.asJsonObject());

            default:
                throw new RuntimeException(
                        "unsupported merge to: " + sourceValue.getValueType() + " from: " + result.getValueType());
        }
    }

    private JsonArray arrayMerge(final TransformationContext ctx, final JsonArray sourceArray,
            final JsonArray resultArray, final Value v) {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        for (int i = 0; i < sourceArray.size(); i++) {
            final JsonObject result = i < resultArray.size() ? resultArray.get(i).asJsonObject()
                    : JsonObject.EMPTY_JSON_OBJECT;
            builder.add(v.copy(ctx, sourceArray.get(i).asJsonObject(), result));
        }
        return builder.build();
    }
}
