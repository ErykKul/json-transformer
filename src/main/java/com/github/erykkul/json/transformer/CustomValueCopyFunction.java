package com.github.erykkul.json.transformer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

@FunctionalInterface
public interface CustomValueCopyFunction {
    JsonValue copy(TransformationContext ctx, JsonObject from, JsonObject to, String valuePointer, String funcArg);

    CustomValueCopyFunction FILTER_UNIQUE = (ctx, from, to, valuePointer, funcArg) -> {
        final JsonArray toFilter = to.getValue(valuePointer).asJsonArray();
        final Set<JsonValue> items = new HashSet<>();
        final List<JsonValue> filtered = toFilter.stream()
                .filter(x -> items.add(x.asJsonObject().getValue(funcArg)))
                .collect(Collectors.toList());
        final JsonArray result = Json.createArrayBuilder(filtered).build();
        return Json.createPointer(valuePointer).replace(to, result);
    };

    // TODO:
    //generate uuid
    //concat values
    //filter on field value
    //select first
    //delete
}
