package com.frost.envoys.lua;

import org.junit.jupiter.api.Test;
import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.ValueFactory;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.interrupt.InterruptAction;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LuaSandboxTest {

    @Test
    void compileOnlyAcceptsValidLua() {
        assertDoesNotThrow(() -> LuaSandbox.compileOnly("return 1"));
    }

    @Test
    void compileOnlyRejectsSyntaxError() {
        assertThrows(CompileException.class, () -> LuaSandbox.compileOnly("return ("));
    }

    @Test
    void standardGlobalsRemoveDangerousFunctions() throws Exception {
        LuaSandbox.InstructionBudget budget = new LuaSandbox.InstructionBudget();
        LuaState state = LuaSandbox.createState(budget);
        LuaSandbox.installStandardGlobals(state);

        for (String name : new String[]{"load", "loadstring", "dofile", "loadfile", "require", "debug"}) {
            assertEquals(Constants.NIL, state.globals().rawget(ValueFactory.valueOf(name)), name);
        }
    }

    @Test
    void instructionBudgetSuspendsWhenExhausted() throws Exception {
        LuaSandbox.InstructionBudget budget = new LuaSandbox.InstructionBudget();
        LuaSandbox.createState(budget);

        budget.arm(1);
        assertEquals(InterruptAction.SUSPEND, budget.interrupted());
        assertTrue(budget.wasExhausted());

        budget.arm(2);
        assertEquals(InterruptAction.CONTINUE, budget.interrupted());
        assertEquals(InterruptAction.SUSPEND, budget.interrupted());
        assertTrue(budget.wasExhausted());
    }

    @Test
    void utf8SourceCompiles() {
        assertDoesNotThrow(() -> LuaSandbox.compileOnly("local s = \"Привет\" return s"));
    }
}
