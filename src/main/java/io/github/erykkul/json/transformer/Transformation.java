// Author: Eryk Kulikowski (2024). Apache 2.0 License

package io.github.erykkul.json.transformer;

import static jakarta.json.JsonValue.EMPTY_JSON_ARRAY;
import static jakarta.json.JsonValue.EMPTY_JSON_OBJECT;
import static jakarta.json.JsonValue.NULL;
import static jakarta.json.JsonValue.ValueType.ARRAY;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

/**
 * The transformation as used by the transformer. See documentation: <a href=
 * "https://github.com/ErykKul/json-transformer?tab=readme-ov-file#transformer">Transformer</a>
 * 
 * @author Eryk Kulikowski
 * @version 1.0.3
 * @since 1.0.0
 */
public class Transformation {
    private static final Logger logger = Logger.getLogger(Utils.class.getName());

    /**
     * Executes the expressions. You can call this, e.g., when wrapping a function.
     * See, for example, the "withLogger" function in the documentation: <a href=
     * "https://github.com/ErykKul/json-transformer?tab=readme-ov-file#functions">Functions</a>
     * 
     * @param ctx         the context of this transformation
     * @param source      the JsonValue resolved from the source document at
     *                    "sourcePointer"
     * @param result      the JsonValue resolved from the resulting document at
     *                    "resultPointer"
     * @param expressions the expressions to be executed
     * @return the resulting JsonValue
     */
    public static JsonValue executeExpressions(final TransformationCtx ctx, final JsonValue source,
            final JsonValue result, final List<String> expressions) {
        if (expressions != null && !expressions.isEmpty()) {
            JsonValue res = result;
            for (final String expression : expressions) {
                res = executeExpression(ctx, ctx.useResultAsSource() ? res : source, res, expression);
            }
            return res;
        }
        return source;
    }

    private static JsonValue executeExpression(final TransformationCtx ctx, final JsonValue source,
            final JsonValue result, final String expression) {
        if (expression.startsWith("\"")) {
            final String literal = expression.length() > 1 ? expression.substring(1, expression.length() - 1) : "";
            return Json.createValue(literal);
        } else if (!expression.isEmpty()) {
            final String[] functionParts = expression.split("\\(");
            final String functionName = functionParts.length > 0 ? functionParts[0] : "";
            final String str = functionParts.length > 1
                    ? String.join("(", Arrays.copyOfRange(functionParts, 1, functionParts.length))
                    : "";
            final String functionArg = !str.isEmpty() ? str.substring(0, str.length() - 1) : "";
            final ExprFunction func = ctx.getFunctions().get(functionName);
            if (func == null) {
                logger.severe("function \"" + functionName + "\" not found");
                return result;
            }
            return func.execute(ctx, source, result, functionArg);
        }
        return result;
    }

    private final boolean append;
    private final boolean useResultAsSource;
    private final String sourcePointer;
    private final String resultPointer;
    private final List<String> expressions;
    private final Map<String, ExprFunction> functions;

    /**
     * Class constructor. See documentation: <a href=
     * "https://github.com/ErykKul/json-transformer?tab=readme-ov-file#transformer">Transformer</a>
     * 
     * @param append            determines if the produced values are appended to
     *                          the array at the "resultPointer" or are merged with
     *                          already existing values
     * @param useResultAsSource when set to true the result is also used as the
     *                          source of this transformation, where the source
     *                          itself is ignored
     * @param sourcePointer     a JSON Pointer extended with [i] notation pointing
     *                          to a value in the source document.
     * @param resultPointer     a JSON Pointer extended with [i] notation pointing
     *                          to a value in the resulting document.
     * @param expressions       the expressions to be executed
     * @param functions         functions registered in the transformer factory
     */
    public Transformation(final boolean append, final boolean useResultAsSource, final String sourcePointer,
            final String resultPointer, final List<String> expressions,
            final Map<String, ExprFunction> functions) {
        this.append = append;
        this.useResultAsSource = useResultAsSource;
        this.sourcePointer = sourcePointer;
        this.resultPointer = resultPointer;
        this.expressions = expressions;
        this.functions = functions;
    }

    /**
     * The transform function as called by the transformer during the
     * transformations.
     * 
     * @param source       the source document for the transformation
     * @param result       the resulting document as produced by the preceding
     *                     transformations
     * @param engineHolder the engine holder object that holds the script engine
     *                     during the execution of the Transformer::transform method
     * @return the resulting JSON document
     */
    public JsonObject transform(final JsonObject source, final JsonObject result, final EngineHolder engineHolder) {
        final JsonObject srcOrRes = useResultAsSource ? result : source;
        final TransformationCtx ctx = new TransformationCtx(srcOrRes, result, srcOrRes, result, this, engineHolder);
        if (!sourcePointer.contains("[i]")) {
            return doTransform(ctx, sourcePointer, resultPointer).asJsonObject();
        }
        final List<String> sourcePointers = Arrays.asList(sourcePointer.split("\\[i\\]", -1));
        final List<String> resultPointers = Arrays.asList(resultPointer.split("\\[i\\]", -1));
        return transform(ctx, sourcePointers, resultPointers, false, engineHolder).asJsonObject();
    }

