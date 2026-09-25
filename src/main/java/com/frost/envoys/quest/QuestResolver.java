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

        String key = target.trim();
        String normalizedKey = QuestIndex.normalizeUuid(key);

        QuestDefinition uuidMatch = null;
        if (currentNpc != null && currentNpc.quests != null) {
            for (QuestDefinition quest : currentNpc.quests) {
                if (quest == null) {
                    continue;
                }
                if (quest.localId != null && key.equals(quest.localId.trim())) {
                    return Optional.of(quest);
                }
                if (uuidMatch == null && uuidEquals(quest.questUuid, key, normalizedKey)) {
                    uuidMatch = quest;
                }
            }
        }
        if (uuidMatch != null) {
            return Optional.of(uuidMatch);
        }

        // Fall back to the global index (other NPCs), matching by quest_uuid or local_id.
        return QuestIndex.resolve(key);
    }

    private static boolean uuidEquals(String questUuid, String key, String normalizedKey) {
        if (questUuid == null || questUuid.isBlank()) {
            return false;
        }
        String trimmed = questUuid.trim();
        return trimmed.equals(key) || QuestIndex.normalizeUuid(trimmed).equals(normalizedKey);
    }
}
