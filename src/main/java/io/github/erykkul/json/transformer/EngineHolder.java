// Author: Eryk Kulikowski (2024). Apache 2.0 License

package io.github.erykkul.json.transformer;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

/**
 * Holds ScriptEngine object during Transformer::transform method execution.
 * See documentation: <a href=
 * "https://github.com/ErykKul/json-transformer?tab=readme-ov-file#functions">Functions</a>
 * 
 * @author Eryk Kulikowski
 * @version 1.0.2
 * @since 1.0.0
 */
public class EngineHolder {
    private ScriptEngine engine;
    private final ScriptEngineFactory scriptEngineFactory;

    /**
     * Class constructor.
     * 
     * @param scriptEngineFactory the script engine factory
     */
    public EngineHolder(final ScriptEngineFactory scriptEngineFactory) {
        this.scriptEngineFactory = scriptEngineFactory;
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

    /**
     * ScriptEngineFactory getter
     * 
     * @return the script engine factory
     */
    public ScriptEngineFactory getScriptEngineFactory() {
        return scriptEngineFactory;
    }
}
