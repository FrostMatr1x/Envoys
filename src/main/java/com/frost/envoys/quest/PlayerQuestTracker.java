package com.frost.envoys.quest;

import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;

public class PlayerQuestTracker {

    public static final Codec<PlayerQuestTracker> CODEC =
            Codec.unboundedMap(Codec.STRING, QuestProgress.CODEC)
                    .xmap(PlayerQuestTracker::new, PlayerQuestTracker::asMap);

    private final Map<String, QuestProgress> quests;

    public PlayerQuestTracker() {
        this.quests = new HashMap<>();
    }

    public PlayerQuestTracker(Map<String, QuestProgress> quests) {
        this.quests = new HashMap<>();
        if (quests != null) {
            this.quests.putAll(quests);
        }
    }

    public QuestProgress get(String uuid) {
        return uuid == null ? null : quests.get(uuid);
    }

    public QuestProgress getOrCreate(String uuid) {
        return quests.computeIfAbsent(uuid, key -> new QuestProgress());
    }

    public void put(String uuid, QuestProgress progress) {
        if (uuid == null || progress == null) {
            return;
        }
        quests.put(uuid, progress);
    }

    public void remove(String uuid) {
        if (uuid != null) {
            quests.remove(uuid);
        }
    }

    public boolean has(String uuid) {
        return uuid != null && quests.containsKey(uuid);
    }

    public boolean isEmpty() {
        return quests.isEmpty();
    }

    public Map<String, QuestProgress> asMap() {
        return quests;
    }
}
