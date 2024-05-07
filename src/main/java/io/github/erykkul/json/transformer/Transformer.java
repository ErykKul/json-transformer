// Author: Eryk Kulikowski (2024). Apache 2.0 License

package io.github.erykkul.json.transformer;

import java.util.List;

import jakarta.json.JsonObject;

/**
 * The transformer. See documentation: <a href=
 * "https://github.com/ErykKul/json-transformer?tab=readme-ov-file#transformer">Transformer</a>
 * 
 * @author Eryk Kulikowski
 * @version 1.0.0
 * @since 1.0.0
 */
public class Transformer {
    private final List<Transformation> transformations;

    /**
     * Class constructor.
     * 
     * @param transformations the list of the transformations of this transformer
     */
    public Transformer(final List<Transformation> transformations) {
        this.transformations = transformations;
    }

    /**
     * The transform method.
     * 
     * @param source the source JSON document
     * @return the transformed JSON document
     */
    public JsonObject transform(final JsonObject source) {
        final EngineHolder engineHolder = new EngineHolder();
        JsonObject result = JsonObject.EMPTY_JSON_OBJECT;
        for (final Transformation t : transformations) {
            result = t.transform(source, result, engineHolder);
        }
        return result;
    }
}
