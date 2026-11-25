package com.frost.envoys.network;

import com.frost.envoys.TestSupport;
import com.frost.envoys.network.payload.AnimOpResultPayload;
import com.frost.envoys.network.payload.AnimRemovedPayload;
import com.frost.envoys.network.payload.DeleteAnimPayload;
import com.frost.envoys.network.payload.PushAnimPayload;
import com.frost.envoys.network.payload.RequestAnimListPayload;
import com.frost.envoys.network.payload.RequestAnimPayload;
import com.frost.envoys.skin.service.AnimSyncService;
import com.frost.envoys.skin.service.SkinCacheService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerAnimPayloadHandlerTest {

    private static final int MAX_ANIM_BYTES = 2 * 1024 * 1024;

    @TempDir
    Path tempDir;

    private NetworkTestSupport.RecordingNetworkManager network;
    private Path animDir;

    @BeforeEach
    void setUp() {
        NetworkTestSupport.bootStrap();
        TestSupport.installEnvironment(tempDir);
        TestSupport.resetRequestRateLimiter();
        TestSupport.await(AnimSyncService.refreshCache());
        network = new NetworkTestSupport.RecordingNetworkManager();
        Network.init(network);
        animDir = tempDir.resolve("world/envoys/anim");
    }

    @AfterEach
    void tearDown() {
        Network.init(null);
        TestSupport.clearEnvironment();
    }

    @Test
    void requestAnimListIsRateLimited() {
        UUID playerId = TestSupport.uuid(1);
        ServerPlayer player = NetworkTestSupport.serverPlayer(playerId, true);
        for (int i = 0; i < 10; i++) {
            assertTrue(RequestRateLimiter.tryAcquire(playerId));
        }

        ServerAnimPayloadHandler.handleRequestAnimList(
                new RequestAnimListPayload(), NetworkTestSupport.context(player));

        assertTrue(network.toPlayer().isEmpty());
    }

    @Test
    void requestAnimListIgnoresNonServerPlayer() {
        Player player = NetworkTestSupport.player();

        ServerAnimPayloadHandler.handleRequestAnimList(
                new RequestAnimListPayload(), NetworkTestSupport.context(player));

        assertTrue(network.toPlayer().isEmpty());
    }

    @Test
    void requestAnimIsRateLimited() {
        UUID playerId = TestSupport.uuid(2);
        ServerPlayer player = NetworkTestSupport.serverPlayer(playerId, true);
        for (int i = 0; i < 10; i++) {
            assertTrue(RequestRateLimiter.tryAcquire(playerId));
        }

        ServerAnimPayloadHandler.handleRequestAnim(
                new RequestAnimPayload("walk"), NetworkTestSupport.context(player));

        assertTrue(network.toPlayer().isEmpty());
    }

    @Test
    void requestAnimIgnoresNonServerPlayer() {
        Player player = NetworkTestSupport.player();

        ServerAnimPayloadHandler.handleRequestAnim(
                new RequestAnimPayload("walk"), NetworkTestSupport.context(player));

        assertTrue(network.toPlayer().isEmpty());
    }

    @Test
    void pushWithoutOpIsRejectedSynchronously() {
        ServerPlayer player = NetworkTestSupport.serverPlayer(TestSupport.uuid(3), false);

        ServerAnimPayloadHandler.handlePushAnim(
                new PushAnimPayload("walk", "{}".getBytes(StandardCharsets.UTF_8), "hash"),
                NetworkTestSupport.context(player));

        AnimOpResultPayload result = lastPlayerPayload();
        assertFalse(result.success());
        assertEquals("push", result.op());
        assertEquals("walk", result.name());
        assertEquals("envoys.cmd.anim.op_only", result.message());
        assertFalse(Files.exists(animDir.resolve("walk.json")));
    }

    @Test
    void deleteWithoutOpIsRejectedSynchronously() {
        ServerPlayer player = NetworkTestSupport.serverPlayer(TestSupport.uuid(4), false);

        ServerAnimPayloadHandler.handleDeleteAnim(
                new DeleteAnimPayload("walk"), NetworkTestSupport.context(player));

        AnimOpResultPayload result = lastPlayerPayload();
        assertFalse(result.success());
        assertEquals("delete", result.op());
        assertEquals("walk", result.name());
        assertEquals("envoys.cmd.anim.op_only", result.message());
    }

    @Test
    void pushRejectsOversizedData() {
        ServerPlayer player = opPlayer();
        byte[] data = new byte[MAX_ANIM_BYTES + 1];

        ServerAnimPayloadHandler.handlePushAnim(
                new PushAnimPayload("huge", data, "hash"), NetworkTestSupport.context(player));

        AnimOpResultPayload result = (AnimOpResultPayload) network.awaitPlayer();
        assertFalse(result.success());
        assertEquals("envoys.cmd.anim.too_big", result.message());
    }

    @Test
    void pushRejectsHashMismatch() {
        ServerPlayer player = opPlayer();
        byte[] data = "{\"name\":\"Walk\"}".getBytes(StandardCharsets.UTF_8);

        ServerAnimPayloadHandler.handlePushAnim(
                new PushAnimPayload("walk", data, "bad-hash"), NetworkTestSupport.context(player));

        AnimOpResultPayload result = (AnimOpResultPayload) network.awaitPlayer();
        assertFalse(result.success());
        assertEquals("envoys.cmd.anim.hash_mismatch", result.message());
    }

    @Test
    void pushRejectsMalformedJson() {
        ServerPlayer player = opPlayer();
        byte[] data = "{".getBytes(StandardCharsets.UTF_8);
        String hash = SkinCacheService.calculateSHA256(data);

        ServerAnimPayloadHandler.handlePushAnim(
                new PushAnimPayload("walk", data, hash), NetworkTestSupport.context(player));

        AnimOpResultPayload result = (AnimOpResultPayload) network.awaitPlayer();
        assertFalse(result.success());
        assertEquals("envoys.cmd.anim.invalid_json", result.message());
    }

    @Test
    void pushValidAnimationWritesFile() {
        ServerPlayer player = opPlayer();
        byte[] data = "{\"name\":\"Walk\"}".getBytes(StandardCharsets.UTF_8);
        String hash = SkinCacheService.calculateSHA256(data);

        ServerAnimPayloadHandler.handlePushAnim(
                new PushAnimPayload("walk", data, hash), NetworkTestSupport.context(player));

        AnimOpResultPayload result = (AnimOpResultPayload) network.awaitPlayer();
        assertTrue(result.success());
        assertEquals("envoys.cmd.anim.push_done", result.message());
        assertTrue(Files.isRegularFile(animDir.resolve("walk.json")));
    }

    @Test
    void deleteMissingAnimationRejects() {
        ServerPlayer player = opPlayer();

        ServerAnimPayloadHandler.handleDeleteAnim(
                new DeleteAnimPayload("ghost"), NetworkTestSupport.context(player));

        AnimOpResultPayload result = (AnimOpResultPayload) network.awaitPlayer();
        assertFalse(result.success());
        assertEquals("envoys.cmd.anim.not_found_server", result.message());
        assertTrue(network.toAll().isEmpty());
    }

    @Test
    void deleteExistingAnimationRemovesFileAndBroadcasts() throws Exception {
        ServerPlayer player = opPlayer();
        Files.writeString(animDir.resolve("walk.json"), "{\"name\":\"Walk\"}", StandardCharsets.UTF_8);
        TestSupport.await(AnimSyncService.refreshCache());

        ServerAnimPayloadHandler.handleDeleteAnim(
                new DeleteAnimPayload("Walk"), NetworkTestSupport.context(player));

        AnimOpResultPayload result = (AnimOpResultPayload) network.awaitPlayer();
        assertTrue(result.success());
        assertEquals("envoys.cmd.anim.delete_done", result.message());
        assertFalse(Files.exists(animDir.resolve("walk.json")));
        assertEquals(1, network.toAll().size());
        AnimRemovedPayload removed = (AnimRemovedPayload) network.toAll().get(0);
        assertEquals("Walk", removed.name());
    }

    private static ServerPlayer opPlayer() {
        ServerPlayer player = NetworkTestSupport.serverPlayer(UUID.randomUUID(), true);
        NetworkTestSupport.setServerField(player, NetworkTestSupport.serverWithLevels());
        return player;
    }

    private AnimOpResultPayload lastPlayerPayload() {
        assertFalse(network.toPlayer().isEmpty());
        return (AnimOpResultPayload) network.toPlayer().get(network.toPlayer().size() - 1);
    }
}
