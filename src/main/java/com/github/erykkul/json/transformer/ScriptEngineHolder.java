package com.github.erykkul.json.transformer;

import javax.script.ScriptEngine;

public class ScriptEngineHolder {
    private ScriptEngine engine;

    public ScriptEngineHolder() {
    }

    public ScriptEngine getEngine() {
        return engine;
    }

    public void setEngine(final ScriptEngine engine) {
        this.engine = engine;
    }
}
