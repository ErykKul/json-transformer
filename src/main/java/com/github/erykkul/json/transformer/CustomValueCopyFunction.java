package com.github.erykkul.json.transformer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonValue;

@FunctionalInterface
public interface CustomValueCopyFunction {
    CustomValueCopyFunction FILTER_UNIQUE = (ctx, from, to, valuePointer, funcArg) -> {
        final JsonValue toFilter = Utils.getValue(to, valuePointer);
        if (!Utils.isArray(toFilter) || Utils.isEmpty(toFilter)) {
            return to;
        }
        final Set<JsonValue> items = new HashSet<>();
        final List<JsonValue> filtered = toFilter.asJsonArray().stream()
                .filter(x -> items.add(Utils.getValue(x, funcArg)))
                .collect(Collectors.toList());
        final JsonArray result = Json.createArrayBuilder(filtered).build();
        return Utils.replace(to, valuePointer, result);
    };

    JsonValue copy(TransformationContext ctx, JsonValue from, JsonValue to, String valuePointer, String funcArg);

    // TODO:
    // generate uuid
    // concat values
    // filter on field value
    // select first
    // delete
}
