package com.frost.envoys.quest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.entity.player.Player;

public final class PlayerQuestTags {

    private static final String PREFIX = "quest_";
    private static final String ACTIVE_SUFFIX = "_active";
    private static final String STEP_SUFFIX = "_step_";
    private static final String KILL_SUFFIX = "_kill_";
    private static final String COMPLETED_SUFFIX = "_completed";

    private PlayerQuestTags() {
    }

    private static String base(String questUuid) {
        return PREFIX + (questUuid == null ? "" : questUuid);
    }

    public static String activeTag(String questUuid) {
        return base(questUuid) + ACTIVE_SUFFIX;
    }

    public static String stepPrefix(String questUuid) {
        return base(questUuid) + STEP_SUFFIX;
    }

    public static String killPrefix(String questUuid) {
        return base(questUuid) + KILL_SUFFIX;
    }

    public static String completedTag(String questUuid) {
        return base(questUuid) + COMPLETED_SUFFIX;
    }

    public static void give(Player player, QuestDefinition quest) {
        if (player == null || quest == null || isBlank(quest.questUuid)) {
            return;
        }
        player.addTag(activeTag(quest.questUuid));
    }

    public static boolean hasActive(Player player, String questUuid) {
        return player != null && !isBlank(questUuid) && player.getTags().contains(activeTag(questUuid));
    }

    public static boolean hasCompleted(Player player, String questUuid) {
        return player != null && !isBlank(questUuid) && player.getTags().contains(completedTag(questUuid));
    }

    public static void advanceStep(Player player, QuestDefinition quest, String completionId) {
        if (player == null || quest == null || isBlank(quest.questUuid)) {
            return;
        }
        if (hasCompleted(player, quest.questUuid)) {
            return;
        }
        String tag = stepPrefix(quest.questUuid) + (completionId == null ? "" : completionId);
        player.addTag(tag);
        if (countStepTags(player, quest.questUuid) >= quest.requiredCompletions) {
            clearStepTags(player, quest.questUuid);
            player.addTag(completedTag(quest.questUuid));
        }
    }

    public static void addKill(Player player, QuestDefinition quest) {
        if (player == null || quest == null || isBlank(quest.questUuid)) {
            return;
        }
        if (hasCompleted(player, quest.questUuid)) {
            return;
        }
        int n = countKillTags(player, quest.questUuid);
        player.addTag(killPrefix(quest.questUuid) + n);
        if (n + 1 >= quest.killCount) {
            clearKillTags(player, quest.questUuid);
            player.addTag(completedTag(quest.questUuid));
        }
    }

    public static void markCompleted(Player player, QuestDefinition quest) {
        if (player == null || quest == null || isBlank(quest.questUuid)) {
            return;
        }
        clearStepTags(player, quest.questUuid);
        clearKillTags(player, quest.questUuid);
        player.addTag(completedTag(quest.questUuid));
    }

    public static int countStepTags(Player player, String questUuid) {
        return countByPrefix(player, stepPrefix(questUuid));
    }

    public static int countKillTags(Player player, String questUuid) {
        return countByPrefix(player, killPrefix(questUuid));
    }

    public static void clearStepTags(Player player, String questUuid) {
        removeByPrefix(player, stepPrefix(questUuid));
    }

    public static void clearKillTags(Player player, String questUuid) {
        removeByPrefix(player, killPrefix(questUuid));
    }

    public static void removeCompleted(Player player, String questUuid) {
        if (player == null || isBlank(questUuid)) {
            return;
        }
        player.removeTag(completedTag(questUuid));
    }

    public static void removeActive(Player player, String questUuid) {
        if (player == null || isBlank(questUuid)) {
            return;
        }
        player.removeTag(activeTag(questUuid));
    }

    private static int countByPrefix(Player player, String prefix) {
        if (player == null || prefix == null) {
            return 0;
        }
        int count = 0;
        for (String tag : player.getTags()) {
            if (tag != null && tag.startsWith(prefix)) {
                count++;
            }
        }
        return count;
    }

    private static void removeByPrefix(Player player, String prefix) {
        if (player == null || prefix == null) {
            return;
        }
        List<String> toRemove = new ArrayList<>();
        for (String tag : player.getTags()) {
            if (tag != null && tag.startsWith(prefix)) {
                toRemove.add(tag);
            }
        }
        for (String tag : toRemove) {
            player.removeTag(tag);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
