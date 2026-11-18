package com.frost.envoys.client.quest;

import java.util.List;

import com.frost.envoys.network.payload.ClientQuestEntry;

public final class ClientQuestTracker {

    private static final ClientQuestTracker INSTANCE = new ClientQuestTracker();

    private List<ClientQuestEntry> entries = List.of();
    private String pinnedQuestUuid;

    private ClientQuestTracker() {
    }

    public static ClientQuestTracker get() {
        return INSTANCE;
    }

    public void replace(List<ClientQuestEntry> newEntries) {
        this.entries = newEntries == null ? List.of() : List.copyOf(newEntries);
        if (pinnedQuestUuid != null && find(pinnedQuestUuid) == null) {
            pinnedQuestUuid = null;
        }
    }

    public void clear() {
        entries = List.of();
        pinnedQuestUuid = null;
    }

    public List<ClientQuestEntry> entries() {
        return entries;
    }

    public String pinnedQuestUuid() {
        return pinnedQuestUuid;
    }

    public void setPinned(String uuid) {
        pinnedQuestUuid = (uuid == null || uuid.isBlank()) ? null : uuid;
    }

    public ClientQuestEntry activeForHud() {
        ClientQuestEntry pinned = pinnedQuestUuid == null ? null : find(pinnedQuestUuid);
        if (pinned != null && pinned.visibleInGui() && !pinned.completed()) {
            return pinned;
        }
        for (ClientQuestEntry entry : entries) {
            if (entry.visibleInGui() && !entry.completed()) {
                return entry;
            }
        }
        return null;
    }

    private ClientQuestEntry find(String uuid) {
        if (uuid == null) {
            return null;
        }
        for (ClientQuestEntry entry : entries) {
            if (uuid.equals(entry.questUuid())) {
                return entry;
            }
        }
        return null;
    }
}
