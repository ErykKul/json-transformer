package com.github.erykkul.json.transformer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

public class TransformerFactory {
    public static class TransformerVO {
        public List<TransformationVO> transformations;
    }

    public static class TransformationVO {
        public Boolean merge;
        public Boolean selfTranform;
        public String sourcePointer;
        public String targetPointer;
        public List<ValueVO> values;
    }

    public static class ValueVO {
        public String valuePointer;
        public String valueExpression;
    }

    private final Map<String, ValueFunction> functions = new HashMap<>();

    public TransformerFactory() {
        functions.put("filterUnique", ValueFunction.FILTER_UNIQUE);
        functions.put("generateUUID", ValueFunction.GENERATE_UUID);
        functions.put("concat", ValueFunction.CONCAT);
        functions.put("remove", ValueFunction.REMOVE);
        functions.put("count", ValueFunction.COUNT);
        functions.put("total", ValueFunction.TOTAL);
    }

    public TransformerFactory addValueFunction(final String name, final ValueFunction function) {
        functions.put(name, function);
        return this;
    }

    public Transformer createFromJsonString(final String json) {
        final Jsonb jsonb = JsonbBuilder.newBuilder().build();
        final TransformerVO t = jsonb.fromJson(json, TransformerVO.class);
        return new Transformer(t.transformations == null ? Collections.emptyList()
                : t.transformations.stream().map(this::toTransformation).collect(Collectors.toList()));
    }

    public Transformer createFromFile(final String file) throws IOException {
        final String content = Files.readString(Paths.get(file));
        return createFromJsonString(content);
    }

    public Transformation toTransformation(final TransformationVO t) {
        return new Transformation(t.merge == null ? false : t.merge, t.selfTranform == null ? false : t.selfTranform,
                t.sourcePointer == null ? "" : t.sourcePointer, t.targetPointer == null ? "" : t.targetPointer,
                t.values == null ? Collections.emptyList()
                        : t.values.stream().map(this::toValue).collect(Collectors.toList()));
    }

    public Value toValue(final ValueVO v) {
        return new Value(v.valuePointer == null ? "" : v.valuePointer,
                v.valueExpression == null ? "" : v.valueExpression, functions);
    }
}
