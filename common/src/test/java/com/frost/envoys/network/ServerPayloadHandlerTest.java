package com.frost.envoys.network;

import com.frost.envoys.TestSupport;
import com.frost.envoys.action.NPCInteractManager;
import com.frost.envoys.action.event.EventType;
import com.frost.envoys.action.model.ActionStart;
import com.frost.envoys.lua.LuaScriptStore;
import com.frost.envoys.network.payload.FetchNpcLuaScriptPayload;
import com.frost.envoys.network.payload.LuaScriptUploadResultPayload;
import com.frost.envoys.network.payload.NpcLuaScriptResponsePayload;
import com.frost.envoys.network.payload.SaveNpcLuaScriptPayload;
import com.frost.envoys.network.payload.SaveNPCPassportPayload;
import com.frost.envoys.network.payload.SaveNPCScriptPayload;
import com.frost.envoys.network.payload.SelectDialogAnswerPayload;
import com.frost.envoys.network.payload.TradeAllPayload;
import com.frost.envoys.npc.entity.BaseNPC;
import com.frost.envoys.platform.EnvoysPlatform;
import com.frost.envoys.skin.service.SkinCacheService;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServerPayloadHandlerTest {

    @TempDir
    Path tempDir;

    private NetworkTestSupport.RecordingNetworkManager network;
    private Path npcDir;
    private Path luaDir;

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
        npcDir = tempDir.resolve("world/envoys/npcs");
        luaDir = tempDir.resolve("world/envoys/lua");
    }

    @AfterEach
    void tearDown() {
        Network.init(null);
        EnvoysPlatform.config().luaEnabled = true;
        TestSupport.clearEnvironment();
    }

    @Test
    void hasTunerAccessRequiresPlayerAndOp() {
        assertFalse(ServerPayloadHandler.hasTunerAccess(null));
        assertFalse(ServerPayloadHandler.hasTunerAccess(NetworkTestSupport.serverPlayer(false)));
        assertTrue(ServerPayloadHandler.hasTunerAccess(NetworkTestSupport.serverPlayer(true)));
    }

    @Test
    void saveScriptWithoutOpIsRejected() {
        UUID id = TestSupport.uuid(1);
        ServerPlayer player = NetworkTestSupport.serverPlayer(id, false);
        ServerPayloadHandler.handleSaveNPCScript(
                new SaveNPCScriptPayload(id, "{}"), NetworkTestSupport.context(player));

        assertTrue(NPCInteractManager.byUUID(id).isEmpty());
        assertFalse(Files.exists(npcDir.resolve(id + ".json")));
    }

    @Test
    void saveScriptFromTooFarAwayIsRejected() {
        UUID id = TestSupport.uuid(2);
        Entity entity = mock(Entity.class);
        ServerLevel level = NetworkTestSupport.serverLevelWithNpc(id, entity);
        ServerPlayer player = NetworkTestSupport.serverPlayer(id, true, level);
        when(player.distanceToSqr(entity)).thenReturn(65.0D);

        ServerPayloadHandler.handleSaveNPCScript(
                new SaveNPCScriptPayload(id, "{}"), NetworkTestSupport.context(player));

        assertTrue(NPCInteractManager.byUUID(id).isEmpty());
        assertFalse(Files.exists(npcDir.resolve(id + ".json")));
    }

    @Test
    void saveScriptWithMalformedJsonIsIgnored() {
        UUID id = TestSupport.uuid(3);
        ServerPlayer player = NetworkTestSupport.serverPlayer(id, true);

        assertDoesNotThrow(() -> ServerPayloadHandler.handleSaveNPCScript(
                new SaveNPCScriptPayload(id, "{"), NetworkTestSupport.context(player)));

        assertFalse(Files.exists(npcDir.resolve(id + ".json")));
    }

    @Test
    void saveEmptyScriptDoesNotWipePopulatedNpc() {
        UUID id = TestSupport.uuid(4);
        NPCInteractManager manager = new NPCInteractManager(id);
        manager.getOrCreateEvent(EventType.CLICK).actions().add(new ActionStart("a"));
        ServerPlayer player = NetworkTestSupport.serverPlayer(id, true);

        ServerPayloadHandler.handleSaveNPCScript(
                new SaveNPCScriptPayload(id, "{}"), NetworkTestSupport.context(player));

        assertTrue(manager.hasActions());
        assertEquals(1, manager.getEvent(EventType.CLICK).actions().size());
        assertFalse(Files.exists(npcDir.resolve(id + ".json")));
    }

    @Test
    void saveScriptForUnknownNpcCreatesConfig() {
        UUID id = TestSupport.uuid(5);
        ServerPlayer player = NetworkTestSupport.serverPlayer(id, true);

        ServerPayloadHandler.handleSaveNPCScript(
                new SaveNPCScriptPayload(id, "{}"), NetworkTestSupport.context(player));

        assertTrue(NPCInteractManager.byUUID(id).isPresent());
        assertTrue(Files.isRegularFile(npcDir.resolve(id + ".json")));
    }

    @Test
    void savePassportWithoutOpIsRejected() {
        UUID id = TestSupport.uuid(10);
        NPCInteractManager manager = new NPCInteractManager(id);
        manager.passport.npcName = "Old";
        ServerPlayer player = NetworkTestSupport.serverPlayer(id, false);

        ServerPayloadHandler.handleSaveNPCPassport(passport(id, "New"), NetworkTestSupport.context(player));

        assertEquals("Old", manager.passport.npcName);
        assertFalse(Files.exists(npcDir.resolve(id + ".json")));
    }

    @Test
    void savePassportForUnknownNpcIsIgnored() {
        UUID id = TestSupport.uuid(11);
        ServerPlayer player = NetworkTestSupport.serverPlayer(id, true);

        ServerPayloadHandler.handleSaveNPCPassport(passport(id, "New"), NetworkTestSupport.context(player));

        assertTrue(NPCInteractManager.byUUID(id).isEmpty());
        assertFalse(Files.exists(npcDir.resolve(id + ".json")));
    }

    @Test
    void savePassportSanitizesBlankName() {
        UUID id = TestSupport.uuid(12);
        NPCInteractManager manager = new NPCInteractManager(id);
        ServerPlayer player = NetworkTestSupport.serverPlayer(id, true);

        ServerPayloadHandler.handleSaveNPCPassport(passport(id, "   "), NetworkTestSupport.context(player));

        assertEquals("Steve", manager.passport.npcName);
        assertTrue(Files.isRegularFile(npcDir.resolve(id + ".json")));
    }

    @Test
    void savePassportTruncatesLongName() {
        UUID id = TestSupport.uuid(13);
        NPCInteractManager manager = new NPCInteractManager(id);
        ServerPlayer player = NetworkTestSupport.serverPlayer(id, true);
        String longName = "a".repeat(60);

        ServerPayloadHandler.handleSaveNPCPassport(passport(id, longName), NetworkTestSupport.context(player));

        assertEquals("a".repeat(50), manager.passport.npcName);
    }

    @Test
    void saveLuaScriptWithoutOpRepliesOpOnly() {
        UUID id = TestSupport.uuid(20);
        ServerPlayer player = NetworkTestSupport.serverPlayer(id, false);

        ServerPayloadHandler.handleSaveNpcLuaScript(
                luaPayload(id, "main.lua", "print(1)", false), NetworkTestSupport.context(player));

        LuaScriptUploadResultPayload result = lastLuaResult();
        assertFalse(result.success());
        assertEquals("envoys.cmd.lua.upload_op_only", messageKey(result.message()));
    }

    @Test
    void saveLuaScriptWhenDisabledRepliesDisabled() {
        UUID id = TestSupport.uuid(21);
        EnvoysPlatform.config().luaEnabled = false;
        ServerPlayer player = NetworkTestSupport.serverPlayer(id, true);

        ServerPayloadHandler.handleSaveNpcLuaScript(
                luaPayload(id, "main.lua", "print(1)", false), NetworkTestSupport.context(player));

        LuaScriptUploadResultPayload result = lastLuaResult();
        assertFalse(result.success());
        assertEquals("envoys.cmd.lua.disabled", messageKey(result.message()));
    }

    @Test
    void saveLuaScriptForMissingNpcRepliesNotFound() {
        UUID id = TestSupport.uuid(22);
        ServerPlayer player = playerWithLevels(NetworkTestSupport.level(), true);

        ServerPayloadHandler.handleSaveNpcLuaScript(
                luaPayload(id, "main.lua", "print(1)", false), NetworkTestSupport.context(player));

        LuaScriptUploadResultPayload result = lastLuaResult();
        assertFalse(result.success());
        assertEquals("envoys.cmd.lua.npc_not_found", messageKey(result.message()));
    }

    @Test
    void saveLuaScriptRejectsInvalidFileNames() {
        UUID id = TestSupport.uuid(23);
        BaseNPC npc = mock(BaseNPC.class);
        ServerPlayer player = playerWithLevels(NetworkTestSupport.serverLevelWithNpc(id, npc), true);

        for (String fileName : new String[]{"../x.lua", "x.txt", "", null}) {
            ServerPayloadHandler.handleSaveNpcLuaScript(
                    luaPayload(id, fileName, "print(1)", false), NetworkTestSupport.context(player));
            LuaScriptUploadResultPayload result = lastLuaResult();
            assertFalse(result.success(), String.valueOf(fileName));
            assertEquals("envoys.cmd.lua.bad_name", messageKey(result.message()), String.valueOf(fileName));
        }
    }

    @Test
    void saveLuaScriptRejectsOversizedSource() {
        UUID id = TestSupport.uuid(24);
        BaseNPC npc = mock(BaseNPC.class);
        ServerPlayer player = playerWithLevels(NetworkTestSupport.serverLevelWithNpc(id, npc), true);

        ServerPayloadHandler.handleSaveNpcLuaScript(
                luaPayload(id, "main.lua", "a".repeat(1024 * 1024 + 1), false),
                NetworkTestSupport.context(player));

        LuaScriptUploadResultPayload result = lastLuaResult();
        assertFalse(result.success());
        assertEquals("envoys.cmd.lua.too_big", messageKey(result.message()));
        assertFalse(Files.isRegularFile(luaDir.resolve(id.toString()).resolve("main.lua")));
    }

    @Test
    void saveLuaScriptRejectsCompileErrors() {
        UUID id = TestSupport.uuid(25);
        BaseNPC npc = mock(BaseNPC.class);
        ServerPlayer player = playerWithLevels(NetworkTestSupport.serverLevelWithNpc(id, npc), true);

        ServerPayloadHandler.handleSaveNpcLuaScript(
                luaPayload(id, "main.lua", "local", false), NetworkTestSupport.context(player));

        LuaScriptUploadResultPayload result = lastLuaResult();
        assertFalse(result.success());
        assertEquals("envoys.cmd.lua.compile_error", messageKey(result.message()));
    }

    @Test
    void saveLuaScriptWritesFileAndHashWithoutRestart() throws Exception {
        UUID id = TestSupport.uuid(26);
        BaseNPC npc = mock(BaseNPC.class);
        ServerPlayer player = playerWithLevels(NetworkTestSupport.serverLevelWithNpc(id, npc), true);
        String source = "local x = 1\nreturn x\n";

        ServerPayloadHandler.handleSaveNpcLuaScript(
                luaPayload(id, "main.lua", source, false), NetworkTestSupport.context(player));

        LuaScriptUploadResultPayload result = lastLuaResult();
        assertTrue(result.success());
        assertEquals("envoys.cmd.lua.saved_no_restart", messageKey(result.message()));
        assertEquals(id.toString(), result.npcUuid());
        assertEquals(SkinCacheService.calculateSHA256(source.getBytes(StandardCharsets.UTF_8)),
                result.sourceHash());
        assertEquals(source, Files.readString(luaDir.resolve(id.toString()).resolve("main.lua")));
    }

    @Test
    void fetchLuaScriptWithoutOpIsRejected() {
        UUID id = TestSupport.uuid(30);
        ServerPlayer player = NetworkTestSupport.serverPlayer(id, false);

        ServerPayloadHandler.handleFetchNpcLuaScript(
                new FetchNpcLuaScriptPayload(id), NetworkTestSupport.context(player));

        NpcLuaScriptResponsePayload result = lastResponse();
        assertFalse(result.exists());
        assertEquals("envoys.cmd.lua.fetch_op_only", messageKey(result.message()));
    }

    @Test
    void fetchMissingLuaScriptRepliesNotFound() {
        UUID id = TestSupport.uuid(31);
        ServerPlayer player = NetworkTestSupport.serverPlayer(id, true);

        ServerPayloadHandler.handleFetchNpcLuaScript(
                new FetchNpcLuaScriptPayload(id), NetworkTestSupport.context(player));

        NpcLuaScriptResponsePayload result = lastResponse();
        assertFalse(result.exists());
        assertEquals("envoys.cmd.lua.fetch_not_found", messageKey(result.message()));
    }

    @Test
    void fetchExistingLuaScriptReturnsSource() {
        UUID id = TestSupport.uuid(32);
        ServerPlayer player = NetworkTestSupport.serverPlayer(id, true);
        assertTrue(LuaScriptStore.writeScript(id, "return 42"));

        ServerPayloadHandler.handleFetchNpcLuaScript(
                new FetchNpcLuaScriptPayload(id), NetworkTestSupport.context(player));

        NpcLuaScriptResponsePayload result = lastResponse();
        assertTrue(result.exists());
        assertEquals("return 42", result.source());
        assertEquals("main.lua", result.fileName());
        assertEquals("envoys.cmd.lua.fetch_ok", messageKey(result.message()));
    }

    @Test
    void selectDialogAnswerWithMissingEntityIsNoop() {
        UUID id = TestSupport.uuid(40);
        ServerLevel level = NetworkTestSupport.serverLevelWithNpc(id, null);
        ServerPlayer player = NetworkTestSupport.serverPlayer(id, true, level);

        assertDoesNotThrow(() -> ServerPayloadHandler.handleSelectDialogAnswer(
                new SelectDialogAnswerPayload(id, "next"), NetworkTestSupport.context(player)));
    }

    @Test
    void tradeAllWithoutContainerMenuIsNoop() {
        UUID id = TestSupport.uuid(41);
        ServerPlayer player = NetworkTestSupport.serverPlayer(id, true);

        assertDoesNotThrow(() -> ServerPayloadHandler.handleTradeAll(
                new TradeAllPayload(0), NetworkTestSupport.context(player)));
    }

    private ServerPlayer playerWithLevels(ServerLevel level, boolean op) {
        ServerPlayer player = NetworkTestSupport.serverPlayer(UUID.randomUUID(), op, level);
        MinecraftServer server = NetworkTestSupport.serverWithLevels(level);
        when(player.getServer()).thenReturn(server);
        return player;
    }

    private static SaveNPCPassportPayload passport(UUID id, String name) {
        return new SaveNPCPassportPayload(id, name, 1.2f, 0.3f, 30.0f, new Vec3(1, 2, 3),
                true, true, false, true, false, true, "wave");
    }

    private static SaveNpcLuaScriptPayload luaPayload(UUID id, String fileName, String source, boolean restart) {
        return new SaveNpcLuaScriptPayload(id, fileName, source, restart);
    }

    private LuaScriptUploadResultPayload lastLuaResult() {
        assertFalse(network.toPlayer().isEmpty());
        return (LuaScriptUploadResultPayload) network.toPlayer().get(network.toPlayer().size() - 1);
    }

    private NpcLuaScriptResponsePayload lastResponse() {
        assertFalse(network.toPlayer().isEmpty());
        return (NpcLuaScriptResponsePayload) network.toPlayer().get(network.toPlayer().size() - 1);
    }

    private static String messageKey(Component component) {
        assertNotNull(component);
        if (component.getContents() instanceof TranslatableContents translatable) {
            return translatable.getKey();
        }
        return component.getString();
    }
}
