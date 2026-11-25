package com.frost.envoys.quest;

import com.frost.envoys.TestSupport;
import com.frost.envoys.action.NPCInteractManager;
import com.frost.envoys.network.Network;
import com.frost.envoys.network.NetworkTestSupport;
import com.frost.envoys.platform.TestAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuestKillHandlerTest {

    private static final String QUEST_UUID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

    @TempDir
    Path tempDir;

    private ServerPlayer player;
    private LivingEntity dead;
    private ServerLevel deadLevel;
    private DamageSource source;
    private NPCInteractManager manager;

    @BeforeAll
    static void bootstrap() {
        NetworkTestSupport.bootStrap();
    }

    @BeforeEach
    void setUp() {
        TestSupport.installEnvironment(tempDir, new TestAttachments());
        TestSupport.resetQuestState();
        Network.init(new NetworkTestSupport.RecordingNetworkManager());

        player = NetworkTestSupport.serverPlayer(UUID.fromString("11111111-2222-3333-4444-555555555555"), false);

        deadLevel = mock(ServerLevel.class);
        when(deadLevel.isClientSide()).thenReturn(false);
        dead = mock(LivingEntity.class);
        when(dead.level()).thenReturn(deadLevel);
        doReturn(EntityType.ZOMBIE).when(dead).getType();

        source = mock(DamageSource.class);
        when(source.getEntity()).thenReturn(player);

        manager = new NPCInteractManager(TestSupport.uuid(1));
        manager.quests.add(killQuest());
        QuestIndex.invalidate();
        PlayerQuestManager.give(player, QUEST_UUID);
    }

    @AfterEach
    void tearDown() {
        Network.init(null);
        TestSupport.resetQuestState();
        TestSupport.clearEnvironment();
    }

    @Test
    void matchingKillAdvancesAndCompletes() {
        QuestKillHandler.handle(dead, source);

        assertTrue(PlayerQuestManager.isCompleted(player, QUEST_UUID));
    }

    @Test
    void nonServerKillerIsIgnored() {
        when(source.getEntity()).thenReturn(mock(Player.class));

        QuestKillHandler.handle(dead, source);

        assertTrue(PlayerQuestManager.isActive(player, QUEST_UUID));
        assertEquals(0, PlayerQuestManager.currentKillCount(player, QUEST_UUID));
    }

    @Test
    void clientSideDeathIsIgnored() {
        when(deadLevel.isClientSide()).thenReturn(true);

        QuestKillHandler.handle(dead, source);

        assertTrue(PlayerQuestManager.isActive(player, QUEST_UUID));
        assertEquals(0, PlayerQuestManager.currentKillCount(player, QUEST_UUID));
    }

    @Test
    void noKillQuestsIsNoOp() {
        TestSupport.resetQuestState();

        QuestKillHandler.handle(dead, source);

        assertTrue(PlayerQuestManager.isActive(player, QUEST_UUID));
        assertEquals(0, PlayerQuestManager.currentKillCount(player, QUEST_UUID));
    }

    private static QuestDefinition killQuest() {
        QuestDefinition quest = new QuestDefinition();
        quest.localId = "kill-zombie";
        quest.questUuid = QUEST_UUID;
        quest.type = QuestType.KILL;
        quest.entityId = "minecraft:zombie";
        quest.killCount = 1;
        return quest;
    }
}
