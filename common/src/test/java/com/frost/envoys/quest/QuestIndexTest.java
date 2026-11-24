package com.frost.envoys.quest;

import com.frost.envoys.TestSupport;
import com.frost.envoys.action.NPCInteractManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestIndexTest {

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
    void normalizeUuidRemovesDashesAndLowercases() {
        assertEquals("aaaaaaaabbbbccccddddeeeeeeeeeeee", QuestIndex.normalizeUuid(" " + UUID_A.toUpperCase() + " "));
        assertEquals("", QuestIndex.normalizeUuid(null));
    }

    @Test
    void resolvesByUuidExactNormalizedAndMissing() {
        NPCInteractManager manager = new NPCInteractManager(TestSupport.uuid(1));
        QuestDefinition quest = booleanQuest("a", UUID_A);
        manager.quests.add(quest);
        QuestIndex.invalidate();

        assertSame(quest, QuestIndex.byUuid(UUID_A).orElseThrow());
        assertSame(quest, QuestIndex.byUuid(UUID_A.replace("-", "")).orElseThrow());
        assertSame(quest, QuestIndex.byUuid(UUID_A.toUpperCase()).orElseThrow());
        assertTrue(QuestIndex.byUuid("").isEmpty());
        assertTrue(QuestIndex.byUuid(null).isEmpty());
        assertTrue(QuestIndex.byUuid("missing").isEmpty());
    }

    @Test
    void duplicateUuidIsRefused() {
        NPCInteractManager first = new NPCInteractManager(TestSupport.uuid(1));
        NPCInteractManager second = new NPCInteractManager(TestSupport.uuid(2));
        first.quests.add(booleanQuest("a", UUID_A));
        second.quests.add(booleanQuest("b", UUID_A));
        QuestIndex.invalidate();

        assertTrue(QuestIndex.byUuid(UUID_A).isEmpty());
        assertTrue(QuestIndex.byLocalId("a").isPresent());
    }

    @Test
    void duplicateLocalIdIsRefused() {
        NPCInteractManager first = new NPCInteractManager(TestSupport.uuid(1));
        NPCInteractManager second = new NPCInteractManager(TestSupport.uuid(2));
        first.quests.add(booleanQuest("a", UUID_A));
        second.quests.add(booleanQuest("a", UUID_B));
        QuestIndex.invalidate();

        assertTrue(QuestIndex.byLocalId("a").isEmpty());
        assertTrue(QuestIndex.byLocalId("b").isEmpty());
        assertTrue(QuestIndex.byLocalId(null).isEmpty());
    }

    @Test
    void resolvePrefersUuidThenLocalId() {
        NPCInteractManager manager = new NPCInteractManager(TestSupport.uuid(1));
        QuestDefinition byUuid = booleanQuest("a", UUID_A);
        QuestDefinition byLocal = booleanQuest("b", UUID_B);
        manager.quests.add(byUuid);
        manager.quests.add(byLocal);
        QuestIndex.invalidate();

        assertSame(byUuid, QuestIndex.resolve(UUID_A).orElseThrow());
        assertSame(byLocal, QuestIndex.resolve("b").orElseThrow());
        assertTrue(QuestIndex.resolve("missing").isEmpty());
        assertTrue(QuestIndex.resolve(null).isEmpty());
    }

    @Test
    void killCandidatesAreCaseInsensitiveAndDeduplicated() {
        NPCInteractManager first = new NPCInteractManager(TestSupport.uuid(1));
        QuestDefinition kill = killQuest("kill", UUID_B, "minecraft:zombie");
        first.quests.add(kill);
        QuestIndex.invalidate();

        assertEquals(List.of(kill), QuestIndex.killCandidates("MINECRAFT:ZOMBIE"));
        assertEquals(List.of(kill), QuestIndex.killCandidates("minecraft:zombie"));
        assertTrue(QuestIndex.killCandidates("").isEmpty());
        assertTrue(QuestIndex.killCandidates(null).isEmpty());
        assertTrue(QuestIndex.hasKillQuests());

        NPCInteractManager second = new NPCInteractManager(TestSupport.uuid(2));
        second.quests.add(killQuest("kill2", UUID_B, "minecraft:zombie"));
        QuestIndex.invalidate();

        assertEquals(1, QuestIndex.killCandidates("minecraft:zombie").size());
    }

    @Test
    void invalidateRebuildsSnapshot() {
        NPCInteractManager manager = new NPCInteractManager(TestSupport.uuid(1));
        QuestIndex.invalidate();
        assertTrue(QuestIndex.byLocalId("new").isEmpty());

        manager.quests.add(booleanQuest("new", UUID_C));
        assertTrue(QuestIndex.byLocalId("new").isEmpty());

        QuestIndex.invalidate();
        assertTrue(QuestIndex.byLocalId("new").isPresent());
    }

    @Test
    void hasNoKillQuestsInitially() {
        assertFalse(QuestIndex.hasKillQuests());
        assertTrue(QuestIndex.entries().isEmpty());
    }

    private static QuestDefinition booleanQuest(String localId, String uuid) {
        QuestDefinition quest = new QuestDefinition();
        quest.localId = localId;
        quest.questUuid = uuid;
        quest.type = QuestType.BOOLEAN;
        quest.requiredCompletions = 1;
        return quest;
    }

    private static QuestDefinition killQuest(String localId, String uuid, String entityId) {
        QuestDefinition quest = booleanQuest(localId, uuid);
        quest.type = QuestType.KILL;
        quest.entityId = entityId;
        quest.killCount = 1;
        return quest;
    }
}
