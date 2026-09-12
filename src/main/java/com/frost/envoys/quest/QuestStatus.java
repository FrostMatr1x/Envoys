package com.frost.envoys.quest;

import com.mojang.serialization.Codec;

public enum QuestStatus {
    NOT_STARTED,
    ACTIVE,
    COMPLETED;

    public static final Codec<QuestStatus> CODEC = Codec.STRING.xmap(QuestStatus::fromName, QuestStatus::name);

    public static QuestStatus fromName(String name) {
        if (name != null) {
            for (QuestStatus status : values()) {
                if (status.name().equals(name)) {
                    return status;
                }
            }
        }
        return NOT_STARTED;
    }
}
