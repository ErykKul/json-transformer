package com.github.erykkul.json.transformer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.json.Json;
import jakarta.json.JsonNumber;
import jakarta.json.JsonValue;
import jakarta.json.JsonValue.ValueType;

@FunctionalInterface
public interface CustomValueCopyFunction {

    CustomValueCopyFunction FILTER_UNIQUE = (ctx, from, to, valuePointer, funcArg) -> {
        final JsonValue value = Utils.getValue(from, valuePointer);
        if (!Utils.isArray(value) || Utils.isEmpty(value)) {
            return to;
        }
        final Set<JsonValue> items = new HashSet<>();
        final List<JsonValue> filtered = value.asJsonArray().stream()
                .filter(x -> items.add(Utils.getValue(x, funcArg)))
                .collect(Collectors.toList());
        return Utils.replace(to, valuePointer, Json.createArrayBuilder(filtered).build());
    };

    CustomValueCopyFunction GENERATE_UUID = (ctx, from, to, valuePointer, funcArg) -> {
        return Utils.replace(to, valuePointer, Json.createValue(UUID.randomUUID().toString()));
    };

    CustomValueCopyFunction CONCAT = (ctx, from, to, valuePointer, funcArg) -> {
        final JsonValue value = Utils.getValue(from, valuePointer);
        if (Utils.isEmpty(value)) {
            return to;
        }
        final JsonValue empty = Json.createValue("");
        final BiFunction<JsonValue, JsonValue, String> separator = (x, y) -> {
            return Utils.isEmpty(x) || empty.equals(x) || Utils.isEmpty(y) || empty.equals(y) ? "" : funcArg;
        };
        final Function<JsonValue, String> toString = x -> Utils.isEmpty(x) ? "" : x.toString();
        final BiFunction<JsonValue, JsonValue, String> concat = (x, y) -> toString.apply(x) + separator.apply(x, y)
                + toString.apply(y);
        return Utils.replace(to, valuePointer, Utils.stream(value).reduce(Json.createValue(""),
                (result, x) -> Json.createValue(concat.apply(result, x))));
    };

    CustomValueCopyFunction REMOVE = (ctx, from, to, valuePointer, funcArg) -> {
        return Utils.remove(to, valuePointer);
    };

    CustomValueCopyFunction COUNT = (ctx, from, to, valuePointer, funcArg) -> {
        final JsonValue value = Utils.getValue(from, valuePointer);
        return Utils.replace(to, valuePointer, Json.createValue(Utils.stream(value).count()));
    };

    CustomValueCopyFunction TOTAL = (ctx, from, to, valuePointer, funcArg) -> {
        final JsonValue value = Utils.getValue(from, valuePointer);
        if (Utils.isEmpty(value)) {
            return to;
        }
        final Function<JsonValue, Double> toDouble = x -> ValueType.NUMBER.equals(x.getValueType())
                ? JsonNumber.class.cast(x).doubleValue()
                : 0;
        return Utils.replace(to, valuePointer, Utils.stream(value).reduce(Json.createValue(0),
                (subtotal, x) -> Json.createValue(toDouble.apply(subtotal) + toDouble.apply(x))));
    };

    JsonValue copy(TransformationContext ctx, JsonValue from, JsonValue to, String valuePointer, String funcArg);

    // TODO:
    // filter on field value
    // select first
    // apply and log
    // make factory thread safe
}
