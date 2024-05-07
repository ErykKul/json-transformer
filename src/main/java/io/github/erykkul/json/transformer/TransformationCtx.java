// Author: Eryk Kulikowski (2024). Apache 2.0 License

package io.github.erykkul.json.transformer;

import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

/**
 * The transformation context object as exposed in the functions interface. See
 * documentation: <a href=
 * "https://github.com/ErykKul/json-transformer?tab=readme-ov-file#transformer">Transformer</a>
 * 
 * @author Eryk Kulikowski
 * @version 1.0.0
 * @since 1.0.0
 */
public class TransformationCtx {
    private final Transformation transformation;
    private final JsonObject globalSource;
    private final JsonObject globalResult;
    private final JsonValue localSource;
    private final JsonValue localResult;
    private final EngineHolder engineHolder;

    /**
     * The class constructor.
     * 
     * @param globalSource   the global source document of the transformation
     * @param globalResult   the global resulting document of the transformation
     * @param localSource    the local source document, e.g., after an [i] notation
     * @param localResult    the local resulting document, e.g., after an [i]
     *                       notation
     * @param transformation the transformation to which this context belongs
     * @param engineHolder   the script engine holder
     */
    public TransformationCtx(final JsonObject globalSource, final JsonObject globalResult,
            final JsonValue localSource, final JsonValue localResult, final Transformation transformation,
            final EngineHolder engineHolder) {
        this.globalSource = globalSource;
        this.globalResult = globalResult;
        this.localSource = localSource;
        this.localResult = localResult;
        this.transformation = transformation;
        this.engineHolder = engineHolder;
    }

    /**
     * Creates a JSON representation of the TransformationCtx object.
     * 
     * @return the JSON representation of the TransformationCtx object
     */
    public JsonObject toJsonObject() {
        return Json.createObjectBuilder().add("transformation", transformation.toJsonObject())
                .add("globalSource", globalSource).add("globalResult", globalResult).add("localSource", localSource)
                .add("localResult", localResult).build();
    }

    /**
     * Global source getter.
     * 
     * @return the source document
     */
    public JsonObject getGlobalSource() {
        return globalSource;
    }

    /**
     * Global result getter.
     * 
     * @return the resulting document
     */
    public JsonObject getGlobalResult() {
        return globalResult;
    }

    /**
     * Local source getter.
     * 
     * @return the relative source document.
     */
    public JsonValue getLocalSource() {
        return localSource;
    }

    /**
     * Local result getter.
     * 
     * @return the relative resulting document.
     */
    public JsonValue getLocalResult() {
        return localResult;
    }

    /**
     * Functions getter.
     * 
     * @return the functions registered in the transformer factory
     */
    public Map<String, ExprFunction> getFunctions() {
        return transformation.getFunctions();
    }

    /**
     * Use result as source getter.
     * 
     * @return "useResultAsSource" field of the transformation.
     */
    public boolean useResultAsSource() {
        return transformation.useResultAsSource();
    }

    /**
     * Script engine holder getter.
     * 
     * @return the script engine (holder)
     */
    public EngineHolder engine() {
        return engineHolder;
    }
}
