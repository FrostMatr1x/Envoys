package com.frost.envoys.action;

import com.frost.envoys.TestSupport;
import com.frost.envoys.action.event.EventType;
import com.frost.envoys.action.event.NpcClickEvent;
import com.frost.envoys.action.event.NpcEventData;
import com.frost.envoys.action.event.NpcKickEvent;
import com.frost.envoys.action.model.ActionStart;
import com.frost.envoys.quest.QuestDefinition;
import com.frost.envoys.quest.QuestType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NPCScriptDataTest {

    private static final String UUID_A = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final String UUID_B = "11111111-2222-3333-4444-555555555555";
    private static final String UUID_C = "99999999-8888-7777-6666-555555555555";

    @BeforeEach
    void setUp() {
        TestSupport.resetQuestState();
    }

    @AfterEach
    void tearDown() {
        TestSupport.resetQuestState();
    }

    @Test
    void nullInputYieldsEmptyList() {
        assertTrue(NPCScriptData.sanitizeQuests(null, null).isEmpty());
    }

    @Test
    void sanitizeSkipsInvalidDuplicatesAndNulls() {
        NPCInteractManager owner = new NPCInteractManager(TestSupport.uuid(1));
        List<QuestDefinition> input = new ArrayList<>();
        input.add(null);
        input.add(new QuestDefinition());
        QuestDefinition first = quest("a", UUID_A);
        input.add(first);
        input.add(quest("a", UUID_B));
        input.add(quest("b", UUID_A));

        assertEquals(List.of(first), NPCScriptData.sanitizeQuests(input, owner));
    }

    @Test
    void sanitizeRejectsUuidOwnedByAnotherNpc() {
        NPCInteractManager owner = new NPCInteractManager(TestSupport.uuid(1));
        NPCInteractManager other = new NPCInteractManager(TestSupport.uuid(2));
        other.quests.add(quest("other", UUID_A));

        assertTrue(NPCScriptData.sanitizeQuests(List.of(quest("candidate", UUID_A)), owner).isEmpty());
    }

    @Test
    void sanitizeAllowsSameNpcUuidOnDifferentManager() {
        NPCInteractManager owner = new NPCInteractManager(TestSupport.uuid(1));
        NPCInteractManager sameNpc = new NPCInteractManager(TestSupport.uuid(1));
        sameNpc.quests.add(quest("existing", UUID_A));
        QuestDefinition candidate = quest("candidate", UUID_A);

        assertEquals(List.of(candidate), NPCScriptData.sanitizeQuests(List.of(candidate), owner));
    }

    @Test
    void materializeCreatesFourDisabledEvents() {
        NPCScriptData data = new NPCScriptData();
        data.materialize();

        assertEquals(EventType.values().length, data.events.size());
        for (EventType type : EventType.values()) {
            NpcEventData event = data.events.get(type.jsonKey());
            assertNotNull(event);
            assertFalse(event.enabled());
        }

        int size = data.events.size();
        data.materialize();
        assertEquals(size, data.events.size());
    }

    @Test
    void fromManagerAndApplyToRoundTrip() {
        NPCInteractManager manager = new NPCInteractManager(TestSupport.uuid(1));
        NpcClickEvent click = new NpcClickEvent();
        click.actions().add(new ActionStart("a"));
        manager.putEvent(EventType.CLICK, click);
        manager.quests.add(quest("a", UUID_A));
        manager.passport.npcName = "Bob";

        NPCScriptData data = NPCScriptData.fromManager(manager);
        assertEquals(manager.npcUUID.toString(), data.npcUUID);
        assertEquals(EventType.values().length, data.events.size());
        assertEquals(1, data.quests.size());

        NPCInteractManager.unregister(manager.npcUUID);
        NPCInteractManager restored = new NPCInteractManager(TestSupport.uuid(2));
        restored.quests.add(quest("old", UUID_B));
        NpcKickEvent oldKick = new NpcKickEvent();
        oldKick.actions().add(new ActionStart("old"));
        restored.putEvent(EventType.KICK, oldKick);

        data.applyTo(restored);

        assertEquals(1, restored.quests.size());
        assertEquals("a", restored.quests.get(0).localId);
        assertTrue(restored.events.containsKey(EventType.CLICK));
        assertTrue(restored.events.containsKey(EventType.KICK));
        assertTrue(restored.events.get(EventType.KICK).actions().isEmpty());
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
