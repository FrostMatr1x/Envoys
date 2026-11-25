package com.frost.envoys.quest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestProgressTest {

    @Test
    void constructorNormalizesValues() {
        QuestProgress progress = new QuestProgress(null, Arrays.asList("a", null, " ", "b"), -5);

        assertEquals(QuestStatus.NOT_STARTED, progress.getStatus());
        assertEquals(List.of("a", "b"), List.copyOf(progress.getCompletedSteps()));
        assertEquals(0, progress.getKillCount());
    }

    @Test
    void settersNormalizeValues() {
        QuestProgress progress = new QuestProgress();
        progress.setStatus(null);
        assertEquals(QuestStatus.NOT_STARTED, progress.getStatus());

        progress.setStatus(QuestStatus.ACTIVE);
        assertEquals(QuestStatus.ACTIVE, progress.getStatus());

        progress.setKillCount(-5);
        assertEquals(0, progress.getKillCount());
        progress.setKillCount(7);
        assertEquals(7, progress.getKillCount());
    }

    @Test
    void codecRoundTrip() {
        QuestProgress progress = new QuestProgress(QuestStatus.ACTIVE, List.of("step1", "step2"), 3);

        JsonElement encoded = QuestProgress.CODEC.encodeStart(JsonOps.INSTANCE, progress).getOrThrow();
        QuestProgress decoded = QuestProgress.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();

        assertEquals(QuestStatus.ACTIVE, decoded.getStatus());
        assertEquals(List.of("step1", "step2"), List.copyOf(decoded.getCompletedSteps()));
        assertEquals(3, decoded.getKillCount());
    }

    @Test
    void codecDefaultsForEmptyObject() {
        QuestProgress decoded = QuestProgress.CODEC.parse(JsonOps.INSTANCE, new JsonObject()).getOrThrow();
        assertEquals(QuestStatus.NOT_STARTED, decoded.getStatus());
        assertTrue(decoded.getCompletedSteps().isEmpty());
        assertEquals(0, decoded.getKillCount());
    }

    @Test
    void statusNamesAndCodec() {
        assertEquals(QuestStatus.NOT_STARTED, QuestStatus.fromName(null));
        assertEquals(QuestStatus.NOT_STARTED, QuestStatus.fromName("unknown"));
        assertEquals(QuestStatus.NOT_STARTED, QuestStatus.fromName("active"));
        assertEquals(QuestStatus.ACTIVE, QuestStatus.fromName("ACTIVE"));
        assertEquals(QuestStatus.COMPLETED, QuestStatus.fromName("COMPLETED"));

        JsonElement encoded = QuestStatus.CODEC.encodeStart(JsonOps.INSTANCE, QuestStatus.COMPLETED).getOrThrow();
        assertEquals(QuestStatus.COMPLETED, QuestStatus.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
    }
}
