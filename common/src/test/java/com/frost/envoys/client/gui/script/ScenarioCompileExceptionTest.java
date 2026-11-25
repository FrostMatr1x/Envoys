package com.frost.envoys.client.gui.script;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ScenarioCompileExceptionTest {

    @Test
    void messageIsLocalizationKey() {
        ScenarioCompileException error = new ScenarioCompileException("envoys.compile.test", 1, "x");
        assertEquals("envoys.compile.test", error.getMessage());
        assertNotNull(error.component());
    }
}
