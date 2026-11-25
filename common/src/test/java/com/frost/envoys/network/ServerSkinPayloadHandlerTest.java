package com.frost.envoys.network;

import com.frost.envoys.TestSupport;
import com.frost.envoys.action.NPCInteractManager;
import com.frost.envoys.network.payload.RequestSkinPayload;
import com.frost.envoys.network.payload.SaveNPCSkinPayload;
import com.frost.envoys.network.payload.SkinConfirmedPayload;
import com.frost.envoys.platform.EnvoysPlatform;
import com.frost.envoys.skin.model.SkinIndexData;
import com.frost.envoys.skin.service.SkinCacheService;
import com.frost.envoys.skin.service.SkinLocalService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ServerSkinPayloadHandlerTest {

    private static final int MAX_SKIN_BYTES = 2 * 1024 * 1024;

    @TempDir
    Path tempDir;

    private NetworkTestSupport.RecordingNetworkManager network;
    private Path cacheDir;
    private Path indexFile;

    @BeforeEach
    void setUp() {
        NetworkTestSupport.bootStrap();
        TestSupport.installEnvironment(tempDir);
        TestSupport.resetNpcConfigMigration();
        TestSupport.resetQuestState();
        TestSupport.resetRequestRateLimiter();
        EnvoysPlatform.config().luaEnabled = true;
        network = new NetworkTestSupport.RecordingNetworkManager();
        Network.init(network);
        cacheDir = tempDir.resolve("world/envoys/skins/cache");
        indexFile = tempDir.resolve("world/envoys/skin/index.json");
    }

    @AfterEach
    void tearDown() {
        Network.init(null);
        TestSupport.clearEnvironment();
    }

    @Test
    void requestSkinIsRateLimited() {
        UUID playerId = TestSupport.uuid(1);
        ServerPlayer player = NetworkTestSupport.serverPlayer(playerId, true);
        for (int i = 0; i < 10; i++) {
            assertTrue(RequestRateLimiter.tryAcquire(playerId));
        }

        ServerSkinPayloadHandler.handleRequestSkin(
                new RequestSkinPayload(TestSupport.uuid(2)), NetworkTestSupport.context(player));

        assertTrue(network.toPlayer().isEmpty());
    }

    @Test
    void requestSkinIgnoresNonServerPlayer() {
        Player player = NetworkTestSupport.player();

        ServerSkinPayloadHandler.handleRequestSkin(
                new RequestSkinPayload(TestSupport.uuid(3)), NetworkTestSupport.context(player));

        assertTrue(network.toPlayer().isEmpty());
    }

    @Test
    void saveSkinWithoutOpIsRejected() {
        UUID id = TestSupport.uuid(10);
        NPCInteractManager manager = manager(id);
        manager.passport.skinType = "NICKNAME";
        manager.passport.skinValue = "Old";
        ServerPlayer player = NetworkTestSupport.serverPlayer(id, false);

        ServerSkinPayloadHandler.handleSaveNPCSkin(
                skin(id, "NICKNAME", "Steve", "classic", TestSupport.png64(false)),
                NetworkTestSupport.context(player));

        assertEquals("Old", manager.passport.skinValue);
        assertTrue(network.toPlayer().isEmpty());
        assertFalse(Files.exists(cacheDir.resolve("Steve.png")));
    }

    @Test
    void saveSkinForUnknownNpcIsIgnored() {
        UUID id = TestSupport.uuid(11);
        ServerPlayer player = NetworkTestSupport.serverPlayer(id, true);

        ServerSkinPayloadHandler.handleSaveNPCSkin(
                skin(id, "NICKNAME", "Steve", "classic", TestSupport.png64(false)),
                NetworkTestSupport.context(player));

        assertTrue(NPCInteractManager.byUUID(id).isEmpty());
        assertTrue(network.toPlayer().isEmpty());
    }

    @Test
    void saveSkinRejectsInvalidType() {
        UUID id = TestSupport.uuid(12);
        NPCInteractManager manager = manager(id);
        manager.passport.skinValue = "Old";
        ServerPlayer player = NetworkTestSupport.serverPlayer(id, true);

        ServerSkinPayloadHandler.handleSaveNPCSkin(
                skin(id, "BAD", "Steve", "classic", TestSupport.png64(false)),
                NetworkTestSupport.context(player));

        assertEquals("Old", manager.passport.skinValue);
        assertTrue(network.toPlayer().isEmpty());
    }

    @Test
    void saveSkinRejectsOversizedPng() {
        UUID id = TestSupport.uuid(13);
        NPCInteractManager manager = manager(id);
        manager.passport.skinValue = "Old";
        ServerPlayer player = NetworkTestSupport.serverPlayer(id, true);

        ServerSkinPayloadHandler.handleSaveNPCSkin(
                skin(id, "NICKNAME", "Steve", "classic", new byte[MAX_SKIN_BYTES + 1]),
                NetworkTestSupport.context(player));

        assertEquals("Old", manager.passport.skinValue);
        assertTrue(network.toPlayer().isEmpty());
    }

    @Test
    void saveSkinRejectsInvalidPngSignature() {
        UUID id = TestSupport.uuid(14);
        NPCInteractManager manager = manager(id);
        manager.passport.skinValue = "Old";
        ServerPlayer player = NetworkTestSupport.serverPlayer(id, true);

        ServerSkinPayloadHandler.handleSaveNPCSkin(
                skin(id, "NICKNAME", "Steve", "classic", new byte[]{1, 2, 3}),
                NetworkTestSupport.context(player));

        assertEquals("Old", manager.passport.skinValue);
        assertTrue(network.toPlayer().isEmpty());
    }

    @Test
    void saveSkinWithWideModelStoresClassic() throws Exception {
        UUID id = TestSupport.uuid(15);
        NPCInteractManager manager = manager(id);
        ServerPlayer player = NetworkTestSupport.serverPlayer(id, true);
        byte[] png = TestSupport.png64(false);
        String hash = SkinCacheService.calculateSHA256(png);

        ServerSkinPayloadHandler.handleSaveNPCSkin(
                skin(id, "NICKNAME", "Steve", "wide", png), NetworkTestSupport.context(player));

        assertEquals("NICKNAME", manager.passport.skinType);
        assertEquals("Steve", manager.passport.skinValue);
        assertEquals("classic", manager.passport.skinModel);
        assertEquals(hash, manager.passport.skinHash);
        assertTrue(Files.isRegularFile(cacheDir.resolve("Steve.png")));
        assertTrue(Files.isRegularFile(indexFile));

        SkinIndexData index = SkinLocalService.readIndex(indexFile.toFile());
        assertNotNull(index);
        assertEquals(1, index.skins.size());
        assertEquals("Steve", index.skins.get(0).uuid);
        assertEquals(hash, index.skins.get(0).hash);

        assertEquals(1, network.toPlayer().size());
        SkinConfirmedPayload confirmed = (SkinConfirmedPayload) network.toPlayer().get(0);
        assertEquals(id, confirmed.npcUuid());
        assertEquals(hash, confirmed.hash());
    }

    @Test
    void saveSkinWithSlimModelStoresSlim() {
        UUID id = TestSupport.uuid(16);
        NPCInteractManager manager = manager(id);
        ServerPlayer player = NetworkTestSupport.serverPlayer(id, true);

        ServerSkinPayloadHandler.handleSaveNPCSkin(
                skin(id, "NICKNAME", "Alex", "slim", TestSupport.png64(true)),
                NetworkTestSupport.context(player));

        assertEquals("slim", manager.passport.skinModel);
        assertTrue(Files.isRegularFile(cacheDir.resolve("Alex.png")));
    }

    @Test
    void saveSkinTooFarAwayIsRejected() {
        UUID id = TestSupport.uuid(17);
        NPCInteractManager manager = manager(id);
        manager.passport.skinValue = "Old";
        net.minecraft.world.entity.Entity entity = mock(net.minecraft.world.entity.Entity.class);
        ServerLevel level = NetworkTestSupport.serverLevelWithNpc(id, entity);
        ServerPlayer player = NetworkTestSupport.serverPlayer(id, true, level);
        org.mockito.Mockito.when(player.distanceToSqr(entity)).thenReturn(100.0D);

        ServerSkinPayloadHandler.handleSaveNPCSkin(
                skin(id, "NICKNAME", "Steve", "classic", TestSupport.png64(false)),
                NetworkTestSupport.context(player));

        assertEquals("Old", manager.passport.skinValue);
        assertTrue(network.toPlayer().isEmpty());
    }

    private static NPCInteractManager manager(UUID id) {
        return new NPCInteractManager(id);
    }

    private static SaveNPCSkinPayload skin(UUID id, String type, String value, String model, byte[] png) {
        return new SaveNPCSkinPayload(id, type, value, model, png);
    }
}
