package com.frost.envoys.quest;

import com.frost.envoys.TestSupport;
import com.frost.envoys.network.NetworkTestSupport;
import com.frost.envoys.platform.EnvoysPlatform;
import com.frost.envoys.platform.TestAttachments;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlayerQuestManagerTest {

    private static final String QUEST = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

    @TempDir
    Path tempDir;

    private TestAttachments attachments;
    private Player player;

    @BeforeAll
    static void bootstrap() {
        NetworkTestSupport.bootStrap();
    }

    @BeforeEach
    void setUp() {
        attachments = new TestAttachments();
        TestSupport.installEnvironment(tempDir, attachments);
        TestSupport.resetQuestState();
        player = mock(Player.class);
        when(player.getUUID()).thenReturn(UUID.fromString("11111111-2222-3333-4444-555555555555"));
    }

    @AfterEach
    void tearDown() {
        TestSupport.resetQuestState();
        TestSupport.clearEnvironment();
    }

    @Test
    void giveIgnoresNullPlayerAndBlankUuid() {
        PlayerQuestManager.give(null, QUEST);
        PlayerQuestManager.give(player, null);
        PlayerQuestManager.give(player, " ");

        assertFalse(EnvoysPlatform.hasQuests(player));
        assertEquals(QuestStatus.NOT_STARTED, PlayerQuestManager.status(player, QUEST));
    }

    @Test
    void giveCreatesActiveQuestWithEmptyProgress() {
        PlayerQuestManager.give(player, QUEST);

        assertEquals(QuestStatus.ACTIVE, PlayerQuestManager.status(player, QUEST));
        assertTrue(PlayerQuestManager.completedSteps(player, QUEST).isEmpty());
        assertEquals(0, PlayerQuestManager.currentKillCount(player, QUEST));
    }

    @Test
    void repeatedGiveWhileActiveKeepsProgress() {
        PlayerQuestManager.give(player, QUEST);
        PlayerQuestManager.advanceStep(player, quest(5, 1), "step-1");
        PlayerQuestManager.addKill(player, quest(5, 5), 2);

        PlayerQuestManager.give(player, QUEST);

        assertEquals(QuestStatus.ACTIVE, PlayerQuestManager.status(player, QUEST));
        assertEquals(List.of("step-1"), PlayerQuestManager.completedSteps(player, QUEST));
        assertEquals(2, PlayerQuestManager.currentKillCount(player, QUEST));
    }

    @Test
    void giveAfterCompletionReactivatesWithCleanProgress() {
        PlayerQuestManager.give(player, QUEST);
        PlayerQuestManager.advanceStep(player, quest(1, 1), "step-1");
        assertEquals(QuestStatus.COMPLETED, PlayerQuestManager.status(player, QUEST));

        PlayerQuestManager.give(player, QUEST);

        assertEquals(QuestStatus.ACTIVE, PlayerQuestManager.status(player, QUEST));
        assertTrue(PlayerQuestManager.completedSteps(player, QUEST).isEmpty());
        assertEquals(0, PlayerQuestManager.currentKillCount(player, QUEST));
    }

    @Test
    void giveOverwritesNotStartedEntry() {
        EnvoysPlatform.quests(player).getOrCreate(QUEST).setKillCount(4);

        PlayerQuestManager.give(player, QUEST);

        assertEquals(QuestStatus.ACTIVE, PlayerQuestManager.status(player, QUEST));
        assertEquals(0, PlayerQuestManager.currentKillCount(player, QUEST));
    }

    @Test
    void advanceStepIgnoresInvalidInputs() {
        PlayerQuestManager.give(player, QUEST);

        PlayerQuestManager.advanceStep(null, quest(5, 1), "step");
        PlayerQuestManager.advanceStep(player, null, "step");
        PlayerQuestManager.advanceStep(player, quest(5, 1), null);
        PlayerQuestManager.advanceStep(player, quest(5, 1), " ");
        PlayerQuestManager.advanceStep(player, questDefinition("", 5, 1), "step");

        assertTrue(PlayerQuestManager.completedSteps(player, QUEST).isEmpty());
    }

    @Test
    void advanceStepIgnoresUnknownAndInactiveQuests() {
        PlayerQuestManager.advanceStep(player, quest(5, 1), "step");
        assertFalse(EnvoysPlatform.hasQuests(player));

        PlayerQuestManager.give(player, QUEST);
        PlayerQuestManager.markCompleted(player, QUEST);
        PlayerQuestManager.advanceStep(player, quest(5, 1), "step");

        assertEquals(QuestStatus.COMPLETED, PlayerQuestManager.status(player, QUEST));
        assertTrue(PlayerQuestManager.completedSteps(player, QUEST).isEmpty());
    }

    @Test
    void advanceStepAddsUniqueCompletionIds() {
        PlayerQuestManager.give(player, QUEST);

        PlayerQuestManager.advanceStep(player, quest(5, 1), "step-1");
        PlayerQuestManager.advanceStep(player, quest(5, 1), "step-1");
        PlayerQuestManager.advanceStep(player, quest(5, 1), "step-2");

        assertEquals(List.of("step-1", "step-2"), PlayerQuestManager.completedSteps(player, QUEST));
        assertEquals(2, PlayerQuestManager.completedStepCount(player, QUEST));
    }

    @Test
    void advanceStepCompletesAtRequiredCompletions() {
        PlayerQuestManager.give(player, QUEST);

        PlayerQuestManager.advanceStep(player, quest(2, 1), "step-1");
        assertEquals(QuestStatus.ACTIVE, PlayerQuestManager.status(player, QUEST));

        PlayerQuestManager.advanceStep(player, quest(2, 1), "step-2");
        assertEquals(QuestStatus.COMPLETED, PlayerQuestManager.status(player, QUEST));
    }

    @Test
    void advanceStepTreatsNonPositiveRequiredCompletionsAsOne() {
        PlayerQuestManager.give(player, QUEST);

        PlayerQuestManager.advanceStep(player, quest(0, 1), "step-1");

        assertEquals(QuestStatus.COMPLETED, PlayerQuestManager.status(player, QUEST));
    }

    @Test
    void addKillIgnoresInvalidInputs() {
        PlayerQuestManager.give(player, QUEST);

        PlayerQuestManager.addKill(null, quest(5, 5), 1);
        PlayerQuestManager.addKill(player, null, 1);
        PlayerQuestManager.addKill(player, quest(5, 5), 0);
        PlayerQuestManager.addKill(player, quest(5, 5), -2);
        PlayerQuestManager.addKill(player, questDefinition("", 5, 5), 1);

        assertEquals(0, PlayerQuestManager.currentKillCount(player, QUEST));
    }

    @Test
    void addKillIgnoresInactiveQuest() {
        PlayerQuestManager.addKill(player, quest(5, 5), 1);

        assertFalse(EnvoysPlatform.hasQuests(player));
    }

    @Test
    void addKillAccumulatesAndCompletes() {
        PlayerQuestManager.give(player, QUEST);

        PlayerQuestManager.addKill(player, quest(5, 5), 2);
        assertEquals(2, PlayerQuestManager.currentKillCount(player, QUEST));
        assertEquals(QuestStatus.ACTIVE, PlayerQuestManager.status(player, QUEST));

        PlayerQuestManager.addKill(player, quest(5, 5), 4);
        assertEquals(QuestStatus.COMPLETED, PlayerQuestManager.status(player, QUEST));
    }

    @Test
    void addKillAfterCompletionDoesNotChangeState() {
        PlayerQuestManager.give(player, QUEST);
        PlayerQuestManager.addKill(player, quest(5, 1), 1);
        assertEquals(QuestStatus.COMPLETED, PlayerQuestManager.status(player, QUEST));

        PlayerQuestManager.addKill(player, quest(5, 1), 3);

        assertEquals(QuestStatus.COMPLETED, PlayerQuestManager.status(player, QUEST));
        assertEquals(0, PlayerQuestManager.currentKillCount(player, QUEST));
    }

    @Test
    void markCompletedIgnoresInvalidInputs() {
        PlayerQuestManager.give(player, QUEST);
        PlayerQuestManager.advanceStep(player, quest(5, 5), "step-1");

        PlayerQuestManager.markCompleted(null, QUEST);
        PlayerQuestManager.markCompleted(player, null);
        PlayerQuestManager.markCompleted(player, " ");

        assertEquals(QuestStatus.ACTIVE, PlayerQuestManager.status(player, QUEST));
        assertEquals(List.of("step-1"), PlayerQuestManager.completedSteps(player, QUEST));
    }

    @Test
    void markCompletedResetsProgress() {
        PlayerQuestManager.give(player, QUEST);
        PlayerQuestManager.advanceStep(player, quest(5, 5), "step-1");
        PlayerQuestManager.addKill(player, quest(5, 5), 2);

        PlayerQuestManager.markCompleted(player, QUEST);

        assertEquals(QuestStatus.COMPLETED, PlayerQuestManager.status(player, QUEST));
        assertTrue(PlayerQuestManager.completedSteps(player, QUEST).isEmpty());
        assertEquals(0, PlayerQuestManager.currentKillCount(player, QUEST));
    }

    @Test
    void statusReturnsNotStartedForMissingState() {
        assertEquals(QuestStatus.NOT_STARTED, PlayerQuestManager.status(null, QUEST));
        assertEquals(QuestStatus.NOT_STARTED, PlayerQuestManager.status(player, null));
        assertEquals(QuestStatus.NOT_STARTED, PlayerQuestManager.status(player, " "));
        assertEquals(QuestStatus.NOT_STARTED, PlayerQuestManager.status(player, QUEST));
        assertEquals(QuestStatus.NOT_STARTED, PlayerQuestManager.status(player, "unknown"));
    }

    @Test
    void completedStepsReturnsImmutableCopy() {
        PlayerQuestManager.give(player, QUEST);
        PlayerQuestManager.advanceStep(player, quest(5, 1), "step-1");

        List<String> steps = PlayerQuestManager.completedSteps(player, QUEST);

        assertEquals(List.of("step-1"), steps);
        assertThrows(UnsupportedOperationException.class, () -> steps.add("step-2"));
        assertEquals(1, PlayerQuestManager.completedStepCount(player, QUEST));
    }

    @Test
    void completedStepsReturnsEmptyForMissingTracker() {
        assertTrue(PlayerQuestManager.completedSteps(player, QUEST).isEmpty());
        assertTrue(PlayerQuestManager.completedSteps(null, QUEST).isEmpty());
        assertTrue(PlayerQuestManager.completedSteps(player, null).isEmpty());
        assertTrue(PlayerQuestManager.completedSteps(player, " ").isEmpty());
    }

    @Test
    void countersReturnZeroForMissingState() {
        assertEquals(0, PlayerQuestManager.currentKillCount(null, QUEST));
        assertEquals(0, PlayerQuestManager.currentKillCount(player, null));
        assertEquals(0, PlayerQuestManager.currentKillCount(player, " "));
        assertEquals(0, PlayerQuestManager.currentKillCount(player, QUEST));
        assertEquals(0, PlayerQuestManager.completedStepCount(null, QUEST));
        assertEquals(0, PlayerQuestManager.completedStepCount(player, null));
        assertEquals(0, PlayerQuestManager.completedStepCount(player, " "));
        assertEquals(0, PlayerQuestManager.completedStepCount(player, QUEST));
    }

    @Test
    void isActiveAndIsCompletedFollowStatus() {
        assertFalse(PlayerQuestManager.isActive(null, QUEST));
        assertFalse(PlayerQuestManager.isActive(player, null));
        assertFalse(PlayerQuestManager.isCompleted(null, QUEST));
        assertFalse(PlayerQuestManager.isCompleted(player, null));

        PlayerQuestManager.give(player, QUEST);
        assertTrue(PlayerQuestManager.isActive(player, QUEST));
        assertFalse(PlayerQuestManager.isCompleted(player, QUEST));

        PlayerQuestManager.markCompleted(player, QUEST);
        assertFalse(PlayerQuestManager.isActive(player, QUEST));
        assertTrue(PlayerQuestManager.isCompleted(player, QUEST));
    }

    @Test
    void resetRemovesProgressAndIsIdempotent() {
        PlayerQuestManager.give(player, QUEST);
        PlayerQuestManager.advanceStep(player, quest(5, 1), "step-1");

        PlayerQuestManager.reset(player, QUEST);
        assertEquals(QuestStatus.NOT_STARTED, PlayerQuestManager.status(player, QUEST));

        assertDoesNotThrow(() -> PlayerQuestManager.reset(player, QUEST));
        assertDoesNotThrow(() -> PlayerQuestManager.reset(player, null));
        assertDoesNotThrow(() -> PlayerQuestManager.reset(null, QUEST));
    }

    @Test
    void resetWithoutTrackerDoesNotThrow() {
        assertDoesNotThrow(() -> PlayerQuestManager.reset(player, QUEST));
    }

    private static QuestDefinition quest(int requiredCompletions, int killCount) {
        return questDefinition(QUEST, requiredCompletions, killCount);
    }

    private static QuestDefinition questDefinition(String uuid, int requiredCompletions, int killCount) {
        QuestDefinition quest = new QuestDefinition();
        quest.questUuid = uuid;
        quest.localId = "local";
        quest.type = QuestType.BOOLEAN;
        quest.requiredCompletions = requiredCompletions;
        quest.killCount = killCount;
        return quest;
    }
}
