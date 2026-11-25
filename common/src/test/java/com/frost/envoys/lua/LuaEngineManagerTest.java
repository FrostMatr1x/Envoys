package com.frost.envoys.lua;

import com.frost.envoys.TestSupport;
import com.frost.envoys.platform.EnvoysPlatform;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LuaEngineManagerTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        TestSupport.installEnvironment(tempDir);
        TestSupport.resetLuaEngines();
        EnvoysPlatform.config().luaEnabled = true;
    }

    @AfterEach
    void tearDown() {
        TestSupport.resetLuaEngines();
        EnvoysPlatform.config().luaEnabled = true;
        TestSupport.clearEnvironment();
    }

    @Test
    void ensureReturnsCachedEngineForExistingScript() {
        UUID id = TestSupport.uuid(1);
        assertTrue(LuaScriptStore.writeScript(id, "return 1"));

        LuaNpcEngine first = LuaEngineManager.ensure(id);
        LuaNpcEngine second = LuaEngineManager.ensure(id);

        assertNotNull(first);
        assertSame(first, second);
    }

    @Test
    void ensureCachesAbsentForMissingScript() {
        UUID id = TestSupport.uuid(2);

        assertNull(LuaEngineManager.ensure(id));

        assertTrue(LuaScriptStore.writeScript(id, "return 1"));
        assertNull(LuaEngineManager.ensure(id));
    }

    @Test
    void createOrRestartBypassesAbsentCache() {
        UUID id = TestSupport.uuid(3);
        assertNull(LuaEngineManager.ensure(id));

        assertTrue(LuaScriptStore.writeScript(id, "return 1"));
        LuaNpcEngine engine = LuaEngineManager.createOrRestart(id);

        assertNotNull(engine);
        assertSame(engine, LuaEngineManager.ensure(id));
    }

    @Test
    void createOrRestartShutsDownPreviousInstance() {
        UUID id = TestSupport.uuid(4);
        assertTrue(LuaScriptStore.writeScript(id, "return 1"));

        LuaNpcEngine first = LuaEngineManager.createOrRestart(id);
        LuaNpcEngine second = LuaEngineManager.createOrRestart(id);

        assertNotNull(first);
        assertNotNull(second);
        assertNotSame(first, second);
        assertTrue(first.isShutdown());
        assertSame(second, LuaEngineManager.getEngine(id));
    }

    @Test
    void reloadAllCountsLoadedAndFailed() {
        UUID validA = TestSupport.uuid(10);
        UUID validB = TestSupport.uuid(11);
        UUID broken = TestSupport.uuid(12);
        assertTrue(LuaScriptStore.writeScript(validA, "return 1"));
        assertTrue(LuaScriptStore.writeScript(validB, "return 2"));
        assertTrue(LuaScriptStore.writeScript(broken, "return ("));

        LuaEngineManager.ReloadResult result = LuaEngineManager.reloadAll();

        assertEquals(2, result.loaded());
        assertEquals(1, result.failed());
        assertNotNull(LuaEngineManager.getEngine(validA));
        assertNotNull(LuaEngineManager.getEngine(validB));
        assertNull(LuaEngineManager.getEngine(broken));
    }

    @Test
    void reloadAllRecoversPreviouslyFailedScript() {
        UUID validA = TestSupport.uuid(13);
        UUID validB = TestSupport.uuid(14);
        UUID broken = TestSupport.uuid(15);
        assertTrue(LuaScriptStore.writeScript(validA, "return 1"));
        assertTrue(LuaScriptStore.writeScript(validB, "return 2"));
        assertTrue(LuaScriptStore.writeScript(broken, "return ("));

        LuaEngineManager.ReloadResult first = LuaEngineManager.reloadAll();
        assertEquals(1, first.failed());

        assertTrue(LuaScriptStore.writeScript(broken, "return 3"));
        LuaEngineManager.ReloadResult second = LuaEngineManager.reloadAll();

        assertEquals(3, second.loaded());
        assertEquals(0, second.failed());
    }

    @Test
    void reloadAllClearsStoppedState() {
        UUID id = TestSupport.uuid(16);
        assertTrue(LuaScriptStore.writeScript(id, "return 1"));

        assertNotNull(LuaEngineManager.ensure(id));
        LuaEngineManager.stop(id);
        assertTrue(LuaEngineManager.isStopped(id));

        LuaEngineManager.ReloadResult result = LuaEngineManager.reloadAll();

        assertEquals(1, result.loaded());
        assertFalse(LuaEngineManager.isStopped(id));
        assertNotNull(LuaEngineManager.getEngine(id));
    }

    @Test
    void stopShutsDownAndBlocksEnsure() {
        UUID id = TestSupport.uuid(17);
        assertTrue(LuaScriptStore.writeScript(id, "return 1"));

        LuaNpcEngine engine = LuaEngineManager.ensure(id);
        assertNotNull(engine);

        LuaEngineManager.stop(id);

        assertTrue(engine.isShutdown());
        assertNull(LuaEngineManager.getEngine(id));
        assertTrue(LuaEngineManager.isStopped(id));
        assertNull(LuaEngineManager.ensure(id));
    }

    @Test
    void nullStopIsNoopAndNullIsNotStopped() {
        assertDoesNotThrow(() -> LuaEngineManager.stop(null));
        assertFalse(LuaEngineManager.isStopped(null));
    }

    @Test
    void removeClearsAbsentCache() {
        UUID id = TestSupport.uuid(18);
        assertNull(LuaEngineManager.ensure(id));

        LuaEngineManager.remove(id);
        assertTrue(LuaScriptStore.writeScript(id, "return 1"));

        assertNotNull(LuaEngineManager.ensure(id));
    }

    @Test
    void removeShutsDownEngineWithoutMarkingStopped() {
        UUID id = TestSupport.uuid(19);
        assertTrue(LuaScriptStore.writeScript(id, "return 1"));

        LuaNpcEngine engine = LuaEngineManager.ensure(id);
        assertNotNull(engine);

        LuaEngineManager.remove(id);

        assertTrue(engine.isShutdown());
        assertNull(LuaEngineManager.getEngine(id));
        assertFalse(LuaEngineManager.isStopped(id));
    }

    @Test
    void luaDisabledDisablesAllEntryPoints() {
        UUID id = TestSupport.uuid(20);
        assertTrue(LuaScriptStore.writeScript(id, "return 1"));
        LuaNpcEngine engine = LuaEngineManager.ensure(id);
        assertNotNull(engine);

        EnvoysPlatform.config().luaEnabled = false;

        assertNull(LuaEngineManager.ensure(id));
        assertNull(LuaEngineManager.createOrRestart(id));
        assertNull(LuaEngineManager.getEngine(id));

        LuaEngineManager.ReloadResult result = LuaEngineManager.reloadAll();
        assertEquals(0, result.loaded());
        assertEquals(0, result.failed());
        assertTrue(engine.isShutdown());
    }

    @Test
    void reloadResultRecordExposesCounts() {
        LuaEngineManager.ReloadResult result = new LuaEngineManager.ReloadResult(2, 1);

        assertEquals(2, result.loaded());
        assertEquals(1, result.failed());
    }
}
