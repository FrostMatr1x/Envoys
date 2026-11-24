package com.frost.envoys.quest;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerQuestTrackerTest {

    @Test
    void nullKeysAreHandledSafely() {
        PlayerQuestTracker tracker = new PlayerQuestTracker();
        assertNull(tracker.get(null));
        assertFalse(tracker.has(null));
        tracker.put(null, new QuestProgress());
        tracker.remove(null);
        assertTrue(tracker.isEmpty());
    }

    @Test
    void getOrCreateCachesInstance() {
        PlayerQuestTracker tracker = new PlayerQuestTracker();
        QuestProgress first = tracker.getOrCreate("q");
        QuestProgress second = tracker.getOrCreate("q");
        assertSame(first, second);
        assertTrue(tracker.has("q"));
        assertFalse(tracker.isEmpty());
    }

    @Test
    void putRemoveAndAsMap() {
        PlayerQuestTracker tracker = new PlayerQuestTracker();
        QuestProgress progress = new QuestProgress(QuestStatus.ACTIVE, List.of("s"), 1);
        tracker.put("q", progress);
        assertSame(progress, tracker.get("q"));
        assertEquals(Map.of("q", progress), tracker.asMap());

        tracker.remove("q");
        assertFalse(tracker.has("q"));
        assertTrue(tracker.isEmpty());
    }

    @Test
    void constructorCopiesEntries() {
        PlayerQuestTracker source = new PlayerQuestTracker();
        source.put("q", new QuestProgress(QuestStatus.ACTIVE, List.of(), 0));

        PlayerQuestTracker copy = new PlayerQuestTracker(source.asMap());
        source.remove("q");

        assertTrue(copy.has("q"));
        assertFalse(source.has("q"));
    }

    @Test
    void codecRoundTrip() {
        PlayerQuestTracker tracker = new PlayerQuestTracker();
        tracker.put("q", new QuestProgress(QuestStatus.ACTIVE, List.of("a"), 2));

        JsonElement encoded = PlayerQuestTracker.CODEC.encodeStart(JsonOps.INSTANCE, tracker).getOrThrow();
        PlayerQuestTracker decoded = PlayerQuestTracker.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();

        assertEquals(QuestStatus.ACTIVE, decoded.get("q").getStatus());
        assertEquals(List.of("a"), List.copyOf(decoded.get("q").getCompletedSteps()));
        assertEquals(2, decoded.get("q").getKillCount());
    }
}
