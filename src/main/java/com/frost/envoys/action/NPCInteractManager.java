package com.frost.envoys.action;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.frost.envoys.action.event.EventType;
import com.frost.envoys.action.event.NpcEventData;

public class NPCInteractManager {

    public static final Map<UUID, NPCInteractManager> SCRIPTS = new HashMap<>();

    public final UUID npcUUID;
    public final Map<EventType, NpcEventData> events = new EnumMap<>(EventType.class);

    public NPCPassportData passport = new NPCPassportData();

    public NPCInteractManager(UUID npcUUID) {
        this.npcUUID = npcUUID;
        SCRIPTS.put(npcUUID, this);
    }

    public NpcEventData getEvent(EventType type) {
        if (type == null) {
            return null;
        }
        return events.get(type);
    }

    public NpcEventData getOrCreateEvent(EventType type) {
        if (type == null) {
            return null;
        }
        return events.computeIfAbsent(type, EventType::createEvent);
    }

    public void putEvent(EventType type, NpcEventData event) {
        if (type == null || event == null) {
            return;
        }
        events.put(type, event);
    }

    public boolean hasActions() {
        for (NpcEventData event : events.values()) {
            if (event != null && event.actions() != null && !event.actions().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static Optional<NPCInteractManager> byUUID(UUID uuid) {
        return Optional.ofNullable(SCRIPTS.get(uuid));
    }

    public static void unregister(UUID uuid) {
        SCRIPTS.remove(uuid);
    }
}
