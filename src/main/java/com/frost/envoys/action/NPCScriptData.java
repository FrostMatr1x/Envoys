package com.frost.envoys.action;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.frost.envoys.Envoys;
import com.frost.envoys.action.event.EventType;
import com.frost.envoys.action.event.NpcEventData;
import com.frost.envoys.quest.QuestDefinition;
import com.frost.envoys.quest.QuestIndex;

public class NPCScriptData {
    public String npcUUID;
    public NPCPassportData passport = new NPCPassportData();
    public Map<String, NpcEventData> events = new LinkedHashMap<>();
    public List<QuestDefinition> quests = new ArrayList<>();

    public static NPCScriptData fromManager(NPCInteractManager manager) {
        NPCScriptData data = new NPCScriptData();
        data.npcUUID = manager.npcUUID.toString();
        data.passport = manager.passport;
        for (Map.Entry<EventType, NpcEventData> entry : manager.events.entrySet()) {
            data.events.put(entry.getKey().jsonKey(), entry.getValue());
        }
        if (manager.quests != null) {
            data.quests = new ArrayList<>(manager.quests);
        }
        data.materialize();
        return data;
    }

    public void applyTo(NPCInteractManager manager) {
        manager.events.clear();
        for (Map.Entry<String, NpcEventData> entry : events.entrySet()) {
            EventType type = EventType.fromKey(entry.getKey());
            if (type != null && entry.getValue() != null) {
                manager.putEvent(type, entry.getValue());
            }
        }
        manager.quests.clear();
        if (quests != null) {
            manager.quests.addAll(sanitizeQuests(quests, manager));
        }
        QuestIndex.invalidate();
    }

    public static List<QuestDefinition> sanitizeQuests(List<QuestDefinition> input, NPCInteractManager owner) {
        List<QuestDefinition> result = new ArrayList<>();
        Set<String> localIds = new HashSet<>();
        Set<String> uuids = new HashSet<>();
        for (QuestDefinition quest : input) {
            if (quest == null) {
                continue;
            }
            if (!quest.isValid()) {
                Envoys.LOGGER.warn("[Envoys] Skipping invalid quest (local_id='{}', uuid='{}', type={})",
                        quest.localId, quest.questUuid, quest.type);
                continue;
            }
            if (!localIds.add(quest.localId)) {
                Envoys.LOGGER.warn("[Envoys] Skipping quest with duplicate local_id '{}'", quest.localId);
                continue;
            }
            if (!uuids.add(quest.questUuid)) {
                Envoys.LOGGER.warn("[Envoys] Skipping quest with duplicate quest_uuid '{}'", quest.questUuid);
                continue;
            }
            if (uuidUsedByOtherNpc(quest.questUuid, owner)) {
                Envoys.LOGGER.warn("[Envoys] Skipping quest_uuid '{}' already owned by another NPC", quest.questUuid);
                continue;
            }
            result.add(quest);
        }
        return result;
    }

    private static boolean uuidUsedByOtherNpc(String uuid, NPCInteractManager owner) {
        for (NPCInteractManager manager : NPCInteractManager.SCRIPTS.values()) {
            if (manager == null || manager == owner || manager.quests == null) {
                continue;
            }
            for (QuestDefinition quest : manager.quests) {
                if (quest != null && uuid.equals(quest.questUuid)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void materialize() {
        for (EventType type : EventType.values()) {
            events.computeIfAbsent(type.jsonKey(), key -> {
                NpcEventData event = type.createEvent();
                event.setEnabled(false);
                return event;
            });
        }
    }
}
