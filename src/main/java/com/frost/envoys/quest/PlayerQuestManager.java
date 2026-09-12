package com.frost.envoys.quest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.frost.envoys.init.ModAttachments;
import com.frost.envoys.network.payload.ClientQuestEntry;
import com.frost.envoys.network.payload.SyncPlayerQuestsPayload;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PlayerQuestManager {

    private PlayerQuestManager() {
    }

    private static PlayerQuestTracker tracker(Player player) {
        return player.getData(ModAttachments.PLAYER_QUESTS);
    }

    public static void give(Player player, String questUuid) {
        if (player == null || isBlank(questUuid)) {
            return;
        }
        PlayerQuestTracker tracker = tracker(player);
        
        if (!tracker.has(questUuid))
        {
            tracker.put(questUuid, new QuestProgress(QuestStatus.ACTIVE, List.of(), 0));
            syncIfServer(player);
        }
    }

    public static boolean isActive(Player player, String questUuid) {
        if (player == null || isBlank(questUuid) || !player.hasData(ModAttachments.PLAYER_QUESTS)) {
            return false;
        }
        QuestProgress progress = tracker(player).get(questUuid);
        return progress != null && progress.getStatus() == QuestStatus.ACTIVE;
    }

    public static boolean isCompleted(Player player, String questUuid) {
        if (player == null || isBlank(questUuid) || !player.hasData(ModAttachments.PLAYER_QUESTS)) {
            return false;
        }
        QuestProgress progress = tracker(player).get(questUuid);
        return progress != null && progress.getStatus() == QuestStatus.COMPLETED;
    }

    public static void advanceStep(Player player, QuestDefinition quest, String completionId) {
        if (player == null || quest == null || isBlank(quest.questUuid) || isBlank(completionId)) {
            return;
        }
        if (!isActive(player, quest.questUuid)) {
            return;
        }
        QuestProgress progress = tracker(player).get(quest.questUuid);
        if (progress == null) {
            return;
        }
        progress.getCompletedSteps().add(completionId);
        if (progress.getCompletedSteps().size() >= Math.max(1, quest.requiredCompletions)) {
            markCompleted(player, quest.questUuid);
        }
        syncIfServer(player);
    }

    public static void addKill(Player player, QuestDefinition quest) {
        if (player == null || quest == null || isBlank(quest.questUuid)) {
            return;
        }
        if (!isActive(player, quest.questUuid)) {
            return;
        }
        QuestProgress progress = tracker(player).get(quest.questUuid);
        if (progress == null) {
            return;
        }
        progress.setKillCount(progress.getKillCount() + 1);
        if (progress.getKillCount() >= Math.max(1, quest.killCount)) {
            markCompleted(player, quest.questUuid);
        }
        syncIfServer(player);
    }

    public static void markCompleted(Player player, String questUuid) {
        if (player == null || isBlank(questUuid)) {
            return;
        }
        tracker(player).put(questUuid, new QuestProgress(QuestStatus.COMPLETED, List.of(), 0));
        syncIfServer(player);
    }

    public static void reset(Player player, String questUuid) {
        if (player == null || isBlank(questUuid) || !player.hasData(ModAttachments.PLAYER_QUESTS)) {
            return;
        }
        tracker(player).remove(questUuid);
        syncIfServer(player);
    }

    public static int currentKillCount(Player player, String questUuid) {
        if (player == null || isBlank(questUuid) || !player.hasData(ModAttachments.PLAYER_QUESTS)) {
            return 0;
        }
        QuestProgress progress = tracker(player).get(questUuid);
        return progress == null ? 0 : progress.getKillCount();
    }

    public static int completedStepCount(Player player, String questUuid) {
        if (player == null || isBlank(questUuid) || !player.hasData(ModAttachments.PLAYER_QUESTS)) {
            return 0;
        }
        QuestProgress progress = tracker(player).get(questUuid);
        return progress == null ? 0 : progress.getCompletedSteps().size();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static void sync(ServerPlayer player) {
        if (player == null) {
            return;
        }
        PacketDistributor.sendToPlayer(player, new SyncPlayerQuestsPayload(buildEntries(player)));
    }

    private static List<ClientQuestEntry> buildEntries(ServerPlayer player) {
        if (!player.hasData(ModAttachments.PLAYER_QUESTS)) {
            return List.of();
        }

        Map<String, QuestDefinition> definitions = new HashMap<>();
        Map<String, String> npcNames = new HashMap<>();
        for (QuestIndex.Entry entry : QuestIndex.entries()) {
            QuestDefinition quest = entry.quest();
            if (quest == null || isBlank(quest.questUuid)) {
                continue;
            }
            definitions.putIfAbsent(quest.questUuid, quest);
            String name = (entry.manager() != null && entry.manager().passport != null)
                    ? entry.manager().passport.npcName
                    : "";
            if (name != null && !name.isBlank()) {
                npcNames.putIfAbsent(quest.questUuid, name);
            }
        }

        List<ClientQuestEntry> result = new ArrayList<>();
        for (Map.Entry<String, QuestProgress> progressEntry : player.getData(ModAttachments.PLAYER_QUESTS).asMap().entrySet()) {
            QuestProgress progress = progressEntry.getValue();
            if (progress == null || progress.getStatus() == QuestStatus.NOT_STARTED) {
                continue;
            }
            QuestDefinition definition = definitions.get(progressEntry.getKey());
            if (definition == null || definition.type == null) {
                continue;
            }

            int current = switch (definition.type) {
                case KILL -> progress.getKillCount();
                case BOOLEAN -> progress.getCompletedSteps().size();
                case ITEM -> 0;
            };
            int target = switch (definition.type) {
                case KILL -> Math.max(1, definition.killCount);
                case BOOLEAN -> Math.max(1, definition.requiredCompletions);
                case ITEM -> Math.max(1, definition.itemCount);
            };

            result.add(new ClientQuestEntry(
                definition.questUuid,
                definition.title == null ? "" : definition.title,
                npcNames.getOrDefault(definition.questUuid, ""),
                definition.visibleInGui,
                definition.type,
                definition.itemId == null ? "" : definition.itemId,
                target,
                current,
                progress.getStatus() == QuestStatus.COMPLETED
            ));
        }
        return result;
    }

    private static void syncIfServer(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            sync(serverPlayer);
        }
    }
}
