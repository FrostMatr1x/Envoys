package com.frost.envoys.action.serialization;

import com.frost.envoys.TestSupport;
import com.frost.envoys.action.NPCInteractManager;
import com.frost.envoys.action.NPCScriptData;
import com.frost.envoys.action.event.EventType;
import com.frost.envoys.action.event.NpcClickEvent;
import com.frost.envoys.action.event.NpcEventData;
import com.frost.envoys.action.event.NpcKickEvent;
import com.frost.envoys.action.event.NpcRangeEvent;
import com.frost.envoys.action.event.NpcUpdateEvent;
import com.frost.envoys.action.model.ActionDialog;
import com.frost.envoys.action.model.ActionStart;
import com.frost.envoys.action.model.ActionTrade;
import com.frost.envoys.config.NPCConfigManager;
import com.frost.envoys.network.NetworkTestSupport;
import com.frost.envoys.npc.NPCTrade;
import com.frost.envoys.platform.TestAttachments;
import com.frost.envoys.quest.QuestDefinition;
import com.frost.envoys.quest.QuestType;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NPCScriptDataSerializationTest {

    private static final String QUEST_UUID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

    @TempDir
    Path tempDir;

    @BeforeAll
    static void bootstrap() {
        NetworkTestSupport.bootStrap();
    }

    @BeforeEach
    void setUp() {
        TestSupport.installEnvironment(tempDir, new TestAttachments());
        TestSupport.resetQuestState();
    }

    @AfterEach
    void tearDown() {
        TestSupport.resetQuestState();
        TestSupport.clearEnvironment();
    }

    @Test
    void roundTripsFullScript() {
        UUID npcId = TestSupport.uuid(1);
        NPCScriptData data = new NPCScriptData();
        data.npcUUID = npcId.toString();
        data.passport.npcName = "Merchant";
        data.passport.emote = "wave";

        NpcClickEvent click = new NpcClickEvent();
        click.setEnabled(true);
        click.actions().add(tradeAction());
        click.actions().add(dialogAction());
        data.events.put(EventType.CLICK.jsonKey(), click);

        NpcUpdateEvent update = new NpcUpdateEvent();
        update.setEnabled(true);
        update.actions().add(new ActionStart("start-1"));
        data.events.put(EventType.UPDATE.jsonKey(), update);
        data.events.put(EventType.KICK.jsonKey(), new NpcKickEvent());
        NpcRangeEvent range = new NpcRangeEvent();
        range.rangeDistance = 8.5f;
        data.events.put(EventType.RANGE.jsonKey(), range);

        data.quests.add(quest("q", QUEST_UUID));

        NPCScriptData restored = EntityActionAdapter.GSON.fromJson(
                EntityActionAdapter.GSON.toJsonTree(data), NPCScriptData.class);

        assertNotNull(restored);
        assertEquals(npcId.toString(), restored.npcUUID);
        assertEquals("Merchant", restored.passport.npcName);
        assertEquals("wave", restored.passport.emote);
        assertEquals(EventType.values().length, restored.events.size());

        NpcEventData restoredClick = restored.events.get(EventType.CLICK.jsonKey());
        assertNotNull(restoredClick);
        assertTrue(restoredClick.enabled());
        assertEquals(2, restoredClick.actions().size());
        assertTrue(restoredClick.actions().get(0) instanceof ActionTrade);
        ActionTrade restoredTrade = (ActionTrade) restoredClick.actions().get(0);
        assertEquals(Items.STONE, restoredTrade.trades.get(0).input1.getItem());
        assertEquals(5, restoredTrade.trades.get(0).input1.getCount());
        assertEquals(Items.EMERALD, restoredTrade.trades.get(0).output.getItem());
        assertTrue(restoredClick.actions().get(1) instanceof ActionDialog);
        assertEquals("Hello", ((ActionDialog) restoredClick.actions().get(1)).npcMessage.getString());

        NpcEventData restoredRange = restored.events.get(EventType.RANGE.jsonKey());
        assertEquals(8.5f, ((NpcRangeEvent) restoredRange).rangeDistance);

        assertEquals(1, restored.quests.size());
        assertEquals("q", restored.quests.get(0).localId);
        assertEquals(QUEST_UUID, restored.quests.get(0).questUuid);
    }

    @Test
    void fileSaveAndLoadKeepsNestedPayloads() {
        UUID npcId = TestSupport.uuid(2);
        NPCInteractManager manager = new NPCInteractManager(npcId);
        manager.passport.npcName = "Trader";

        NpcClickEvent click = new NpcClickEvent();
        click.actions().add(tradeAction());
        click.actions().add(dialogAction());
        manager.putEvent(EventType.CLICK, click);
        manager.quests.add(quest("q", QUEST_UUID));

        NPCConfigManager.save(manager);
        TestSupport.resetQuestState();

        NPCInteractManager loaded = NPCConfigManager.load(npcId).orElseThrow();

        assertEquals("Trader", loaded.passport.npcName);
        NpcEventData loadedClick = loaded.events.get(EventType.CLICK);
        assertNotNull(loadedClick);
        assertEquals(2, loadedClick.actions().size());

        ActionTrade loadedTrade = (ActionTrade) loadedClick.actions().get(0);
        assertEquals(1, loadedTrade.trades.size());
        assertEquals(Items.STONE, loadedTrade.trades.get(0).input1.getItem());
        assertEquals(5, loadedTrade.trades.get(0).input1.getCount());
        assertEquals(Items.EMERALD, loadedTrade.trades.get(0).output.getItem());

        ActionDialog loadedDialog = (ActionDialog) loadedClick.actions().get(1);
        assertEquals("Hello", loadedDialog.npcMessage.getString());
        assertEquals("a1", loadedDialog.answers.get("Yes"));

        assertEquals(1, loaded.quests.size());
        assertEquals("q", loaded.quests.get(0).localId);
    }

    @Test
    void jsonNullReturnsNull() {
        assertNull(EntityActionAdapter.GSON.fromJson("null", NPCScriptData.class));
    }

    @Test
    void jsonArrayReturnsEmptyData() {
        NPCScriptData data = EntityActionAdapter.GSON.fromJson("[]", NPCScriptData.class);

        assertNotNull(data);
        assertNull(data.npcUUID);
        assertTrue(data.events.isEmpty());
        assertTrue(data.quests.isEmpty());
        assertNotNull(data.passport);
    }

    @Test
    void emptyObjectMaterializesFourDisabledEvents() {
        NPCScriptData data = EntityActionAdapter.GSON.fromJson("{}", NPCScriptData.class);

        assertNotNull(data);
        assertNull(data.npcUUID);
        assertTrue(data.quests.isEmpty());
        assertEquals(EventType.values().length, data.events.size());
        for (EventType type : EventType.values()) {
            NpcEventData event = data.events.get(type.jsonKey());
            assertNotNull(event);
            assertFalse(event.enabled());
        }
    }

    @Test
    void unknownEventKeyIsIgnored() {
        String json = "{\"events\":{\"nope\":{\"enabled\":true,\"actions\":[]}}}";

        NPCScriptData data = EntityActionAdapter.GSON.fromJson(json, NPCScriptData.class);

        assertNotNull(data);
        assertFalse(data.events.containsKey("nope"));
        assertEquals(EventType.values().length, data.events.size());
    }

    @Test
    void malformedEventActionAbortsLoad() {
        assertThrows(JsonParseException.class, () -> EntityActionAdapter.GSON.fromJson(
                "{\"events\":{\"click\":{\"actions\":[{\"id\":\"action-1\"}]}}}",
                NPCScriptData.class));
        assertThrows(JsonParseException.class, () -> EntityActionAdapter.GSON.fromJson(
                "{\"events\":{\"click\":{\"actions\":[{\"type\":\"unknown\"}]}}}",
                NPCScriptData.class));
    }

    @Test
    void invalidQuestEntriesAreSkipped() {
        String json = "{\"npcUUID\":\"" + TestSupport.uuid(3) + "\",\"quests\":["
                + "\"text\",5,null,{\"quest_uuid\":{}},"
                + "{\"local_id\":\"q\",\"quest_uuid\":\"" + QUEST_UUID + "\",\"type\":\"BOOLEAN\"}]}";

        NPCScriptData data = EntityActionAdapter.GSON.fromJson(json, NPCScriptData.class);

        assertNotNull(data);
        assertEquals(1, data.quests.size());
        assertEquals("q", data.quests.get(0).localId);
        assertEquals(QUEST_UUID, data.quests.get(0).questUuid);
    }

    @Test
    void serializeSkipsNullEventEntries() {
        NPCScriptData data = new NPCScriptData();
        data.events.put(null, new NpcClickEvent());
        data.events.put(EventType.CLICK.jsonKey(), null);

        JsonObject root = EntityActionAdapter.GSON.toJsonTree(data).getAsJsonObject();

        assertEquals(0, root.getAsJsonObject("events").size());
    }

    @Test
    void passportRoundTripsAllScalarFields() {
        NPCScriptData data = new NPCScriptData();
        data.npcUUID = TestSupport.uuid(4).toString();
        data.passport.npcName = "Full";
        data.passport.size = 1.5f;
        data.passport.speed = 0.4f;
        data.passport.hp = 42.5f;
        data.passport.holdX = 1.25;
        data.passport.holdY = 2.5;
        data.passport.holdZ = 3.75;
        data.passport.isVisible = false;
        data.passport.isHoldPosEnabled = true;
        data.passport.canTakeDamage = true;
        data.passport.useGravity = false;
        data.passport.creativeTunerOnly = true;
        data.passport.lookLocked = true;
        data.passport.emote = "dance";
        data.passport.skinType = "TEXTURE";
        data.passport.skinValue = "value";
        data.passport.skinModel = "slim";
        data.passport.skinHash = "hash";
        data.passport.dimension = "minecraft:overworld";
        data.passport.lastX = 10.0;
        data.passport.lastY = 64.0;
        data.passport.lastZ = -10.0;
        data.passport.hasLastPosition = true;

        NPCScriptData restored = EntityActionAdapter.GSON.fromJson(
                EntityActionAdapter.GSON.toJsonTree(data), NPCScriptData.class);

        assertNotNull(restored);
        assertEquals("Full", restored.passport.npcName);
        assertEquals(1.5f, restored.passport.size);
        assertEquals(0.4f, restored.passport.speed);
        assertEquals(42.5f, restored.passport.hp);
        assertEquals(1.25, restored.passport.holdX);
        assertEquals(2.5, restored.passport.holdY);
        assertEquals(3.75, restored.passport.holdZ);
        assertFalse(restored.passport.isVisible);
        assertTrue(restored.passport.isHoldPosEnabled);
        assertTrue(restored.passport.canTakeDamage);
        assertFalse(restored.passport.useGravity);
        assertTrue(restored.passport.creativeTunerOnly);
        assertTrue(restored.passport.lookLocked);
        assertEquals("dance", restored.passport.emote);
        assertEquals("TEXTURE", restored.passport.skinType);
        assertEquals("value", restored.passport.skinValue);
        assertEquals("slim", restored.passport.skinModel);
        assertEquals("hash", restored.passport.skinHash);
        assertEquals("minecraft:overworld", restored.passport.dimension);
        assertEquals(10.0, restored.passport.lastX);
        assertEquals(64.0, restored.passport.lastY);
        assertEquals(-10.0, restored.passport.lastZ);
        assertTrue(restored.passport.hasLastPosition);
    }

    private static ActionTrade tradeAction() {
        ActionTrade action = new ActionTrade("trade-1");
        action.baseSlots = 2;
        action.trades.add(new NPCTrade(
                new ItemStack(Items.STONE, 5),
                null,
                new ItemStack(Items.EMERALD, 1)));
        return action;
    }

    private static ActionDialog dialogAction() {
        ActionDialog dialog = new ActionDialog("dialog-1", "Merchant");
        dialog.npcMessage = Component.literal("Hello");
        dialog.addAnswer("Yes", "a1");
        return dialog;
    }

    private static QuestDefinition quest(String localId, String uuid) {
        QuestDefinition quest = new QuestDefinition();
        quest.localId = localId;
        quest.questUuid = uuid;
        quest.type = QuestType.BOOLEAN;
        quest.requiredCompletions = 1;
        return quest;
    }
}
