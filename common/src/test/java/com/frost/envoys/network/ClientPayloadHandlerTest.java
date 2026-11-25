package com.frost.envoys.network;

import com.frost.envoys.TestSupport;
import com.frost.envoys.client.ClientLuaScriptBridge;
import com.frost.envoys.client.quest.ClientQuestTracker;
import com.frost.envoys.network.payload.ClientQuestEntry;
import com.frost.envoys.network.payload.FetchNpcLuaScriptPayload;
import com.frost.envoys.network.payload.NpcLuaScriptResponsePayload;
import com.frost.envoys.network.payload.SyncPlayerQuestsPayload;
import com.frost.envoys.quest.QuestType;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ClientPayloadHandlerTest {

    private NetworkTestSupport.RecordingNetworkManager network;

    @BeforeEach
    void setUp() {
        ClientQuestTracker.get().clear();
        ClientLuaScriptBridge.clear();
        network = new NetworkTestSupport.RecordingNetworkManager();
        Network.init(network);
    }

    @AfterEach
    void tearDown() {
        Network.init(null);
        ClientQuestTracker.get().clear();
        ClientLuaScriptBridge.clear();
    }

    @Test
    void syncPlayerQuestsReplacesTrackerEntries() {
        ClientQuestEntry first = new ClientQuestEntry(
                "q-1", "First", "Bob", true, QuestType.ITEM, "minecraft:stone", 5, 2, false);
        ClientQuestEntry second = new ClientQuestEntry(
                "q-2", "Second", "Alice", false, QuestType.KILL, "minecraft:zombie", 10, 10, true);
        ClientQuestTracker.get().replace(List.of(new ClientQuestEntry(
                "stale", "Stale", "Old", true, QuestType.BOOLEAN, "", 1, 1, false)));

        ClientPayloadHandler.handleSyncPlayerQuests(
                new SyncPlayerQuestsPayload(List.of(first, second)), NetworkTestSupport.context(null));

        assertEquals(List.of(first, second), ClientQuestTracker.get().entries());
    }

    @Test
    void syncPlayerQuestsWithNullListClearsTracker() {
        ClientQuestTracker.get().replace(List.of(new ClientQuestEntry(
                "q-1", "First", "Bob", true, QuestType.ITEM, "minecraft:stone", 5, 2, false)));

        ClientPayloadHandler.handleSyncPlayerQuests(
                new SyncPlayerQuestsPayload(null), NetworkTestSupport.context(null));

        assertEquals(List.of(), ClientQuestTracker.get().entries());
    }

    @Test
    void luaScriptResponseIsDeliveredToScreenListener() {
        UUID id = TestSupport.uuid(1);
        AtomicReference<NpcLuaScriptResponsePayload> received = new AtomicReference<>();
        ClientLuaScriptBridge.requestForScreen(id, received::set);

        assertEquals(1, network.toServer().size());
        FetchNpcLuaScriptPayload request = (FetchNpcLuaScriptPayload) network.toServer().get(0);
        assertEquals(id, request.npcUuid());

        NpcLuaScriptResponsePayload payload =
                new NpcLuaScriptResponsePayload(id, "main.lua", "return 1", true, Component.empty());
        ClientPayloadHandler.handleNpcLuaScriptResponse(payload, NetworkTestSupport.context(null));

        assertSame(payload, received.get());
    }

    @Test
    void repeatedLuaScriptResponseWithoutListenerDoesNotThrow() {
        UUID id = TestSupport.uuid(2);
        NpcLuaScriptResponsePayload payload =
                new NpcLuaScriptResponsePayload(id, "main.lua", "", false, Component.empty());

        ClientPayloadHandler.handleNpcLuaScriptResponse(payload, NetworkTestSupport.context(null));

        assertDoesNotThrow(() -> ClientPayloadHandler.handleNpcLuaScriptResponse(
                payload, NetworkTestSupport.context(null)));
    }
}
