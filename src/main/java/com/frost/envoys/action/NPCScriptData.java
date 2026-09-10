package com.frost.envoys.action;

import java.util.LinkedHashMap;
import java.util.Map;

import com.frost.envoys.action.event.EventType;
import com.frost.envoys.action.event.NpcEventData;

public class NPCScriptData {
    public String npcUUID;
    public NPCPassportData passport = new NPCPassportData();
    public Map<String, NpcEventData> events = new LinkedHashMap<>();

    public static NPCScriptData fromManager(NPCInteractManager manager) {
        NPCScriptData data = new NPCScriptData();
        data.npcUUID = manager.npcUUID.toString();
        data.passport = manager.passport;
        for (Map.Entry<EventType, NpcEventData> entry : manager.events.entrySet()) {
            data.events.put(entry.getKey().jsonKey(), entry.getValue());
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
