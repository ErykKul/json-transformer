// Author: Eryk Kulikowski (2024). Apache 2.0 License

package io.github.erykkul.json.transformer;

import java.util.List;

import javax.script.ScriptEngineFactory;

import jakarta.json.JsonObject;

/**
 * The transformer. See documentation: <a href=
 * "https://github.com/ErykKul/json-transformer?tab=readme-ov-file#transformer">Transformer</a>
 * 
 * @author Eryk Kulikowski
 * @version 1.0.2
 * @since 1.0.0
 */
public class Transformer {
    private final List<Transformation> transformations;
    private final ScriptEngineFactory scriptEngineFactory;

    /**
     * Class constructor.
     * 
     * @param transformations     the list of the transformations of this
     *                            transformer
     * @param scriptEngineFactory the script engine factory
     */
    public Transformer(final List<Transformation> transformations, final ScriptEngineFactory scriptEngineFactory) {
        this.transformations = transformations;
        this.scriptEngineFactory = scriptEngineFactory;
    }

    /**
     * The transform method.
     * 
     * @param source the source JSON document
     * @return the transformed JSON document
     */
    public JsonObject transform(final JsonObject source) {
        final EngineHolder engineHolder = new EngineHolder(scriptEngineFactory);
        JsonObject result = JsonObject.EMPTY_JSON_OBJECT;
        for (final Transformation t : transformations) {
            result = t.transform(source, result, engineHolder);
        }
        return result;
    }
}
