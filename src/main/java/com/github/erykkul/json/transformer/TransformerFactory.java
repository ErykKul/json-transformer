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
        public Boolean append;
        public Boolean selfTranform;
        public String sourcePointer;
        public String targetPointer;
        public List<ValueVO> steps;
    }

    public static class ValueVO {
        public String valuePointer;
        public String valueExpression;
    }

    public static TransformerFactory factory() {
        return new TransformerFactory();
    }

    public static TransformerFactory factory(final Map<String, TransformationStepFunction> functions) {
        return new TransformerFactory(functions);
    }

    private final Map<String, TransformationStepFunction> functions;

    private TransformerFactory(final Map<String, TransformationStepFunction> functions) {
        final Map<String, TransformationStepFunction> result = builtin();
        result.putAll(functions);
        this.functions = Collections.unmodifiableMap(result);
    }

    private TransformerFactory() {
        this(Collections.emptyMap());
    }

    public Transformer createFromJsonString(final String json) {
        final Jsonb jsonb = JsonbBuilder.newBuilder().build();
        final TransformerVO t = jsonb.fromJson(json, TransformerVO.class);
        return new Transformer(t.transformations == null ? Collections.emptyList()
                : t.transformations.stream().map(this::asTransformation).collect(Collectors.toList()));
    }

    public Transformer createFromFile(final String file) throws IOException {
        final String content = Files.readString(Paths.get(file));
        return createFromJsonString(content);
    }

    public Transformation asTransformation(final TransformationVO t) {
        return new Transformation(t.append == null ? false : t.append, t.selfTranform == null ? false : t.selfTranform,
                t.sourcePointer == null ? "" : t.sourcePointer, t.targetPointer == null ? "" : t.targetPointer,
                t.steps == null ? Collections.emptyList()
                        : t.steps.stream().map(this::asValue).collect(Collectors.toList()),
                functions);
    }

    public TransformationStep asValue(final ValueVO v) {
        return new TransformationStep(v.valuePointer == null ? "" : v.valuePointer,
                v.valueExpression == null ? "" : v.valueExpression);
    }

    public Map<String, TransformationStepFunction> getFunctions() {
        return functions;
    }

    private Map<String, TransformationStepFunction> builtin() {
        final Map<String, TransformationStepFunction> result = new HashMap<>();
        result.put("generateUUID", TransformationStepFunction.GENERATE_UUID);
        result.put("remove", TransformationStepFunction.REMOVE);
        result.put("filter", TransformationStepFunction.FILTER);
        result.put("map", TransformationStepFunction.MAP);
        result.put("reduce", TransformationStepFunction.REDUCE);
        return result;
    }
}
