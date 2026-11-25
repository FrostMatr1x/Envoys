package com.frost.envoys.lua;

import com.frost.envoys.TestSupport;
import com.frost.envoys.platform.EnvoysPlatform;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LuaNpcEngineTest {

    @TempDir
    Path tempDir;

    private final List<LuaNpcEngine> engines = new ArrayList<>();

    @BeforeEach
    void setUp() {
        TestSupport.installEnvironment(tempDir);
        EnvoysPlatform.config().luaEnabled = true;
        EnvoysPlatform.config().luaInstructionLimit = 50000;
    }

    @AfterEach
    void tearDown() {
        for (LuaNpcEngine engine : engines) {
            engine.shutdown();
        }
        engines.clear();
        EnvoysPlatform.config().luaEnabled = true;
        EnvoysPlatform.config().luaInstructionLimit = 50000;
        TestSupport.clearEnvironment();
    }

    @Test
    void loadValidScriptSucceedsAndIsNotShutdown() {
        LuaNpcEngine engine = engine(1);

        assertTrue(engine.load("return 1"));
        assertFalse(engine.isShutdown());
        assertFalse(engine.isErrored());
    }

    @Test
    void loadSyntaxErrorReturnsFalseWithoutThrowing() {
        LuaNpcEngine engine = engine(2);

        assertFalse(assertDoesNotThrow(() -> engine.load("return (")));
        assertTrue(engine.isShutdown());
    }

    @Test
    void runtimeErrorMarksEngineErrored() {
        LuaNpcEngine engine = engine(3);
        assertTrue(engine.load("error('boom')"));

        engine.tick(null);

        assertTrue(engine.isErrored());
        assertTrue(engine.errorText().contains("boom"));
    }

    @Test
    void removedGlobalsAreNilAndApiIsInstalled() {
        LuaNpcEngine engine = engine(4);
        String script = """
                assert(load == nil)
                assert(loadstring == nil)
                assert(dofile == nil)
                assert(loadfile == nil)
                assert(require == nil)
                assert(debug == nil)
                assert(type(envoys) == "table")
                """;
        assertTrue(engine.load(script));

        engine.tick(null);

        assertFalse(engine.isErrored(), engine.errorText());
    }

    @Test
    void instructionLimitStopsInfiniteLoop() {
        EnvoysPlatform.config().luaInstructionLimit = 50;
        LuaNpcEngine engine = engine(5);
        assertTrue(engine.load("while true do end"));

        engine.tick(null);

        assertTrue(engine.isErrored());
        assertTrue(engine.errorText().contains("too long without yielding"));
    }

    @Test
    void waitActionCompletesHappyPath() {
        LuaNpcEngine engine = engine(6);
        assertTrue(engine.load("envoys.wait(0)"));

        assertDoesNotThrow(() -> engine.tick(null));
        assertFalse(engine.isErrored(), engine.errorText());
        assertDoesNotThrow(() -> engine.tick(null));
        assertFalse(engine.isErrored(), engine.errorText());
    }

    @Test
    void errorInOneNpcDoesNotAffectAnother() {
        LuaNpcEngine failing = engine(7);
        LuaNpcEngine healthy = engine(8);
        assertTrue(failing.load("error('boom')"));
        assertTrue(healthy.load("return 1"));

        failing.tick(null);
        healthy.tick(null);

        assertTrue(failing.isErrored());
        assertFalse(healthy.isErrored());
        assertFalse(healthy.isShutdown());
    }

    @Test
    void shutdownIsIdempotent() {
        LuaNpcEngine engine = engine(9);
        assertTrue(engine.load("return 1"));

        engine.shutdown();
        assertDoesNotThrow(engine::shutdown);

        assertTrue(engine.isShutdown());
        assertDoesNotThrow(() -> engine.tick(null));
        assertTrue(engine.isShutdown());
    }

    private LuaNpcEngine engine(int value) {
        LuaNpcEngine engine = new LuaNpcEngine(TestSupport.uuid(value));
        engines.add(engine);
        return engine;
    }
}
