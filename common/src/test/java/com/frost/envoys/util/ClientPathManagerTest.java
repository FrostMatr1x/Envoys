package com.frost.envoys.util;

import com.frost.envoys.TestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientPathManagerTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        TestSupport.installEnvironment(tempDir);
    }

    @AfterEach
    void tearDown() {
        TestSupport.clearEnvironment();
    }

    @Test
    void sanitizeFallsBackToSingleplayer() {
        assertEquals("singleplayer", ClientPathManager.sanitizeServerId(null));
        assertEquals("singleplayer", ClientPathManager.sanitizeServerId(""));
        assertEquals("singleplayer", ClientPathManager.sanitizeServerId("   "));
    }

    @Test
    void sanitizeReplacesUnsafeCharacters() {
        assertEquals("mc.example.com_25565", ClientPathManager.sanitizeServerId("mc.example.com:25565"));
        assertEquals("a_b_c", ClientPathManager.sanitizeServerId("a/b:c"));
        assertEquals("plain-name.example", ClientPathManager.sanitizeServerId("plain-name.example"));
    }

    @Test
    void resolveLocalServerAlwaysSingleplayer() {
        assertEquals("singleplayer", ClientPathManager.resolveServerId(true, "1.2.3.4", "fallback"));
        assertEquals("singleplayer", ClientPathManager.resolveServerId(true, null, "fallback"));
    }

    @Test
    void resolveRemoteServerSanitizesIp() {
        assertEquals("1.2.3.4_25565",
                ClientPathManager.resolveServerId(false, "1.2.3.4:25565", "fallback"));
    }

    @Test
    void resolveRemoteWithoutIpUsesFallback() {
        assertEquals("fallback", ClientPathManager.resolveServerId(false, null, "fallback"));
    }

    @Test
    void resolveRemoteBlankIpKeepsCurrentBehavior() {
        assertEquals("", ClientPathManager.resolveServerId(false, "", "fallback"));
    }

    @Test
    void serverScopedDirectoriesAreCreated() {
        Path envoys = ClientPathManager.getClientEnvoysDir("srv");
        assertEquals(tempDir.resolve("game/envoysCache/srv"), envoys);

        Path cache = ClientPathManager.getClientSkinCacheDir("srv");
        assertEquals(tempDir.resolve("game/envoysCache/srv/skins/cache"), cache);
        assertTrue(Files.isDirectory(cache));

        assertEquals(tempDir.resolve("game/envoysCache/srv/skin/index.json").toFile(),
                ClientPathManager.getClientIndexFile("srv"));
        assertTrue(Files.isDirectory(tempDir.resolve("game/envoysCache/srv/skin")));

        Path anim = ClientPathManager.getClientAnimDir("srv");
        assertEquals(tempDir.resolve("game/envoysCache/srv/anim"), anim);
        assertTrue(Files.isDirectory(anim));
    }

    @Test
    void localDirectoriesAreCreatedUnderGameDir() {
        Path anim = ClientPathManager.getClientLocalAnimDir();
        assertEquals(tempDir.resolve("game/envoys/anim"), anim);
        assertTrue(Files.isDirectory(anim));

        Path lua = ClientPathManager.getClientLocalLuaDir();
        assertEquals(tempDir.resolve("game/envoys/local"), lua);
        assertTrue(Files.isDirectory(lua));

        Path temp = ClientPathManager.getClientLocalTempDir();
        assertEquals(tempDir.resolve("game/envoys/local/.temp"), temp);
        assertTrue(Files.isDirectory(temp));
    }
}
