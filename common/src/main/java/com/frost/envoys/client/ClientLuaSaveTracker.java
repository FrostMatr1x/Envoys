package com.frost.envoys.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientLuaSaveTracker {

    public static final long DEFAULT_TIMEOUT_MILLIS = 15_000L;

    private record Pending(String hash, long sentAt, Runnable onConfirmed) {
    }

    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

    private ClientLuaSaveTracker() {
    }

    public static void begin(UUID npcUuid, String hash, Runnable onConfirmed) {
        if (npcUuid == null) {
            return;
        }
        PENDING.put(npcUuid, new Pending(hash == null ? "" : hash, System.currentTimeMillis(), onConfirmed));
    }

    public static void confirm(UUID npcUuid, String hash, boolean success) {
        if (npcUuid == null) {
            return;
        }
        Pending pending = PENDING.get(npcUuid);
        if (pending == null) {
            return;
        }
        if (!success) {
            PENDING.remove(npcUuid, pending);
            return;
        }
        if (hash != null && !hash.isEmpty() && !hash.equals(pending.hash())) {
            return;
        }
        if (PENDING.remove(npcUuid, pending) && pending.onConfirmed() != null) {
            pending.onConfirmed().run();
        }
    }

    public static boolean isPending(UUID npcUuid) {
        return npcUuid != null && PENDING.containsKey(npcUuid);
    }

    public static void expireIfStale(UUID npcUuid) {
        expireIfStale(npcUuid, DEFAULT_TIMEOUT_MILLIS);
    }

    public static void expireIfStale(UUID npcUuid, long timeoutMillis) {
        if (npcUuid == null) {
            return;
        }
        Pending pending = PENDING.get(npcUuid);
        if (pending != null && System.currentTimeMillis() - pending.sentAt() > timeoutMillis) {
            PENDING.remove(npcUuid, pending);
        }
    }
}
