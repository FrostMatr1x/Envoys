package com.frost.envoys.quest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.frost.envoys.Envoys;
import com.frost.envoys.action.NPCInteractManager;

public final class QuestIndex {

    public record Entry(NPCInteractManager manager, QuestDefinition quest) {
    }

    private record Snapshot(
            List<Entry> entries,
            Map<String, List<QuestDefinition>> byUuid,
            Map<String, List<QuestDefinition>> byNormalizedUuid,
            Map<String, List<QuestDefinition>> byLocalId,
            Map<String, List<QuestDefinition>> byKillEntity) {
    }

    private static volatile Snapshot snapshot;

    private QuestIndex() {
    }

    public static void invalidate() {
        snapshot = null;
    }

    public static List<Entry> entries() {
        return snapshot().entries();
    }

    public static String normalizeUuid(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replace("-", "").toLowerCase(Locale.ROOT);
    }

    public static Optional<QuestDefinition> byUuid(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return Optional.empty();
        }
        String key = uuid.trim();
        Optional<QuestDefinition> exact = unique(snapshot().byUuid().get(key), "quest_uuid", key);
        if (exact.isPresent()) {
            return exact;
        }
        String normalized = normalizeUuid(key);
        return unique(snapshot().byNormalizedUuid().get(normalized), "quest_uuid", key);
    }

    public static Optional<QuestDefinition> byLocalId(String localId) {
        if (localId == null || localId.isBlank()) {
            return Optional.empty();
        }
        String key = localId.trim();
        return unique(snapshot().byLocalId().get(key), "local_id", key);
    }

    public static Optional<QuestDefinition> resolve(String target) {
        if (target == null || target.isBlank()) {
            return Optional.empty();
        }
        Optional<QuestDefinition> byUuid = byUuid(target);
        if (byUuid.isPresent()) {
            return byUuid;
        }
        return byLocalId(target);
    }

    private static Optional<QuestDefinition> unique(List<QuestDefinition> matches, String kind, String key) {
        if (matches == null || matches.isEmpty()) {
            return Optional.empty();
        }
        if (matches.size() > 1) {
            Envoys.LOGGER.warn("[Envoys] Duplicate {} '{}' found on {} NPCs; refusing to resolve it.",
                    kind, key, matches.size());
            return Optional.empty();
        }
        return Optional.of(matches.get(0));
    }

    public static boolean hasKillQuests() {
        return !snapshot().byKillEntity().isEmpty();
    }

    public static List<QuestDefinition> killCandidates(String entityId) {
        if (entityId == null || entityId.isBlank()) {
            return List.of();
        }
        List<QuestDefinition> list = snapshot().byKillEntity().get(entityId.toLowerCase(Locale.ROOT));
        return list == null ? List.of() : list;
    }

    private static Snapshot snapshot() {
        Snapshot current = snapshot;
        if (current == null) {
            current = build();
            snapshot = current;
        }
        return current;
    }

    private static synchronized Snapshot build() {
        Snapshot current = snapshot;
        if (current != null) {
            return current;
        }

        List<Entry> entries = new ArrayList<>();
        Map<String, List<QuestDefinition>> byUuid = new HashMap<>();
        Map<String, List<QuestDefinition>> byNormalizedUuid = new HashMap<>();
        Map<String, List<QuestDefinition>> byLocalId = new HashMap<>();
        Map<String, List<QuestDefinition>> byKillEntity = new HashMap<>();

        for (NPCInteractManager manager : NPCInteractManager.SCRIPTS.values()) {
            if (manager == null || manager.quests == null) {
                continue;
            }
            for (QuestDefinition quest : manager.quests) {
                if (quest == null) {
                    continue;
                }
                entries.add(new Entry(manager, quest));

                if (quest.questUuid != null && !quest.questUuid.isBlank()) {
                    String uuid = quest.questUuid.trim();
                    byUuid.computeIfAbsent(uuid, key -> new ArrayList<>()).add(quest);
                    byNormalizedUuid.computeIfAbsent(normalizeUuid(uuid), key -> new ArrayList<>()).add(quest);
                }
                if (quest.localId != null && !quest.localId.isBlank()) {
                    byLocalId.computeIfAbsent(quest.localId.trim(), key -> new ArrayList<>()).add(quest);
                }

                if (quest.type == QuestType.KILL && quest.entityId != null && !quest.entityId.isBlank()) {
                    String key = quest.entityId.toLowerCase(Locale.ROOT);
                    List<QuestDefinition> list = byKillEntity.computeIfAbsent(key, k -> new ArrayList<>());
                    boolean duplicate = false;
                    for (QuestDefinition existing : list) {
                        if (existing.questUuid != null && existing.questUuid.equals(quest.questUuid)) {
                            duplicate = true;
                            break;
                        }
                    }
                    if (!duplicate) {
                        list.add(quest);
                    }
                }
            }
        }

        Snapshot built = new Snapshot(
                List.copyOf(entries),
                freeze(byUuid),
                freeze(byNormalizedUuid),
                freeze(byLocalId),
                freeze(byKillEntity));
        snapshot = built;
        return built;
    }

    private static Map<String, List<QuestDefinition>> freeze(Map<String, List<QuestDefinition>> source) {
        Map<String, List<QuestDefinition>> result = new HashMap<>();
        for (Map.Entry<String, List<QuestDefinition>> entry : source.entrySet()) {
            result.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Map.copyOf(result);
    }
}
