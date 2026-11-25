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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvoysLuaApiSmokeTest {

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
        TestSupport.clearEnvironment();
    }

    @Test
    void installsAllPublicEnvoysFunctionsAndNamespaces() {
        LuaNpcEngine engine = engine(1);
        String script = """
                assert(type(envoys) == "table")
                for _, n in ipairs({"say", "command", "on", "pos", "name", "playersInRange",
                        "sound", "particle", "lookAt", "teleport", "effect", "wait", "move",
                        "waitEvent", "dialogue", "trade"}) do
                    assert(type(envoys[n]) == "function", n)
                end
                assert(type(envoys.quest) == "table")
                for _, n in ipairs({"status", "stage", "progress", "start", "advance", "complete", "reset"}) do
                    assert(type(envoys.quest[n]) == "function", n)
                end
                assert(type(envoys.merchant) == "table")
                for _, n in ipairs({"getUnlocked", "addUnlocked"}) do
                    assert(type(envoys.merchant[n]) == "function", n)
                end
                assert(type(envoys.checkpoint) == "table")
                for _, n in ipairs({"get", "set"}) do
                    assert(type(envoys.checkpoint[n]) == "function", n)
                end
                """;
        assertTrue(engine.load(script));

        engine.tick(null);

        assertFalse(engine.isErrored(), engine.errorText());
    }

    @Test
    void sayWithoutTargetIsNoop() {
        LuaNpcEngine engine = engine(2);
        assertTrue(engine.load("envoys.say('hi')"));

        engine.tick(null);

        assertFalse(engine.isErrored(), engine.errorText());
    }

    @Test
    void updateCallbackRunsWithoutError() {
        LuaNpcEngine engine = engine(3);
        assertTrue(engine.load("envoys.on('update', function() return true end, 1)"));

        engine.tick(null);

        assertFalse(engine.isErrored(), engine.errorText());
    }

    @Test
    void unknownEventNameFailsSafely() {
        LuaNpcEngine engine = engine(4);
        assertTrue(engine.load("envoys.on('nope', function() end)"));

        engine.tick(null);

        assertTrue(engine.isErrored());
    }

    private LuaNpcEngine engine(int value) {
        LuaNpcEngine engine = new LuaNpcEngine(TestSupport.uuid(value));
        engines.add(engine);
        return engine;
    }
}
