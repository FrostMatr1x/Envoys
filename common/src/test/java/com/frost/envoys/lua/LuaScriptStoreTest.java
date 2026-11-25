package com.frost.envoys.lua;

import com.frost.envoys.TestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LuaScriptStoreTest {

    @TempDir
    Path tempDir;

    private UUID npc;

    @BeforeEach
    void setUp() {
        TestSupport.installEnvironment(tempDir);
        npc = TestSupport.uuid(1);
    }

    @AfterEach
    void tearDown() {
        TestSupport.clearEnvironment();
    }

    @Test
    void npcDirAndScriptFileResolveUnderWorld() {
        assertEquals(tempDir.resolve("world/envoys/lua").resolve(npc.toString()),
                LuaScriptStore.npcDir(npc));
        assertEquals(tempDir.resolve("world/envoys/lua").resolve(npc.toString()).resolve("main.lua"),
                LuaScriptStore.scriptFile(npc));
    }

    @Test
    void scriptExistsIsFalseUntilWritten() {
        assertFalse(LuaScriptStore.scriptExists(npc));
        assertTrue(LuaScriptStore.writeScript(npc, "print('hi')"));
        assertTrue(LuaScriptStore.scriptExists(npc));
    }

    @Test
    void scriptExistsIsFalseForDirectory() throws Exception {
        Files.createDirectories(LuaScriptStore.npcDir(npc).resolve("main.lua"));
        assertFalse(LuaScriptStore.scriptExists(npc));
    }

    @Test
    void writeScriptCreatesDirectoriesAndPreservesUtf8() {
        String source = "print('привет')";
        assertTrue(LuaScriptStore.writeScript(npc, source));
        assertTrue(Files.isDirectory(LuaScriptStore.npcDir(npc)));
        assertEquals(source, LuaScriptStore.readScript(npc));
    }

    @Test
    void writeScriptNullWritesEmptyContent() {
        assertTrue(LuaScriptStore.writeScript(npc, null));
        assertEquals("", LuaScriptStore.readScript(npc));
    }

    @Test
    void writeScriptFailsWhenTargetIsDirectory() throws Exception {
        Files.createDirectories(LuaScriptStore.npcDir(npc).resolve("main.lua"));
        assertFalse(LuaScriptStore.writeScript(npc, "print('x')"));
    }

    @Test
    void readScriptReturnsNullForMissingAndDirectory() throws Exception {
        assertNull(LuaScriptStore.readScript(npc));

        Files.createDirectories(LuaScriptStore.npcDir(npc).resolve("main.lua"));
        assertNull(LuaScriptStore.readScript(npc));
    }

    @Test
    void listNpcsWithScriptsOnlyReturnsValidDirectoriesWithMainScript() throws Exception {
        UUID valid = TestSupport.uuid(2);
        UUID withoutScript = TestSupport.uuid(3);
        UUID plainFile = TestSupport.uuid(4);

        assertTrue(LuaScriptStore.writeScript(valid, "print('x')"));
        Files.createDirectories(LuaScriptStore.npcDir(withoutScript));
        Files.createDirectories(LuaScriptStore.npcDir(plainFile).getParent());
        Files.writeString(LuaScriptStore.npcDir(plainFile), "not a directory", StandardCharsets.UTF_8);

        Path invalidName = LuaScriptStore.npcDir(plainFile).getParent().resolve("not-a-uuid");
        Files.createDirectories(invalidName);
        Files.writeString(invalidName.resolve("main.lua"), "print('x')", StandardCharsets.UTF_8);

        assertEquals(java.util.List.of(valid), LuaScriptStore.listNpcsWithScripts());
    }

    @Test
    void listNpcsWithScriptsIsEmptyWhenRootIsMissing() throws Exception {
        Path envoys = tempDir.resolve("world/envoys");
        if (Files.exists(envoys)) {
            try (var stream = Files.walk(envoys)) {
                stream.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception ignored) {
                    }
                });
            }
        }

        assertTrue(LuaScriptStore.listNpcsWithScripts().isEmpty());
    }
}
