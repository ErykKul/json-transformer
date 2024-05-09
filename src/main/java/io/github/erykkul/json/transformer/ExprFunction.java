// Author: Eryk Kulikowski (2024). Apache 2.0 License

package io.github.erykkul.json.transformer;

import static jakarta.json.JsonValue.NULL;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.json.Json;
import jakarta.json.JsonValue;
import jakarta.json.JsonValue.ValueType;

/**
 * Interface for expression functions and the definitions for the built-in
 * functions. See documentation: <a href=
 * "https://github.com/ErykKul/json-transformer?tab=readme-ov-file#functions">Functions</a>
 * 
 * @author Eryk Kulikowski
 * @version 1.0.1
 * @since 1.0.0
 */
@FunctionalInterface
public interface ExprFunction {

    /**
     * copy(/fromPointer, /toPointer): copies a value from the /fromPointer
     * (relative to the sourcePointer) in the source document to the /toPointer
     * (relative to the resultPointer) in the resulting document. Notice that it is
     * very similar to the default copy functionality when the expressions of the
     * transformation are left empty. In fact, the default functionality is
     * identical to copy(, ) (or simply copy(), where both; the /fromPointer and the
     * /toPointer are empty string pointers, and the value is copied from the
     * /sourcePointer to the resultPointer).
     */
    ExprFunction COPY = (ctx, source, result, expression) -> {
        final String[] args = expression.split(",");
        final String from = args.length > 0 ? args[0].trim() : "";
        final String to = args.length > 1 ? args[1].trim() : "";
        final JsonValue sourceValue = Utils.getValue(source, from);
        if (NULL.equals(sourceValue)) {
            return result;
        }
        return Utils.replace(Utils.fixPath(result, ValueType.OBJECT, to), to, sourceValue);
    };

    /**
     * move(/fromPointer, /toPointer): moves a value from the /fromPointer (relative
     * to the sourcePointer) in the resulting document to the /toPointer (relative
     * to the resultPointer) in the resulting document (the source document is
     * ignored by this function).
     */
    ExprFunction MOVE = (ctx, source, result, expression) -> {
        final String[] args = expression.split(",");
        final String from = args.length > 0 ? args[0].trim() : "";
        final String to = args.length > 1 ? args[1].trim() : "";
        final JsonValue resultValue = Utils.getValue(result, from);
        if (NULL.equals(resultValue)) {
            return result;
        }
        final JsonValue res = Utils.replace(Utils.fixPath(result, ValueType.OBJECT, to), to, resultValue);
        return Utils.remove(res, from);
    };

    /**
     * remove(/atPointer): removes a value from the /atPointer (relative to the
     * resultPointer) in the resulting document. The \atPointer cannot be an empty
     * string pointer, as remove operations are not permitted on the root.
     */
    ExprFunction REMOVE = (ctx, source, result, expression) -> Utils.remove(result, expression);

    /**
     * generateUuid(/atPointer): generates a UUID at the /atPointer (relative to the
     * resultPointer) in the resulting document.
     */
    ExprFunction GENERATE_UUID = (ctx, source, result, expression) -> Utils.replace(
            Utils.fixPath(result, ValueType.OBJECT, expression), expression,
            Json.createValue(UUID.randomUUID().toString()));

    /**
     * script(res = myFunction(x)): executes the JavaScript script sent as an
     * argument to this function. If the script writes a value to the res variable,
     * that value is written at the resultPointer in the resulting document.
     */
    ExprFunction SCRIPT = (ctx, source, result, expression) -> {
        Utils.eval(ctx.engine(), "res = null");
        Utils.eval(ctx.engine(), expression, source, "x");
        Object resultObject = Utils.getObject(ctx.engine(), "res");
        if (resultObject == null) {
            return result;
        }
        return Utils.asJsonValue(resultObject);
    };

    /**
     * filter(res = x > 2): filters out values from an array (or fields in an
     * object) at the sourcePointer in the source document that do not produce res =
     * true in the JavaScript script provided as argument to this function. The
     * values or fields being filtered are passed as x variables to the script
     * engine by the library. The result of the expression is written at the
     * resultPointer in the resulting document.
     */
    ExprFunction FILTER = (ctx, source, result, expression) -> {
        if (Utils.isEmpty(source)) {
            return result;
        }
        Utils.eval(ctx.engine(), "res = null");
        final List<JsonValue> res = Utils.stream(source).filter(x -> {
            Utils.eval(ctx.engine(), expression, x, "x");
            return Boolean.TRUE.equals(Utils.getObject(ctx.engine(), "res"));
        }).collect(Collectors.toList());
        return Json.createArrayBuilder(res).build();
    };

    /**
     * map(res = { a: x.field1, b: x.field2 }): maps values from an array (or fields
     * in an object) at the sourcePointer in the source document to the values
     * written in the res variable by the JavaScript script provided as argument to
     * this function. The values or fields being mapped are passed as x variables to
     * the script engine by the library. The result of the expression is written at
     * the resultPointer in the resulting document.
     */
    ExprFunction MAP = (ctx, source, result, expression) -> {
        if (Utils.isEmpty(source)) {
            return result;
        }
        Utils.eval(ctx.engine(), "res = null");
        final List<JsonValue> res = Utils.stream(source).map(x -> {
            Utils.eval(ctx.engine(), expression, x, "x");
            return Utils.asJsonValue(Utils.getObject(ctx.engine(), "res"));
        }).collect(Collectors.toList());
        return Json.createArrayBuilder(res).build();
    };

    /**
     * reduce(res = res + x): reduces values from an array (or fields in an object)
     * at the sourcePointer in the source document to the values written in the res
     * variable by the JavaScript script provided as argument to this function. The
     * values or fields being reduced are passed as x variables to the script engine
     * by the library. The result of the expression is written at the resultPointer
     * in the resulting document.
     */
    ExprFunction REDUCE = (ctx, source, result, expression) -> {
        if (Utils.isEmpty(source)) {
            return result;
        }
        Utils.eval(ctx.engine(), "res = null");
        Utils.stream(source).forEach(x -> Utils.eval(ctx.engine(), expression, x, "x"));
        return Utils.asJsonValue(Utils.getObject(ctx.engine(), "res"));
    };

    /**
     * The only method of this interface, see documentation: <a href=
     * "https://github.com/ErykKul/json-transformer?tab=readme-ov-file#functions">Functions</a>
     * 
     * @param ctx        the transformation context object
     * @param source     the JsonValue resolved from the source document at
     *                   "sourcePointer"
     * @param result     the JsonValue resolved from the resulting document at
     *                   "resultPointer"
     * @param expression the string value between the round brackets (()) that is
     *                   passed to this function in the expression of the
     *                   transformation being executed
     * @return the expression execution result
     */
    JsonValue execute(TransformationCtx ctx, JsonValue source, JsonValue result, String expression);
}
