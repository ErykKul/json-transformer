// Author: Eryk Kulikowski (2024). Apache 2.0 License

package io.github.erykkul.json.transformer;

import javax.script.ScriptEngine;

/**
 * Holds ScriptEngine object during Transformer::transform method execution.
 * See documentation: <a href=
 * "https://github.com/ErykKul/json-transformer?tab=readme-ov-file#functions">Functions</a>
 * 
 * @author Eryk Kulikowski
 * @version 1.0.0
 * @since 1.0.0
 */
public class EngineHolder {
    private ScriptEngine engine;

    /**
     * Class constructor.
     */
    public EngineHolder() {
    }

    /**
     * ScriptEngine getter.
     * 
     * @return the script engine object
     */
    public ScriptEngine getEngine() {
        return engine;
    }

    /**
     * ScriptEngine setter
     * 
     * @param engine the script engine object
     */
    public void setEngine(final ScriptEngine engine) {
        this.engine = engine;
    }
}
