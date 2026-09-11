package com.frost.envoys.quest;

import java.util.Optional;

import com.frost.envoys.action.NPCInteractManager;

public final class QuestResolver {

    private QuestResolver() {
    }

    public static Optional<QuestDefinition> resolve(NPCInteractManager currentNpc, String target) {
        if (target == null || target.isBlank()) {
            return Optional.empty();
        }

        QuestDefinition uuidMatch = null;
        if (currentNpc != null && currentNpc.quests != null) {
            for (QuestDefinition quest : currentNpc.quests) {
                if (quest == null) {
                    continue;
                }
                if (target.equals(quest.localId)) {
                    return Optional.of(quest);
                }
                if (uuidMatch == null && target.equals(quest.questUuid)) {
                    uuidMatch = quest;
                }
            }
        }
        if (uuidMatch != null) {
            return Optional.of(uuidMatch);
        }

        return QuestIndex.byUuid(target);
    }
}
