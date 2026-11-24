package com.frost.envoys.quest;

import com.frost.envoys.TestSupport;
import com.frost.envoys.action.NPCInteractManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestResolverTest {

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
    void localIdHasPriorityInsideCurrentNpc() {
        NPCInteractManager current = new NPCInteractManager(TestSupport.uuid(1));
        QuestDefinition uuidQuest = booleanQuest("uuidLocal", UUID_A);
        QuestDefinition localQuest = booleanQuest("local", UUID_B);
        current.quests.add(uuidQuest);
        current.quests.add(localQuest);
        QuestIndex.invalidate();

        assertSame(localQuest, QuestResolver.resolve(current, "local").orElseThrow());
    }

    @Test
    void uuidMatchesWithAndWithoutDashes() {
        NPCInteractManager current = new NPCInteractManager(TestSupport.uuid(1));
        QuestDefinition quest = booleanQuest("a", UUID_A);
        current.quests.add(quest);
        QuestIndex.invalidate();

        assertSame(quest, QuestResolver.resolve(current, UUID_A).orElseThrow());
        assertSame(quest, QuestResolver.resolve(current, UUID_A.replace("-", "")).orElseThrow());
        assertSame(quest, QuestResolver.resolve(current, UUID_A.toUpperCase()).orElseThrow());
    }

    @Test
    void fallsBackToGlobalIndex() {
        NPCInteractManager current = new NPCInteractManager(TestSupport.uuid(1));
        NPCInteractManager other = new NPCInteractManager(TestSupport.uuid(2));
        QuestDefinition global = booleanQuest("global", UUID_C);
        other.quests.add(global);
        QuestIndex.invalidate();

        assertSame(global, QuestResolver.resolve(current, "global").orElseThrow());
        assertSame(global, QuestResolver.resolve(current, UUID_C).orElseThrow());
        assertSame(global, QuestResolver.resolve(null, "global").orElseThrow());
    }

    @Test
    void blankOrNullTargetsAreEmpty() {
        NPCInteractManager current = new NPCInteractManager(TestSupport.uuid(1));
        assertTrue(QuestResolver.resolve(current, null).isEmpty());
        assertTrue(QuestResolver.resolve(current, "").isEmpty());
        assertTrue(QuestResolver.resolve(current, "   ").isEmpty());
    }

    @Test
    void missingTargetIsEmpty() {
        NPCInteractManager current = new NPCInteractManager(TestSupport.uuid(1));
        assertTrue(QuestResolver.resolve(current, "missing").isEmpty());
    }

    private static QuestDefinition booleanQuest(String localId, String uuid) {
        QuestDefinition quest = new QuestDefinition();
        quest.localId = localId;
        quest.questUuid = uuid;
        quest.type = QuestType.BOOLEAN;
        quest.requiredCompletions = 1;
        return quest;
    }
}
