package com.frost.envoys.quest;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class QuestProgress {

    public static final Codec<QuestProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            QuestStatus.CODEC.optionalFieldOf("status", QuestStatus.NOT_STARTED).forGetter(QuestProgress::getStatus),
            Codec.STRING.listOf().optionalFieldOf("completed_steps", List.of())
                    .forGetter(progress -> List.copyOf(progress.getCompletedSteps())),
            Codec.INT.optionalFieldOf("kill_count", 0).forGetter(QuestProgress::getKillCount)
    ).apply(instance, QuestProgress::new));

    private QuestStatus status;
    private final Set<String> completedSteps;
    private int killCount;

    public QuestProgress() {
        this(QuestStatus.NOT_STARTED, List.of(), 0);
    }

    public QuestProgress(QuestStatus status, List<String> completedSteps, int killCount) {
        this.status = status == null ? QuestStatus.NOT_STARTED : status;
        this.completedSteps = new LinkedHashSet<>();
        if (completedSteps != null) {
            for (String step : completedSteps) {
                if (step != null && !step.isBlank()) {
                    this.completedSteps.add(step);
                }
            }
        }
        this.killCount = Math.max(0, killCount);
    }

    public QuestStatus getStatus() {
        return status;
    }

    public void setStatus(QuestStatus status) {
        this.status = status == null ? QuestStatus.NOT_STARTED : status;
    }

    public Set<String> getCompletedSteps() {
        return completedSteps;
    }

    public int getKillCount() {
        return killCount;
    }

    public void setKillCount(int killCount) {
        this.killCount = Math.max(0, killCount);
    }
}
