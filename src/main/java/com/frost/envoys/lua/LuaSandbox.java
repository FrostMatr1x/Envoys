package com.frost.envoys.lua;

import com.frost.envoys.Envoys;

import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.function.LuaClosure;
import org.squiddev.cobalt.interrupt.InterruptAction;
import org.squiddev.cobalt.interrupt.InterruptHandler;
import org.squiddev.cobalt.lib.CoreLibraries;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public final class LuaSandbox {

    private static final String[] REMOVED_GLOBALS = {"load", "loadstring", "dofile", "loadfile", "require", "debug"};

    private LuaSandbox() {
    }

    public static LuaState createState(InstructionBudget budget) {
        LuaState state = LuaState.builder()
                .interruptHandler(budget)
                .errorReporter((error, message) -> Envoys.LOGGER.error("[Envoys] Lua internal error: {}", message.get(), error))
                .build();
        budget.attach(state);
        return state;
    }

    public static void installStandardGlobals(LuaState state) throws LuaError {
        CoreLibraries.standardGlobals(state);

        LuaTable globals = state.globals();
        for (String name : REMOVED_GLOBALS) {
            globals.rawset(name, Constants.NIL);
        }
    }

    public static LuaClosure compile(LuaState state, String source) throws CompileException, LuaError {
        byte[] bytes = (source == null ? "" : source).getBytes(StandardCharsets.UTF_8);
        return LoadState.load(state, new ByteArrayInputStream(bytes), "main.lua", state.globals());
    }

    public static final class InstructionBudget implements InterruptHandler {
        private LuaState state;
        private int remaining;
        private boolean exhausted;

        void attach(LuaState state) {
            this.state = state;
        }

        public void arm(int limit) {
            remaining = Math.max(1, limit);
            exhausted = false;
            state.interrupt();
        }

        public boolean wasExhausted() {
            return exhausted;
        }

        @Override
        public InterruptAction interrupted() throws LuaError {
            if (--remaining <= 0) {
                exhausted = true;
                return InterruptAction.SUSPEND;
            }
            state.interrupt();
            return InterruptAction.CONTINUE;
        }
    }
}
