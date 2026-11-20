package com.frost.envoys.network;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RequestRateLimiter {

    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_MILLIS = 5_000L;
    private static final long CLEANUP_INTERVAL_MILLIS = 60_000L;

    private static final ConcurrentHashMap<UUID, Deque<Long>> REQUESTS = new ConcurrentHashMap<>();

    private static volatile long lastCleanup = System.currentTimeMillis();

    private RequestRateLimiter() {
    }

    public static boolean tryAcquire(UUID playerId) {
        if (playerId == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        cleanup(now);

        Deque<Long> timestamps = REQUESTS.computeIfAbsent(playerId, id -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() >= WINDOW_MILLIS) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= MAX_REQUESTS) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }

    private static void cleanup(long now) {
        if (now - lastCleanup < CLEANUP_INTERVAL_MILLIS) {
            return;
        }
        synchronized (REQUESTS) {
            if (now - lastCleanup < CLEANUP_INTERVAL_MILLIS) {
                return;
            }
            lastCleanup = now;
            for (Map.Entry<UUID, Deque<Long>> entry : REQUESTS.entrySet()) {
                Deque<Long> timestamps = entry.getValue();
                synchronized (timestamps) {
                    while (!timestamps.isEmpty() && now - timestamps.peekFirst() >= WINDOW_MILLIS) {
                        timestamps.pollFirst();
                    }
                    if (timestamps.isEmpty()) {
                        REQUESTS.remove(entry.getKey(), timestamps);
                    }
                }
            }
        }
    }
}
