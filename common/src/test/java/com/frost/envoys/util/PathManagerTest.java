package com.frost.envoys.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PathManagerTest {

    @TempDir
    Path tempDir;

    private Path worldDir;

    @BeforeEach
    void setUp() {
        worldDir = tempDir.resolve("world");
        PathManager.clearServer();
    }

    @AfterEach
    void tearDown() {
        PathManager.clearServer();
    }

    @Test
    void initServerReadsWorldPath() {
        MinecraftServer server = mock(MinecraftServer.class);
        when(server.getWorldPath(LevelResource.ROOT)).thenReturn(worldDir);

        PathManager.initServer(server);

        verify(server).getWorldPath(LevelResource.ROOT);
        assertEquals(worldDir, PathManager.getServerWorldDir());
    }

    @Test
    void clearServerForgetsWorldPath() {
        PathManager.initServer(mockServer());
        PathManager.clearServer();

        assertThrows(IllegalStateException.class, PathManager::getServerWorldDir);
    }

    @Test
    void uninitializedGettersThrow() {
        assertThrows(IllegalStateException.class, PathManager::getServerWorldDir);
        assertThrows(IllegalStateException.class, PathManager::getServerEnvoysDir);
        assertThrows(IllegalStateException.class, PathManager::getServerSkinDir);
    }

    @Test
    void gettersResolveExpectedPathsAndCreateDirectories() {
        PathManager.initServer(mockServer());

        assertEquals(worldDir.resolve("envoys"), PathManager.getServerEnvoysDir());

        Path skins = PathManager.getServerSkinDir();
        assertEquals(worldDir.resolve("envoys/skins"), skins);
        assertTrue(Files.isDirectory(skins));

        Path cache = PathManager.getServerSkinCacheDir();
        assertEquals(worldDir.resolve("envoys/skins/cache"), cache);
        assertTrue(Files.isDirectory(cache));

        assertEquals(worldDir.resolve("envoys/skin/index.json").toFile(), PathManager.getServerIndexFile());
        assertTrue(Files.isDirectory(worldDir.resolve("envoys/skin")));

        Path anim = PathManager.getServerAnimDir();
        assertEquals(worldDir.resolve("envoys/anim"), anim);
        assertTrue(Files.isDirectory(anim));

        Path npcs = PathManager.getServerNPCsDir();
        assertEquals(worldDir.resolve("envoys/npcs"), npcs);
        assertTrue(Files.isDirectory(npcs));

        Path trades = PathManager.getServerTradesDir();
        assertEquals(worldDir.resolve("envoys/trades"), trades);
        assertTrue(Files.isDirectory(trades));

        Path lua = PathManager.getServerLuaDir();
        assertEquals(worldDir.resolve("envoys/lua"), lua);
        assertTrue(Files.isDirectory(lua));
    }

    @Test
    void directoryCreationFailureIsSwallowed() throws Exception {
        PathManager.initServer(mockServer());
        Files.createDirectories(worldDir.resolve("envoys"));
        Files.writeString(worldDir.resolve("envoys/skins"), "blocker");

        Path skins = assertDoesNotThrow(PathManager::getServerSkinDir);
        assertEquals(worldDir.resolve("envoys/skins"), skins);
        assertTrue(Files.isRegularFile(skins));

        Path cache = assertDoesNotThrow(PathManager::getServerSkinCacheDir);
        assertEquals(worldDir.resolve("envoys/skins/cache"), cache);
    }

    private MinecraftServer mockServer() {
        MinecraftServer server = mock(MinecraftServer.class);
        when(server.getWorldPath(LevelResource.ROOT)).thenReturn(worldDir);
        return server;
    }
}