    /**
     * Creates a JSON representation of the Transformation object.
     * 
     * @return the JSON representation of the Transformation object
     */
    public JsonObject toJsonObject() {
        return Json.createObjectBuilder().add("append", append).add("useResultAsSource", useResultAsSource)
                .add("sourcePointer", sourcePointer).add("resultPointer", resultPointer)
                .add("expressions", Json.createArrayBuilder(expressions)).build();
    }

    /**
     * Returns the expressions functions as registered in the TransformerFactory and
     * passed to this Transformation in the constructor.
     * 
     * @return the functions registered in the TransformerFactory
     */
    public Map<String, ExprFunction> getFunctions() {
        return functions;
    }

    /**
     * Getter for the "useResultAsSource" Transformation field.
     * 
     * @return determines if the produced values are appended to
     *         the array at the "resultPointer" or are merged with
     *         already existing values
     */
    public boolean useResultAsSource() {
        return useResultAsSource;
    }

    private JsonValue transform(final TransformationCtx ctx, final List<String> sourcePointers,
            final List<String> resultPointers, final boolean flatten, final EngineHolder engineHolder) {
        if (sourcePointers.size() == 1) {
            return doTransform(ctx, sourcePointers.get(0), String.join("[i]", resultPointers));
        }
        final JsonValue sourceValue = Utils.getValue(ctx.getLocalSource(), sourcePointers.get(0));
        if (NULL.equals(sourceValue)) {
            return ctx.getLocalResult();
        }
        final String rootOrResultPointer = resultPointers.isEmpty() ? "" : resultPointers.get(0);
        final JsonValue fixedResult = Utils.fixPath(ctx.getLocalResult(), ARRAY, rootOrResultPointer);
        final JsonArray sourceArray;
        if (!Utils.isArray(sourceValue)) {
            sourceArray = Json.createArrayBuilder().add(sourceValue).build();
        } else {
            sourceArray = sourceValue.asJsonArray();
        }

        final List<String> remainingSourcePointers = sourcePointers.subList(1, sourcePointers.size());
        final List<String> remainingResultPointers = resultPointers.isEmpty() ? Collections.emptyList()
                : resultPointers.subList(1, resultPointers.size());
        final boolean doFlatten = flatten || resultPointers.size() == 1;
        JsonValue result = Utils.getValue(fixedResult, rootOrResultPointer);
        int flattenedMergeIdx = 0;
        for (int i = 0; i < sourceArray.size(); i++) {
            result = Utils.isArray(result) ? result : EMPTY_JSON_ARRAY;
            final JsonArray resultArray = result.asJsonArray();
            final JsonValue resultObject = (!append && resultArray.size() > i) ? resultArray.get(i) : EMPTY_JSON_OBJECT;
            final TransformationCtx localContext = new TransformationCtx(ctx.getGlobalSource(), ctx.getGlobalResult(),
                    sourceArray.get(i), resultObject, this, engineHolder);
            final JsonValue transformed = transform(localContext, remainingSourcePointers, remainingResultPointers,
                    doFlatten, engineHolder);
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
        return Utils.replace(fixedResult, rootOrResultPointer, result);
    }

    private JsonValue doTransform(final TransformationCtx ctx, final String sourcePointer, final String resultPointer) {
        final JsonValue sourceValue = Utils.getValue(ctx.getLocalSource(), sourcePointer);
        if (NULL.equals(sourceValue)) {
            return ctx.getLocalResult();
        }
        if (append) {
            final JsonValue fixedResult = Utils.fixPath(ctx.getLocalResult(), ARRAY, resultPointer);
            final JsonValue result = executeExpressions(ctx, sourceValue, EMPTY_JSON_OBJECT, expressions);
            final JsonValue resultArray = Utils.getValue(fixedResult, resultPointer);
            if (!Utils.isArray(resultArray)) {
                return result;
            }
            return Utils.replace(fixedResult, resultPointer,
                    Json.createArrayBuilder(resultArray.asJsonArray()).add(result).build());
        } else {
            final JsonValue fixedResult = Utils.fixPath(ctx.getLocalResult(), sourceValue.getValueType(),
                    resultPointer);
            final JsonValue result = executeExpressions(ctx, sourceValue, Utils.getValue(fixedResult, resultPointer),
                    expressions);
            if (Utils.isEmpty(fixedResult)) {
                return result;
            }
            return Utils.replace(fixedResult, resultPointer, result);
        }
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
            return Json.createObjectBuilder(result.asJsonObject())
                    .addAll(Json.createObjectBuilder(source.asJsonObject())).build();
        }
        return result;
    }
}
