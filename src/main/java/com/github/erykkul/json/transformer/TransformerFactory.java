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
        public Boolean useResultAsSource;
        public String sourcePointer;
        public String resultPointer;
        public List<ValueVO> steps;
    }

    public static class ValueVO {
        public String resultPointer;
        public String sourcePointer;
        public List<String> expressions;
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
                : t.transformations.stream().map(this::toTransformation).collect(Collectors.toList()));
    }

    public Transformer createFromFile(final String file) throws IOException {
        final String content = Files.readString(Paths.get(file));
        return createFromJsonString(content);
    }

    public Transformation toTransformation(final TransformationVO t) {
        return new Transformation(t.append == null ? false : t.append,
                t.useResultAsSource == null ? false : t.useResultAsSource,
                t.sourcePointer == null ? "" : t.sourcePointer, t.resultPointer == null ? "" : t.resultPointer,
                t.steps == null ? Collections.emptyList()
                        : t.steps.stream().map(this::toValue).collect(Collectors.toList()),
                functions);
    }

    public TransformationStep toValue(final ValueVO v) {
        return new TransformationStep(v.sourcePointer == null ? "" : v.sourcePointer,
                v.resultPointer == null ? "" : v.resultPointer, v.expressions);
    }

    public Map<String, TransformationStepFunction> getFunctions() {
        return functions;
    }

    private Map<String, TransformationStepFunction> builtin() {
        final Map<String, TransformationStepFunction> result = new HashMap<>();
        result.put("generateUuid", TransformationStepFunction.GENERATE_UUID);
        result.put("remove", TransformationStepFunction.REMOVE);
        result.put("script", TransformationStepFunction.SCRIPT);
        result.put("filter", TransformationStepFunction.FILTER);
        result.put("map", TransformationStepFunction.MAP);
        result.put("reduce", TransformationStepFunction.REDUCE);
        return result;
    }
}
