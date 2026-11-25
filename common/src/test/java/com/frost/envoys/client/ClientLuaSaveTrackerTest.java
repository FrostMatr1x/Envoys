package com.frost.envoys.client;

import com.frost.envoys.TestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientLuaSaveTrackerTest {

    @BeforeEach
    void setUp() {
        TestSupport.resetClientLuaSaveTracker();
    }

    @Test
    void confirmWithWrongHashKeepsPending() {
        UUID npc = TestSupport.uuid(1);
        AtomicBoolean confirmed = new AtomicBoolean();
        ClientLuaSaveTracker.begin(npc, "hash", () -> confirmed.set(true));

        assertTrue(ClientLuaSaveTracker.isPending(npc));
        ClientLuaSaveTracker.confirm(npc, "wrong", true);
        assertTrue(ClientLuaSaveTracker.isPending(npc));
        assertFalse(confirmed.get());
    }

    @Test
    void confirmWithCorrectHashCompletes() {
        UUID npc = TestSupport.uuid(1);
        AtomicBoolean confirmed = new AtomicBoolean();
        ClientLuaSaveTracker.begin(npc, "hash", () -> confirmed.set(true));

        ClientLuaSaveTracker.confirm(npc, "hash", true);
        assertFalse(ClientLuaSaveTracker.isPending(npc));
        assertTrue(confirmed.get());
    }

    @Test
    void failureDropsPendingWithoutCallback() {
        UUID npc = TestSupport.uuid(1);
        AtomicBoolean confirmed = new AtomicBoolean();
        ClientLuaSaveTracker.begin(npc, "hash", () -> confirmed.set(true));

        ClientLuaSaveTracker.confirm(npc, "hash", false);
        assertFalse(ClientLuaSaveTracker.isPending(npc));
        assertFalse(confirmed.get());
    }

    @Test
    void stalePendingIsExpired() {
        UUID npc = TestSupport.uuid(1);
        ClientLuaSaveTracker.begin(npc, "hash", () -> {
        });
        ClientLuaSaveTracker.expireIfStale(npc, -1);
        assertFalse(ClientLuaSaveTracker.isPending(npc));
    }

    @Test
    void nullUuidIsIgnored() {
        ClientLuaSaveTracker.begin(null, "hash", () -> {
        });
        ClientLuaSaveTracker.confirm(null, "hash", true);
        ClientLuaSaveTracker.expireIfStale(null, -1);
        assertFalse(ClientLuaSaveTracker.isPending(null));
    }
}
