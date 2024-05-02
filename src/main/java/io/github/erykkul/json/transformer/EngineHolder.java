// Author: Eryk Kulikowski (2024). Apache 2.0 License

package io.github.erykkul.json.transformer;

import javax.script.ScriptEngine;

public class EngineHolder {
    private ScriptEngine engine;

    public EngineHolder() {
    }

    public ScriptEngine getEngine() {
        return engine;
    }

    public void setEngine(final ScriptEngine engine) {
        this.engine = engine;
    }
}
